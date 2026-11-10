package bank.dao;

import bank.model.Utilisateur;
import bank.util.DatabaseManager;
import bank.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des utilisateurs (authentification, CRUD admin).
 */
public class UtilisateurDAO {

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ── Authentification ──────────────────────────────────────────────────────

    /**
     * Verifie les credentials et retourne l'utilisateur si valide, null sinon.
     * Seuls les comptes actifs peuvent se connecter.
     */
    public Utilisateur authentifier(String username, String password) {
        String sql = "SELECT id, username, password_hash, role, actif "
                   + "FROM utilisateurs WHERE username = ? AND actif = 1";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (PasswordUtil.verify(password, storedHash)) {
                        return new Utilisateur(
                            rs.getInt("id"),
                            rs.getString("username"),
                            storedHash,
                            rs.getString("role"),
                            rs.getBoolean("actif")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UtilisateurDAO] authentifier error: " + e.getMessage());
        }
        return null; // Echec
    }

    // ── CRUD (reserve a l'admin) ──────────────────────────────────────────────

    public List<Utilisateur> findAll() {
        List<Utilisateur> list = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, role, actif FROM utilisateurs ORDER BY username";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Utilisateur(
                    rs.getInt("id"), rs.getString("username"),
                    rs.getString("password_hash"), rs.getString("role"),
                    rs.getBoolean("actif")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[UtilisateurDAO] findAll error: " + e.getMessage());
        }
        return list;
    }

    public boolean creer(String username, String password, String role) {
        String sql = "INSERT INTO utilisateurs (username, password_hash, role, actif) VALUES (?,?,?,1)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, role);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[UtilisateurDAO] creer error: " + e.getMessage());
            return false;
        }
    }

    public boolean changerMotDePasse(int id, String newPassword) {
        String sql = "UPDATE utilisateurs SET password_hash = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(newPassword));
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UtilisateurDAO] changerMdp error: " + e.getMessage());
            return false;
        }
    }

    public boolean toggleActif(int id, boolean actif) {
        String sql = "UPDATE utilisateurs SET actif = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, actif ? 1 : 0);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UtilisateurDAO] toggleActif error: " + e.getMessage());
            return false;
        }
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM utilisateurs WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UtilisateurDAO] supprimer error: " + e.getMessage());
            return false;
        }
    }

    public boolean usernameExiste(String username) {
        String sql = "SELECT COUNT(*) FROM utilisateurs WHERE username = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }
}
