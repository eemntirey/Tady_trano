package mg.emit.tadytrano.client.dto;

/** Profil proprietaire / agence de l'utilisateur connecte. */
public record ProprietaireResponse(
        Long id,
        String emailUtilisateur,
        String nomUtilisateur,
        String prenomUtilisateur,
        String type,
        String nomAgence,
        String telephone,
        String emailContact,
        QuartierResponse quartier
) {
}
