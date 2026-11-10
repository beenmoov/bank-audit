package bank.dao;

import bank.model.Compte;
import bank.util.DatabaseManager;
import bank.util.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour les opérations CRUD sur la table `compte`.
 * Chaque opération déclenche automatiquement le trigger SQL correspondant,
 * puis met à jour l'utilisateur et la machine hôte dans audit_compte.
 */
public class CompteDAO {

    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    // ──────────────────────────────────────────────────────────────────────────
    // Lecture
    // ──────────────────────────────────────────────────────────────────────────

    /** Retourne tous les comptes. */
    public List<Compte> findAll() {
        List<Compte> list = new ArrayList<>();
        String sql = "SELECT ncompte, nomclient, solde FROM compte ORDER BY ncompte";
        try (Connection conn = dbManager.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Compte(
                    rs.getString("ncompte"),
                    rs.getString("nomclient"),
                    rs.getDouble("solde")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DAO] findAll error: " + e.getMessage());
        }
        return list;
    }

    /** Recherche un compte par son numéro. */
    public Compte findByNcompte(String ncompte) {
        String sql = "SELECT ncompte, nomclient, solde FROM compte WHERE ncompte = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ncompte);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Compte(
                        rs.getString("ncompte"),
                        rs.getString("nomclient"),
                        rs.getDouble("solde")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] findByNcompte error: " + e.getMessage());
        }
        return null;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Insertion
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Insère un nouveau compte.
     * Le trigger `trg_insert_compte` génère la ligne d'audit automatiquement.
     * @return true si succès
     */
    public boolean insert(Compte compte) {
        String sql = "INSERT INTO compte (ncompte, nomclient, solde) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, compte.getNcompte());
            ps.setString(2, compte.getNomclient());
            ps.setDouble(3, compte.getSolde());
            ps.executeUpdate();

            // Correction du champ utilisateur dans audit (post-trigger)
            patchAudit();
            return true;

        } catch (SQLException e) {
            System.err.println("[DAO] insert error: " + e.getMessage());
            return false;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Mise à jour
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Met à jour un compte existant.
     * Le trigger `trg_update_compte` génère la ligne d'audit.
     * @return true si succès
     */
    public boolean update(Compte compte) {
        String sql = "UPDATE compte SET nomclient = ?, solde = ? WHERE ncompte = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, compte.getNomclient());
            ps.setDouble(2, compte.getSolde());
            ps.setString(3, compte.getNcompte());
            int affected = ps.executeUpdate();

            if (affected > 0) {
                patchAudit();
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("[DAO] update error: " + e.getMessage());
            return false;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Suppression
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Supprime un compte par son numéro.
     * Le trigger `trg_delete_compte` génère la ligne d'audit.
     * @return true si succès
     */
    public boolean delete(String ncompte) {
        String sql = "DELETE FROM compte WHERE ncompte = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ncompte);
            int affected = ps.executeUpdate();

            if (affected > 0) {
                patchAudit();
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("[DAO] delete error: " + e.getMessage());
            return false;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utilitaire
    // ──────────────────────────────────────────────────────────────────────────

    /** Met à jour utilisateur/machine_hote dans le dernier enregistrement d'audit. */
    private void patchAudit() {
        Session s = Session.getInstance();
        dbManager.patchLastAuditUser(s.getUsername(), s.getHostname());
    }
}
