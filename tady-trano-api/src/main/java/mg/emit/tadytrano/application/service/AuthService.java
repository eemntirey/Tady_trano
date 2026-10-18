package mg.emit.tadytrano.application.service;

import lombok.RequiredArgsConstructor;
import mg.emit.tadytrano.application.dto.AuthResponse;
import mg.emit.tadytrano.application.dto.LoginRequest;
import mg.emit.tadytrano.application.dto.ProfilResponse;
import mg.emit.tadytrano.application.dto.RegisterRequest;
import mg.emit.tadytrano.application.exception.ApiException;
import mg.emit.tadytrano.domain.model.Role;
import mg.emit.tadytrano.domain.model.Utilisateur;
import mg.emit.tadytrano.domain.repository.UtilisateurRepository;
import mg.emit.tadytrano.infrastructure.security.CustomUserDetailsService;
import mg.emit.tadytrano.infrastructure.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Transactional
    public AuthResponse inscrire(RegisterRequest demande) {
        if (demande.role() == Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Le role admin ne peut pas etre cree par inscription");
        }
        if (utilisateurRepository.existsByEmail(demande.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Un compte existe deja avec cet email");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(demande.email());
        utilisateur.setMotDePasse(passwordEncoder.encode(demande.motDePasse()));
        utilisateur.setNom(demande.nom());
        utilisateur.setPrenom(demande.prenom());
        utilisateur.setRole(demande.role());
        utilisateur.setActif(true);
        utilisateurRepository.save(utilisateur);

        return construireReponse(utilisateur);
    }

    public AuthResponse connecter(LoginRequest demande) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(demande.email(), demande.motDePasse()));
        } catch (BadCredentialsException | DisabledException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect");
        } catch (AuthenticationException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentification impossible");
        }

        Utilisateur utilisateur = utilisateurRepository.findByEmail(demande.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect"));

        return construireReponse(utilisateur);
    }

    public ProfilResponse profil(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        return new ProfilResponse(
                utilisateur.getId(),
                utilisateur.getEmail(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getRole());
    }

    private AuthResponse construireReponse(Utilisateur utilisateur) {
        UserDetails details = userDetailsService.loadUserByUsername(utilisateur.getEmail());
        String jeton = jwtService.genererToken(details);
        return new AuthResponse(
                jeton,
                "Bearer",
                jwtService.dureeDeVieEnSecondes(),
                profil(utilisateur.getEmail()));
    }
}
