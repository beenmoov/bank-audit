package bank.dao;

import bank.model.AuditLog;
import bank.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la lecture des logs d'audit et les statistiques.
 */
public class AuditDAO {

    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    // ── Lecture ───────────────────────────────────────────────────────────────

    public List<AuditLog> findAll() {
        return query("""
            SELECT id_audit, type_action, date_action,
                   ncompte, nomclient, nomclientAncien,
                   soldeAncien, soldeNouv,
                   utilisateur, requete_sql, machine_hote
            FROM audit_compte
            ORDER BY id_audit DESC
        """, null);
    }

    public List<AuditLog> findByType(String typeAction) {
        return query("""
            SELECT id_audit, type_action, date_action,
                   ncompte, nomclient, nomclientAncien,
                   soldeAncien, soldeNouv,
                   utilisateur, requete_sql, machine_hote
            FROM audit_compte
            WHERE type_action = ?
            ORDER BY id_audit DESC
        """, typeAction);
    }

    private List<AuditLog> query(String sql, String param) {
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection()) {
            PreparedStatement ps = (param == null)
                ? conn.prepareStatement(sql)
                : conn.prepareStatement(sql);
            if (param != null) ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Double ancS = rs.getObject("soldeAncien") != null ? rs.getDouble("soldeAncien") : null;
                    Double nouvS = rs.getObject("soldeNouv")  != null ? rs.getDouble("soldeNouv")   : null;
                    list.add(new AuditLog(
                        rs.getInt("id_audit"),
                        rs.getString("type_action"),
                        rs.getString("date_action"),
                        rs.getString("ncompte"),
                        rs.getString("nomclient"),
                        rs.getString("nomclientAncien"),   // nouveau champ
                        ancS, nouvS,
                        rs.getString("utilisateur"),
                        rs.getString("requete_sql"),
                        rs.getString("machine_hote")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AuditDAO] query error: " + e.getMessage());
        }
        return list;
    }

    // ── Statistiques ──────────────────────────────────────────────────────────

    public int countInserts() { return countByType("INSERT"); }
    public int countUpdates() { return countByType("UPDATE"); }
    public int countDeletes() { return countByType("DELETE"); }
    public int countTotal()   { return countInserts() + countUpdates() + countDeletes(); }

    private int countByType(String type) {
        String sql = "SELECT COUNT(*) FROM audit_compte WHERE type_action = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[AuditDAO] count error: " + e.getMessage());
        }
        return 0;
    }
}
