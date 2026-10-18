package mg.emit.tadytrano.application.dto;

import mg.emit.tadytrano.domain.model.Proprietaire;
import mg.emit.tadytrano.domain.model.TypeProprietaire;

public record ProprietaireResponse(
        Long id,
        String emailUtilisateur,
        String nomUtilisateur,
        String prenomUtilisateur,
        TypeProprietaire type,
        String nomAgence,
        String telephone,
        String emailContact,
        QuartierResponse quartier
) {

    public static ProprietaireResponse depuis(Proprietaire proprietaire) {
        return new ProprietaireResponse(
                proprietaire.getId(),
                proprietaire.getUtilisateur().getEmail(),
                proprietaire.getUtilisateur().getNom(),
                proprietaire.getUtilisateur().getPrenom(),
                proprietaire.getType(),
                proprietaire.getNomAgence(),
                proprietaire.getTelephone(),
                proprietaire.getEmailContact(),
                QuartierResponse.depuis(proprietaire.getQuartier()));
    }
}
