package mg.emit.tadytrano.application.dto;

import mg.emit.tadytrano.domain.model.Annonce;
import mg.emit.tadytrano.domain.model.StatutAnnonce;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resume d'une annonce pour la liste de recherche : suffisant pour une carte
 * dans le fil scrollable, sans les contacts du proprietaire.
 *
 * distanceKm est renseigne uniquement quand la recherche a fourni une position
 * et que l'annonce possede des coordonnees ; sinon il reste null.
 */
public record AnnonceResumeResponse(
        Long id,
        String titre,
        BigDecimal prix,
        Integer surface,
        Integer nbPieces,
        StatutAnnonce statut,
        LocalDateTime datePublication,
        TypeLocationResponse typeLocation,
        QuartierResponse quartier,
        String urlPhotoPrincipale,
        Double distanceKm
) {

    public static AnnonceResumeResponse depuis(Annonce annonce, String urlPhotoPrincipale, Double distanceKm) {
        return new AnnonceResumeResponse(
                annonce.getId(),
                annonce.getTitre(),
                annonce.getPrix(),
                annonce.getSurface(),
                annonce.getNbPieces(),
                annonce.getStatut(),
                annonce.getDatePublication(),
                TypeLocationResponse.depuis(annonce.getTypeLocation()),
                QuartierResponse.depuis(annonce.getQuartier()),
                urlPhotoPrincipale,
                distanceKm);
    }
}
