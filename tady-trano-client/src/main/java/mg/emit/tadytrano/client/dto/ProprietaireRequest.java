package mg.emit.tadytrano.client.dto;

/** Creation / modification du profil proprietaire / agence. */
public record ProprietaireRequest(
        String type,
        String nomAgence,
        String telephone,
        String emailContact,
        Long quartierId
) {
}
