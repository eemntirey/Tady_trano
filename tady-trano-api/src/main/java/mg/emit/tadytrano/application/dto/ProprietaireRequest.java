package mg.emit.tadytrano.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mg.emit.tadytrano.domain.model.TypeProprietaire;

/**
 * Creation ou modification du profil proprietaire / agence.
 * nomAgence est requis pour une agence et interdit pour un particulier
 * (validation faite dans ProprietaireService).
 */
public record ProprietaireRequest(
        @NotNull TypeProprietaire type,
        @Size(max = 120) String nomAgence,
        @NotBlank @Size(max = 30) String telephone,
        @Email @Size(max = 190) String emailContact,
        @NotNull Long quartierId
) {
}
