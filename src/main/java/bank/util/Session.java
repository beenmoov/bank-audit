package bank.util;

/**
 * Singleton de session.
 * Stocke l'utilisateur connecte, son role et le hostname.
 *
 * Roles disponibles :
 *   "admin"      -> acces complet (Gestion + Supervision/Audit)
 *   "utilisateur" -> acces Gestion uniquement, Audit verrouille
 */
public class Session {

    public enum Role { ADMIN, UTILISATEUR }

    private static Session instance;
    private String username;
    private Role   role;
    private String hostname;

    private Session() {
        try {
            hostname = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostname = "inconnu";
        }
    }

    public static Session getInstance() {
        if (instance == null) instance = new Session();
        return instance;
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void login(String username, Role role) {
        this.username = username;
        this.role     = role;
    }

    public void logout() {
        this.username = null;
        this.role     = null;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getUsername() { return username != null ? username : "anonyme"; }
    public Role   getRole()     { return role; }
    public String getHostname() { return hostname; }

    public boolean isLoggedIn() { return username != null && !username.isBlank(); }

    /** Retourne true si l'utilisateur a le role admin. */
    public boolean isAdmin() { return role == Role.ADMIN; }

    /** Libelle affichable du role. */
    public String getRoleLabel() {
        if (role == null) return "—";
        return role == Role.ADMIN ? "Administrateur" : "Utilisateur";
    }
}
