package bank.util;

import java.sql.*;

/**
 * Gestionnaire de base de données SQLite.
 * Crée les tables et les triggers SQL au démarrage de l'application.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:banque_audit.db";
    private static DatabaseManager instance;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Obtient une connexion à la base de données SQLite.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Initialise la base : tables + triggers. Appelé au démarrage.
     */
    public void initialize() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Activer les clés étrangères
            stmt.execute("PRAGMA foreign_keys = ON;");

            // ── TABLE utilisateurs ────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS utilisateurs (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      TEXT    NOT NULL UNIQUE,
                    password_hash TEXT    NOT NULL,
                    role          TEXT    NOT NULL DEFAULT 'utilisateur',
                    actif         INTEGER NOT NULL DEFAULT 1
                );
            """);

            // Comptes par defaut (crees une seule fois si la table est vide)
            // admin  / admin123   -> role admin
            // user   / user123    -> role utilisateur
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM utilisateurs");
            if (rs.next() && rs.getInt(1) == 0) {
                String hashAdmin = bank.util.PasswordUtil.hash("admin123");
                String hashUser  = bank.util.PasswordUtil.hash("user123");
                stmt.execute("INSERT INTO utilisateurs (username, password_hash, role) VALUES "
                    + "('admin', '" + hashAdmin + "', 'admin'),"
                    + "('user',  '" + hashUser  + "', 'utilisateur');");
                System.out.println("[DB] Comptes par defaut crees : admin/admin123, user/user123");
            }

            // ── TABLE compte ──────────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS compte (
                    ncompte    TEXT PRIMARY KEY,
                    nomclient  TEXT NOT NULL,
                    solde      REAL NOT NULL DEFAULT 0
                );
            """);

            // ── TABLE audit_compte ────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_compte (
                    id_audit          INTEGER PRIMARY KEY AUTOINCREMENT,
                    type_action       TEXT    NOT NULL,
                    date_action       TEXT    NOT NULL,
                    ncompte           TEXT,
                    nomclient         TEXT,
                    nomclientAncien   TEXT,
                    soldeAncien       REAL,
                    soldeNouv         REAL,
                    utilisateur       TEXT,
                    requete_sql       TEXT,
                    machine_hote      TEXT
                );
            """);

            // Migration : ajouter la colonne si la table existait sans elle
            try {
                stmt.execute("ALTER TABLE audit_compte ADD COLUMN nomclientAncien TEXT;");
            } catch (Exception ignored) {
                // Colonne deja presente - normal au second lancement
            }

            // ── TRIGGER : AFTER INSERT ────────────────────────────────────
            stmt.execute("DROP TRIGGER IF EXISTS trg_insert_compte;");
            stmt.execute("""
                CREATE TRIGGER trg_insert_compte
                AFTER INSERT ON compte
                FOR EACH ROW
                BEGIN
                    INSERT INTO audit_compte (
                        type_action, date_action, ncompte, nomclient, nomclientAncien,
                        soldeAncien, soldeNouv,
                        utilisateur, requete_sql, machine_hote
                    ) VALUES (
                        'INSERT',
                        datetime('now', 'localtime'),
                        NEW.ncompte,
                        NEW.nomclient,
                        NULL,
                        NULL,
                        NEW.solde,
                        'app_user',
                        'INSERT INTO compte VALUES (' || NEW.ncompte || ', ' || NEW.nomclient || ', ' || NEW.solde || ')',
                        'localhost'
                    );
                END;
            """);

            // ── TRIGGER : AFTER UPDATE ────────────────────────────────────
            stmt.execute("DROP TRIGGER IF EXISTS trg_update_compte;");
            stmt.execute("""
                CREATE TRIGGER trg_update_compte
                AFTER UPDATE ON compte
                FOR EACH ROW
                BEGIN
                    INSERT INTO audit_compte (
                        type_action, date_action, ncompte, nomclient, nomclientAncien,
                        soldeAncien, soldeNouv,
                        utilisateur, requete_sql, machine_hote
                    ) VALUES (
                        'UPDATE',
                        datetime('now', 'localtime'),
                        NEW.ncompte,
                        NEW.nomclient,
                        OLD.nomclient,
                        OLD.solde,
                        NEW.solde,
                        'app_user',
                        'UPDATE compte SET nomclient=' || NEW.nomclient || ', solde=' || NEW.solde || ' WHERE ncompte=' || OLD.ncompte,
                        'localhost'
                    );
                END;
            """);

            // ── TRIGGER : AFTER DELETE ────────────────────────────────────
            stmt.execute("DROP TRIGGER IF EXISTS trg_delete_compte;");
            stmt.execute("""
                CREATE TRIGGER trg_delete_compte
                AFTER DELETE ON compte
                FOR EACH ROW
                BEGIN
                    INSERT INTO audit_compte (
                        type_action, date_action, ncompte, nomclient, nomclientAncien,
                        soldeAncien, soldeNouv,
                        utilisateur, requete_sql, machine_hote
                    ) VALUES (
                        'DELETE',
                        datetime('now', 'localtime'),
                        OLD.ncompte,
                        OLD.nomclient,
                        NULL,
                        OLD.solde,
                        NULL,
                        'app_user',
                        'DELETE FROM compte WHERE ncompte=' || OLD.ncompte,
                        'localhost'
                    );
                END;
            """);

            System.out.println("[DB] Base de données initialisée avec succès.");

        } catch (SQLException e) {
            System.err.println("[DB] Erreur d'initialisation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Met à jour le champ utilisateur dans audit_compte via une procédure post-trigger.
     * Appelé après chaque opération pour remplacer 'app_user' par le vrai utilisateur.
     */
    public void patchLastAuditUser(String username, String hostname) {
        String sql = """
            UPDATE audit_compte
            SET utilisateur  = ?,
                machine_hote = ?
            WHERE id_audit = (SELECT MAX(id_audit) FROM audit_compte)
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hostname);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Patch audit user failed: " + e.getMessage());
        }
    }
}
