package mg.emit.tadytrano.client.api;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import mg.emit.tadytrano.client.dto.ProfilResponse;

/** Etat de connexion du client : jeton JWT + profil de l'utilisateur courant. */
public class Session {

    private final BooleanProperty connecte = new SimpleBooleanProperty(false);
    private final BooleanProperty admin = new SimpleBooleanProperty(false);
    /** Vrai uniquement pour le role strict PROPRIETAIRE (routes /api/proprietaires). */
    private final BooleanProperty proprietaire = new SimpleBooleanProperty(false);
    private String jeton;
    private ProfilResponse profil;

    public void connecter(String jeton, ProfilResponse profil) {
        this.jeton = jeton;
        this.profil = profil;
        this.connecte.set(true);
        this.admin.set("ADMIN".equals(profil == null ? null : profil.role()));
        this.proprietaire.set("PROPRIETAIRE".equals(profil == null ? null : profil.role()));
    }

    public void deconnecter() {
        this.jeton = null;
        this.profil = null;
        this.connecte.set(false);
        this.admin.set(false);
        this.proprietaire.set(false);
    }

    /** Propriete FX : true si un compte est connecte. */
    public BooleanProperty connecteProperty() {
        return connecte;
    }

    /** Propriete FX : true si le compte courant est administrateur. */
    public BooleanProperty adminProperty() {
        return admin;
    }

    /** Propriete FX : true si le compte courant est strictement PROPRIETAIRE. */
    public BooleanProperty proprietaireProperty() {
        return proprietaire;
    }

    public boolean estConnecte() {
        return jeton != null;
    }

    public String jeton() {
        return jeton;
    }

    public ProfilResponse profil() {
        return profil;
    }

    public String email() {
        return profil == null ? null : profil.email();
    }

    /** PROPRIETAIRE strict : le serveur refuse les routes proprietaires aux ADMIN. */
    public boolean estProprietaire() {
        return estAvecRole("PROPRIETAIRE");
    }

    public boolean estAdmin() {
        return estAvecRole("ADMIN");
    }

    private boolean estAvecRole(String role) {
        return profil != null && role.equals(profil.role());
    }
}
