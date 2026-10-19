package mg.emit.tadytrano.presentation.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.emit.tadytrano.application.dto.AnnonceCreateRequest;
import mg.emit.tadytrano.application.dto.AnnonceDetailResponse;
import mg.emit.tadytrano.application.dto.AnnonceUpdateRequest;
import mg.emit.tadytrano.application.dto.PageResponse;
import mg.emit.tadytrano.application.dto.AnnonceResumeResponse;
import mg.emit.tadytrano.application.dto.PhotoRequest;
import mg.emit.tadytrano.application.dto.PhotoResponse;
import mg.emit.tadytrano.application.dto.StatutAnnonceRequest;
import mg.emit.tadytrano.application.exception.ApiException;
import mg.emit.tadytrano.application.service.AnnonceService;
import mg.emit.tadytrano.infrastructure.security.UtilisateurCourant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Annonces : recherche publique, publication et gestion par les proprietaires.
 */
@RestController
@RequestMapping("/api/annonces")
@RequiredArgsConstructor
public class AnnonceController {

    private final AnnonceService annonceService;
    private final UtilisateurCourant utilisateurCourant;

    /**
     * Recherche publique (annonces actives uniquement). Filtres tous optionnels ;
     * lat/lng activent le tri par distance croissante.
     */
    @GetMapping
    public PageResponse<AnnonceResumeResponse> rechercher(
            @RequestParam(required = false) Long typeLocationId,
            @RequestParam(required = false) Long quartierId,
            @RequestParam(required = false) Long villeId,
            @RequestParam(required = false) BigDecimal prixMin,
            @RequestParam(required = false) BigDecimal prixMax,
            @RequestParam(required = false) Integer nbPiecesMin,
            @RequestParam(required = false) Integer nbPiecesMax,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int taille) {
        validerCoordonneesRecherche(lat, lng);
        return annonceService.rechercher(typeLocationId, quartierId, villeId, prixMin, prixMax,
                nbPiecesMin, nbPiecesMax, lat, lng, page, taille);
    }

    /**
     * Valide que les parametres de reference GPS sont fournis en paire, finis
     * et dans les bornes geographiques. Retourne 400 sinon.
     */
    private void validerCoordonneesRecherche(Double lat, Double lng) {
        boolean latFournie = lat != null;
        boolean lngFournie = lng != null;

        if (latFournie != lngFournie) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Les parametres 'lat' et 'lng' doivent etre fournis ensemble pour le tri par distance.");
        }
        if (!latFournie) {
            return; // Aucun tri par distance demande : rien a valider.
        }

        if (!Double.isFinite(lat) || !Double.isFinite(lng)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Les coordonnees 'lat' et 'lng' doivent etre des nombres finis.");
        }
        if (lat < -90.0 || lat > 90.0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La latitude 'lat' doit etre comprise entre -90 et 90 degres.");
        }
        if (lng < -180.0 || lng > 180.0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La longitude 'lng' doit etre comprise entre -180 et 180 degres.");
        }
    }

    @GetMapping("/{id}")
    public AnnonceDetailResponse detail(@PathVariable Long id) {
        return annonceService.detail(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('PROPRIETAIRE')")
    public ResponseEntity<AnnonceDetailResponse> creer(@AuthenticationPrincipal UserDetails principal,
                                                      @Valid @RequestBody AnnonceCreateRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(annonceService.creer(utilisateurCourant.email(principal), demande));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROPRIETAIRE')")
    public AnnonceDetailResponse modifier(@AuthenticationPrincipal UserDetails principal,
                                          @PathVariable Long id,
                                          @Valid @RequestBody AnnonceUpdateRequest demande) {
        return annonceService.modifier(utilisateurCourant.email(principal), id, demande);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasRole('PROPRIETAIRE')")
    public ResponseEntity<Void> changerStatut(@AuthenticationPrincipal UserDetails principal,
                                              @PathVariable Long id,
                                              @Valid @RequestBody StatutAnnonceRequest demande) {
        annonceService.changerStatut(utilisateurCourant.email(principal), id, demande);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROPRIETAIRE')")
    public ResponseEntity<Void> supprimer(@AuthenticationPrincipal UserDetails principal,
                                          @PathVariable Long id) {
        annonceService.supprimer(utilisateurCourant.email(principal), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/photos")
    public List<PhotoResponse> photos(@PathVariable Long id) {
        return annonceService.photos(id);
    }

    @PostMapping("/{id}/photos")
    @PreAuthorize("hasRole('PROPRIETAIRE')")
    public ResponseEntity<PhotoResponse> ajouterPhoto(@AuthenticationPrincipal UserDetails principal,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody PhotoRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(annonceService.ajouterPhoto(utilisateurCourant.email(principal), id, demande));
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    @PreAuthorize("hasRole('PROPRIETAIRE')")
    public ResponseEntity<Void> supprimerPhoto(@AuthenticationPrincipal UserDetails principal,
                                               @PathVariable Long id,
                                               @PathVariable Long photoId) {
        annonceService.supprimerPhoto(utilisateurCourant.email(principal), id, photoId);
        return ResponseEntity.noContent().build();
    }
}
