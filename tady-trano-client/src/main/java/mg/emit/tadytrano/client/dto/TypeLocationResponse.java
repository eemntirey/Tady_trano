package mg.emit.tadytrano.client.dto;

/** Type de location (hotel, etage, piece...). */
public record TypeLocationResponse(Long id, String code, String libelle) {

    @Override
    public String toString() {
        return libelle;
    }
}
