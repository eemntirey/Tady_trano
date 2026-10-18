package mg.emit.tadytrano.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Creation d'une annonce. Les coordonnees geographiques sont optionnelles
 * mais vivement conseillees pour le tri par distance.
 */
public record AnnonceCreateRequest(
        @NotNull Long typeLocationId,
        @NotNull Long quartierId,
        @NotBlank @Size(max = 150) String titre,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin(value = "0") BigDecimal prix,
        @Min(0) Integer surface,
        @NotNull @Min(1) Integer nbPieces,
        Double latitude,
        Double longitude
) {
}
