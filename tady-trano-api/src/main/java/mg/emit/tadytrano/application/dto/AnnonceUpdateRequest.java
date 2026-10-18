package mg.emit.tadytrano.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Modification d'une annonce existante. typeLocationId et quartierId sont
 * requis car une annonce ne peut pas exister sans type ni quartier.
 */
public record AnnonceUpdateRequest(
        @NotNull Long typeLocationId,
        @NotNull Long quartierId,
        @NotNull @Size(max = 150) String titre,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin(value = "0") BigDecimal prix,
        @Min(0) Integer surface,
        @NotNull @Min(1) Integer nbPieces,
        Double latitude,
        Double longitude
) {
}
