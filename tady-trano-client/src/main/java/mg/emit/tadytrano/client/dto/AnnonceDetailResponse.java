package mg.emit.tadytrano.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Detail complet d'une annonce ; contact null si non connecte. */
public record AnnonceDetailResponse(
        Long id,
        String titre,
        String description,
        BigDecimal prix,
        Integer surface,
        Integer nbPieces,
        String statut,
        LocalDateTime datePublication,
        Double latitude,
        Double longitude,
        TypeLocationResponse typeLocation,
        QuartierResponse quartier,
        List<PhotoResponse> photos,
        ContactProprietaireResponse contact
) {
}
