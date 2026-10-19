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
 *
 * latitude / longitude positionnent l'annonce sur la carte (CDC F7) : coordonnees
 * de l'annonce, avec repli sur celles du quartier ; null = annonce non positionnee.
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
        Double distanceKm,
        Double latitude,
        Double longitude
) {

    public static AnnonceResumeResponse depuis(Annonce annonce, String urlPhotoPrincipale, Double distanceKm) {
        QuartierResponse quartier = QuartierResponse.depuis(annonce.getQuartier());
        Double latitude = annonce.getLatitude() != null ? annonce.getLatitude()
                : (quartier == null ? null : quartier.latitude());
        Double longitude = annonce.getLongitude() != null ? annonce.getLongitude()
                : (quartier == null ? null : quartier.longitude());
        return new AnnonceResumeResponse(
                annonce.getId(),
                annonce.getTitre(),
                annonce.getPrix(),
                annonce.getSurface(),
                annonce.getNbPieces(),
                annonce.getStatut(),
                annonce.getDatePublication(),
                TypeLocationResponse.depuis(annonce.getTypeLocation()),
                quartier,
                urlPhotoPrincipale,
                distanceKm,
                latitude,
                longitude);
    }
}
