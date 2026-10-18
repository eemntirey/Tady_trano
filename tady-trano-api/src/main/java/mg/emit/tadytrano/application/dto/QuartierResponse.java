package mg.emit.tadytrano.application.dto;

import mg.emit.tadytrano.domain.model.Quartier;

public record QuartierResponse(
        Long id,
        String nom,
        VilleResponse ville,
        Double latitude,
        Double longitude
) {

    public static QuartierResponse depuis(Quartier quartier) {
        return new QuartierResponse(
                quartier.getId(),
                quartier.getNom(),
                VilleResponse.depuis(quartier.getVille()),
                quartier.getLatitude(),
                quartier.getLongitude());
    }
}
