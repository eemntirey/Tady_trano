package mg.emit.tadytrano.presentation.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.emit.tadytrano.application.dto.AnnonceResumeResponse;
import mg.emit.tadytrano.application.dto.PageResponse;
import mg.emit.tadytrano.application.dto.StatutAnnonceRequest;
import mg.emit.tadytrano.application.service.AnnonceService;
import mg.emit.tadytrano.domain.model.StatutAnnonce;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moderation et administration (CDC F10).
 * Toutes les routes de ce controleur sont reservees au role ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AnnonceService annonceService;

    /**
     * Liste toutes les annonces, tous statuts confondus, filtrable par statut.
     * Permet a l'administrateur de voir les annonces masquees et louees.
     *
     * @param statut filtre optionnel : ACTIVE, LOUEE ou MASQUEE
     * @param page   numero de page (0-indexed)
     * @param taille nombre d'annonces par page
     */
    @GetMapping("/annonces")
    public PageResponse<AnnonceResumeResponse> listerToutesLesAnnonces(
            @RequestParam(required = false) StatutAnnonce statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille) {
        return annonceService.toutesLesAnnonces(statut, page, taille);
    }

    /**
     * Modere une annonce : valide (ACTIVE), masque (MASQUEE) ou marque comme louee (LOUEE).
     * L'administrateur peut agir sur n'importe quelle annonce, independamment du proprietaire.
     *
     * @param id     identifiant de l'annonce a moderer
     * @param demande nouveau statut souhaite
     */
    @PatchMapping("/annonces/{id}/statut")
    public ResponseEntity<Void> modererAnnonce(@PathVariable Long id,
                                               @Valid @RequestBody StatutAnnonceRequest demande) {
        annonceService.modererAnnonce(id, demande);
        return ResponseEntity.noContent().build();
    }
}
