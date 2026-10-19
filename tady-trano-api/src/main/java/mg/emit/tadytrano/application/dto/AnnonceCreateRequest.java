package mg.emit.tadytrano.application.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal prix,
        @Min(0) Integer surface,
        @NotNull @Min(1) Integer nbPieces,
        Double latitude,
        Double longitude
) {
    @AssertTrue(message = "La latitude et la longitude doivent etre fournies ensemble et rester dans les bornes GPS.")
    public boolean isCoordonneesValides() {
        if (latitude == null && longitude == null) {
            return true;
        }
        if (latitude == null || longitude == null
                || !Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            return false;
        }
        return latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }
}
