package mg.emit.tadytrano.presentation.rest;

import lombok.RequiredArgsConstructor;
import mg.emit.tadytrano.application.dto.FavoriResponse;
import mg.emit.tadytrano.application.dto.PageResponse;
import mg.emit.tadytrano.application.service.AnnonceService;
import mg.emit.tadytrano.infrastructure.security.UtilisateurCourant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Favoris du compte connecte (locataire ou tout autre role connecte).
 */
@RestController
@RequestMapping("/api/favoris")
@RequiredArgsConstructor
public class FavoriController {

    private final AnnonceService annonceService;
    private final UtilisateurCourant utilisateurCourant;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<FavoriResponse> mesFavoris(@AuthenticationPrincipal UserDetails principal,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "15") int taille) {
        return annonceService.mesFavoris(utilisateurCourant.utilisateurId(principal), page, taille);
    }

    @GetMapping("/{annonceId}")
    @PreAuthorize("isAuthenticated()")
    public boolean estFavori(@AuthenticationPrincipal UserDetails principal,
                             @PathVariable Long annonceId) {
        return annonceService.estFavori(utilisateurCourant.utilisateurId(principal), annonceId);
    }

    @PostMapping("/{annonceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> ajouter(@AuthenticationPrincipal UserDetails principal,
                                        @PathVariable Long annonceId) {
        annonceService.ajouterFavori(utilisateurCourant.utilisateurId(principal), annonceId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{annonceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> retirer(@AuthenticationPrincipal UserDetails principal,
                                        @PathVariable Long annonceId) {
        annonceService.retirerFavori(utilisateurCourant.utilisateurId(principal), annonceId);
        return ResponseEntity.noContent().build();
    }
}
