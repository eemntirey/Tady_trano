package mg.emit.tadytrano.application.dto;

import mg.emit.tadytrano.domain.model.Ville;

public record VilleResponse(Long id, String nom) {

    public static VilleResponse depuis(Ville ville) {
        return new VilleResponse(ville.getId(), ville.getNom());
    }
}
