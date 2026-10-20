package mg.emit.tadytrano.client.dto;

import mg.emit.tadytrano.client.util.Affichage;

/** Type de location (hotel, etage, piece...). */
public record TypeLocationResponse(Long id, String code, String libelle) {

    @Override
    public String toString() {
        return Affichage.libelleType(libelle);
    }
}
