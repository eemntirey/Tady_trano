package mg.emit.tadytrano.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Resume d'une annonce pour les listes.
 *  latitude / longitude : position sur la carte (repli sur le quartier),
 *  null = annonce non positionnee. */
public record AnnonceResumeResponse(
        Long id,
        String titre,
        BigDecimal prix,
        Integer surface,
        Integer nbPieces,
        String statut,
        LocalDateTime datePublication,
        TypeLocationResponse typeLocation,
        QuartierResponse quartier,
        String urlPhotoPrincipale,
        Double distanceKm,
        Double latitude,
        Double longitude
) {
}
