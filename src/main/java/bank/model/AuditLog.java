package bank.model;

/**
 * Modele representant une ligne de log d'audit.
 * nomclientAncien est renseigne uniquement pour les UPDATE,
 * afin de detecter les changements de nom.
 */
public class AuditLog {

    private int    idAudit;
    private String typeAction;
    private String dateAction;
    private String ncompte;
    private String nomclient;        // nom APRES (ou nom courant pour INSERT/DELETE)
    private String nomclientAncien;  // nom AVANT (uniquement pour UPDATE)
    private Double soldeAncien;
    private Double soldeNouv;
    private String utilisateur;
    private String requeteSql;
    private String machineHote;

    public AuditLog(int idAudit, String typeAction, String dateAction,
                    String ncompte, String nomclient, String nomclientAncien,
                    Double soldeAncien, Double soldeNouv,
                    String utilisateur, String requeteSql, String machineHote) {
        this.idAudit         = idAudit;
        this.typeAction      = typeAction;
        this.dateAction      = dateAction;
        this.ncompte         = ncompte;
        this.nomclient       = nomclient;
        this.nomclientAncien = nomclientAncien;
        this.soldeAncien     = soldeAncien;
        this.soldeNouv       = soldeNouv;
        this.utilisateur     = utilisateur;
        this.requeteSql      = requeteSql;
        this.machineHote     = machineHote;
    }

    public int    getIdAudit()          { return idAudit; }
    public String getTypeAction()       { return typeAction; }
    public String getDateAction()       { return dateAction; }
    public String getNcompte()          { return ncompte; }
    public String getNomclient()        { return nomclient; }
    public String getNomclientAncien()  { return nomclientAncien; }
    public Double getSoldeAncien()      { return soldeAncien; }
    public Double getSoldeNouv()        { return soldeNouv; }
    public String getUtilisateur()      { return utilisateur; }
    public String getRequeteSql()       { return requeteSql; }
    public String getMachineHote()      { return machineHote; }

    /** Retourne true si le nom du client a change lors de cet UPDATE. */
    public boolean nomClientAChange() {
        if (!"UPDATE".equals(typeAction)) return false;
        if (nomclientAncien == null)      return false;
        return !nomclientAncien.equals(nomclient);
    }

    /** Retourne true si le solde a change lors de cet UPDATE. */
    public boolean soldeAChange() {
        if (!"UPDATE".equals(typeAction)) return false;
        if (soldeAncien == null || soldeNouv == null) return false;
        return Math.abs(soldeNouv - soldeAncien) > 0.001;
    }
}
