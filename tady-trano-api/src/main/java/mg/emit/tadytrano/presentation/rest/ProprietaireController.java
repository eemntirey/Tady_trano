package mg.emit.tadytrano.presentation.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.emit.tadytrano.application.dto.AnnonceResumeResponse;
import mg.emit.tadytrano.application.dto.PageResponse;
import mg.emit.tadytrano.application.dto.ProprietaireRequest;
import mg.emit.tadytrano.application.dto.ProprietaireResponse;
import mg.emit.tadytrano.application.service.AnnonceService;
import mg.emit.tadytrano.application.service.ProprietaireService;
import mg.emit.tadytrano.infrastructure.security.UtilisateurCourant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Profil proprietaire / agence, reserve aux comptes de role PROPRIETAIRE.
 */
@RestController
@RequestMapping("/api/proprietaires")
@RequiredArgsConstructor
public class ProprietaireController {

    private final ProprietaireService proprietaireService;
    private final AnnonceService annonceService;
    private final UtilisateurCourant utilisateurCourant;

    @PostMapping
    @PreAuthorize("hasRole('PROPRIETAIRE')")
    public ResponseEntity<ProprietaireResponse> creer(@AuthenticationPrincipal UserDetails principal,
                                                      @Valid @RequestBody ProprietaireRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proprietaireService.creer(utilisateurCourant.email(principal), demande));
    }

    @GetMapping("/moi")
    @PreAuthorize("hasRole('PROPRIETAIRE')")
    public ProprietaireResponse moi(@AuthenticationPrincipal UserDetails principal) {
        return proprietaireService.moi(utilisateurCourant.email(principal));
    }

    @PutMapping("/moi")
    @PreAuthorize("hasRole('PROPRIETAIRE')")
    public ProprietaireResponse modifier(@AuthenticationPrincipal UserDetails principal,
                                         @Valid @RequestBody ProprietaireRequest demande) {
        return proprietaireService.modifier(utilisateurCourant.email(principal), demande);
    }

    /** Annonces du proprietaire courant, tous statuts confondus (espace de gestion). */
    @GetMapping("/mes-annonces")
    @PreAuthorize("hasRole('PROPRIETAIRE')")
    public PageResponse<AnnonceResumeResponse> mesAnnonces(@AuthenticationPrincipal UserDetails principal,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "15") int taille) {
        return annonceService.mesAnnonces(utilisateurCourant.email(principal), page, taille);
    }
}
