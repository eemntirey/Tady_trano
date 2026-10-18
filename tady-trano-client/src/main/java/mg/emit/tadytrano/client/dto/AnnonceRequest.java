package mg.emit.tadytrano.client.dto;

import java.math.BigDecimal;

/** Creation / modification d'une annonce (meme forme des deux cotes). */
public record AnnonceRequest(
        Long typeLocationId,
        Long quartierId,
        String titre,
        String description,
        BigDecimal prix,
        Integer surface,
        Integer nbPieces,
        Double latitude,
        Double longitude
) {
}
