package mg.emit.tadytrano.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Ajout d'une photo a une annonce par son URL (CDC F2, CDC 8.2).
 */
public record PhotoRequest(
        @NotBlank(message = "L'URL de la photo est obligatoire")
        @Size(max = 500, message = "L'URL ne doit pas depasser 500 caracteres")
        @Pattern(regexp = "^https?://.+", message = "L'URL doit commencer par http:// ou https://")
        String url,

        boolean principale
) {
}
