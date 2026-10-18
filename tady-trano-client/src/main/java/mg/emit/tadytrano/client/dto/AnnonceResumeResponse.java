package mg.emit.tadytrano.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Resume d'une annonce pour les listes. */
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
        Double distanceKm
) {
}
