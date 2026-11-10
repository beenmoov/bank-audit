package bank.model;

/**
 * Modele representant un utilisateur de l'application.
 */
public class Utilisateur {

    private int    id;
    private String username;
    private String passwordHash;   // SHA-256 en hex
    private String role;           // "admin" ou "utilisateur"
    private boolean actif;

    public Utilisateur(int id, String username, String passwordHash,
                       String role, boolean actif) {
        this.id           = id;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.actif        = actif;
    }

    public int     getId()           { return id; }
    public String  getUsername()     { return username; }
    public String  getPasswordHash() { return passwordHash; }
    public String  getRole()         { return role; }
    public boolean isActif()         { return actif; }
}
