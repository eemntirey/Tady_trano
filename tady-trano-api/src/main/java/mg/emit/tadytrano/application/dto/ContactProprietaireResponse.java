package mg.emit.tadytrano.application.dto;

import mg.emit.tadytrano.domain.model.Proprietaire;
import mg.emit.tadytrano.domain.model.TypeProprietaire;

/**
 * Coordonnees du proprietaire ou de l'agence, exposees uniquement aux
 * utilisateurs connectes pour limiter le scraping.
 */
public record ContactProprietaireResponse(
        String nom,
        String telephone,
        String emailContact
) {

    public static ContactProprietaireResponse depuis(Proprietaire proprietaire) {
        String nom = proprietaire.getType() == TypeProprietaire.AGENCE
                ? proprietaire.getNomAgence()
                : proprietaire.getUtilisateur().getPrenom() + " " + proprietaire.getUtilisateur().getNom();
        return new ContactProprietaireResponse(nom, proprietaire.getTelephone(), proprietaire.getEmailContact());
    }
}
