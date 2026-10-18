package mg.emit.tadytrano.client.dto;

/** Quartier du referentiel, avec sa ville. */
public record QuartierResponse(Long id, String nom, VilleResponse ville, Double latitude, Double longitude) {

    @Override
    public String toString() {
        return ville == null ? nom : nom + " (" + ville.nom() + ")";
    }
}
