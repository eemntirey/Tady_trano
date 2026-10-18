package mg.emit.tadytrano.application.service;

import lombok.RequiredArgsConstructor;
import mg.emit.tadytrano.application.dto.ProprietaireRequest;
import mg.emit.tadytrano.application.dto.ProprietaireResponse;
import mg.emit.tadytrano.application.exception.ApiException;
import mg.emit.tadytrano.domain.model.Proprietaire;
import mg.emit.tadytrano.domain.model.Quartier;
import mg.emit.tadytrano.domain.model.Role;
import mg.emit.tadytrano.domain.model.TypeProprietaire;
import mg.emit.tadytrano.domain.model.Utilisateur;
import mg.emit.tadytrano.domain.repository.ProprietaireRepository;
import mg.emit.tadytrano.domain.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion du profil proprietaire / agence.
 *
 * Le profil est cree a partir d'un compte deja inscrit avec le role
 * PROPRIETAIRE ; il est ensuite modifiable par son proprietaire.
 */
@Service
@RequiredArgsConstructor
public class ProprietaireService {

    private final ProprietaireRepository proprietaireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ReferentielService referentielService;

    @Transactional
    public ProprietaireResponse creer(String email, ProprietaireRequest demande) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        if (utilisateur.getRole() != Role.PROPRIETAIRE) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Le profil proprietaire est reserve aux comptes de role PROPRIETAIRE");
        }
        if (proprietaireRepository.existsByUtilisateurId(utilisateur.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Un profil proprietaire existe deja pour ce compte");
        }

        Quartier quartier = referentielService.quartier(demande.quartierId());
        validerType(demande);

        Proprietaire proprietaire = new Proprietaire();
        proprietaire.setUtilisateur(utilisateur);
        proprietaire.setType(demande.type());
        proprietaire.setNomAgence(demande.type() == TypeProprietaire.AGENCE ? demande.nomAgence() : null);
        proprietaire.setTelephone(demande.telephone());
        proprietaire.setEmailContact(demande.emailContact());
        proprietaire.setQuartier(quartier);

        return ProprietaireResponse.depuis(proprietaireRepository.save(proprietaire));
    }

    @Transactional
    public ProprietaireResponse modifier(String email, ProprietaireRequest demande) {
        Proprietaire proprietaire = parEmail(email);
        Quartier quartier = referentielService.quartier(demande.quartierId());
        validerType(demande);

        proprietaire.setType(demande.type());
        proprietaire.setNomAgence(demande.type() == TypeProprietaire.AGENCE ? demande.nomAgence() : null);
        proprietaire.setTelephone(demande.telephone());
        proprietaire.setEmailContact(demande.emailContact());
        proprietaire.setQuartier(quartier);

        return ProprietaireResponse.depuis(proprietaireRepository.save(proprietaire));
    }

    @Transactional(readOnly = true)
    public ProprietaireResponse moi(String email) {
        return ProprietaireResponse.depuis(parEmail(email));
    }

    /**
     * Profil proprietaire rattache au compte courant, ou 403 si le compte n'en
     * a pas : utilise avant toute operation sur les annonces.
     */
    @Transactional(readOnly = true)
    public Proprietaire parEmail(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        return proprietaireRepository.findByUtilisateurId(utilisateur.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "Profil proprietaire manquant : completez d'abord votre profil"));
    }

    private void validerType(ProprietaireRequest demande) {
        boolean nomAgenceAbsent = demande.nomAgence() == null || demande.nomAgence().isBlank();
        if (demande.type() == TypeProprietaire.AGENCE && nomAgenceAbsent) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Le nom d'agence est requis pour une agence");
        }
        if (demande.type() == TypeProprietaire.PARTICULIER && !nomAgenceAbsent) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Le nom d'agence est interdit pour un particulier");
        }
    }
}
