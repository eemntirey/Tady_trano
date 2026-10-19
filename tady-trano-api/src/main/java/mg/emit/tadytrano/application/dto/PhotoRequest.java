package mg.emit.tadytrano.application.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.net.URI;

/**
 * Ajout d'une photo a une annonce par son URL (CDC F2, CDC 8.2).
 */
public record PhotoRequest(
        @NotBlank(message = "L'URL de la photo est obligatoire")
        @Size(max = 500, message = "L'URL ne doit pas depasser 500 caracteres")
        @Pattern(regexp = "^https?://[^\\s\"'<>\\\\]+$", message = "L'URL doit etre une URL HTTP(S) sans espaces ni caracteres HTML")
        String url,

        boolean principale
) {
    @AssertTrue(message = "L'URL doit etre une URL HTTP(S) absolue valide.")
    public boolean isUrlHttpValide() {
        if (url == null || url.isBlank()) {
            return true; // @NotBlank signale deja l'absence d'URL.
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            return uri.getHost() != null && uri.getUserInfo() == null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
