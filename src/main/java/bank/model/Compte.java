package bank.model;

/**
 * Modèle représentant un compte bancaire.
 */
public class Compte {

    private String ncompte;
    private String nomclient;
    private double solde;

    public Compte(String ncompte, String nomclient, double solde) {
        this.ncompte   = ncompte;
        this.nomclient = nomclient;
        this.solde     = solde;
    }

    public String getNcompte()   { return ncompte; }
    public String getNomclient() { return nomclient; }
    public double getSolde()     { return solde; }

    public void setNcompte(String ncompte)     { this.ncompte   = ncompte; }
    public void setNomclient(String nomclient) { this.nomclient = nomclient; }
    public void setSolde(double solde)         { this.solde     = solde; }

    @Override
    public String toString() {
        return "Compte{" + ncompte + ", " + nomclient + ", " + solde + "}";
    }
}
