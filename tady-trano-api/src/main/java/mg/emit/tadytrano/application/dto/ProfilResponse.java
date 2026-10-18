package mg.emit.tadytrano.application.dto;

import mg.emit.tadytrano.domain.model.Role;

public record ProfilResponse(
        Long id,
        String email,
        String nom,
        String prenom,
        Role role
) {
}
