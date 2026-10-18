package mg.emit.tadytrano.client.dto;

/** Ville du referentiel. */
public record VilleResponse(Long id, String nom) {

    @Override
    public String toString() {
        return nom;
    }
}
