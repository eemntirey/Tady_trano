package mg.emit.tadytrano.infrastructure.security;

import lombok.RequiredArgsConstructor;
import mg.emit.tadytrano.application.exception.ApiException;
import mg.emit.tadytrano.domain.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Acces aux informations du compte connecte depuis un principal Spring Security.
 */
@Component
@RequiredArgsConstructor
public class UtilisateurCourant {

    private final UtilisateurRepository utilisateurRepository;

    public String email(UserDetails principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        return principal.getUsername();
    }

    /** Id de l'utilisateur connecte, ou 404 si le compte a ete supprime. */
    public Long utilisateurId(UserDetails principal) {
        return utilisateurRepository.findByEmail(email(principal))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"))
                .getId();
    }
}
