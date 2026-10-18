package mg.emit.tadytrano.application.dto;

import mg.emit.tadytrano.domain.model.Annonce;
import mg.emit.tadytrano.domain.model.StatutAnnonce;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detail complet d'une annonce.
 *
 * contact est null tant que l'utilisateur n'est pas connecte : les contacts du
 * proprietaire/agent sont reserve aux comptes connectes pour limiter le
 * scraping.
 */
public record AnnonceDetailResponse(
        Long id,
        String titre,
        String description,
        BigDecimal prix,
        Integer surface,
        Integer nbPieces,
        StatutAnnonce statut,
        LocalDateTime datePublication,
        Double latitude,
        Double longitude,
        TypeLocationResponse typeLocation,
        QuartierResponse quartier,
        List<PhotoResponse> photos,
        ContactProprietaireResponse contact
) {

    public static AnnonceDetailResponse depuis(Annonce annonce, List<PhotoResponse> photos,
                                               ContactProprietaireResponse contact) {
        return new AnnonceDetailResponse(
                annonce.getId(),
                annonce.getTitre(),
                annonce.getDescription(),
                annonce.getPrix(),
                annonce.getSurface(),
                annonce.getNbPieces(),
                annonce.getStatut(),
                annonce.getDatePublication(),
                annonce.getLatitude(),
                annonce.getLongitude(),
                TypeLocationResponse.depuis(annonce.getTypeLocation()),
                QuartierResponse.depuis(annonce.getQuartier()),
                photos,
                contact);
    }
}
