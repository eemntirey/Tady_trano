package mg.emit.tadytrano.presentation.rest;

import lombok.RequiredArgsConstructor;
import mg.emit.tadytrano.application.dto.QuartierResponse;
import mg.emit.tadytrano.application.dto.TypeLocationResponse;
import mg.emit.tadytrano.application.dto.VilleResponse;
import mg.emit.tadytrano.application.service.ReferentielService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Donnees de reference, accessibles sans authentification.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReferentielController {

    private final ReferentielService referentielService;

    @GetMapping("/villes")
    public List<VilleResponse> villes() {
        return referentielService.villes();
    }

    @GetMapping("/villes/{villeId}/quartiers")
    public List<QuartierResponse> quartiers(@PathVariable Long villeId) {
        return referentielService.quartiersDeLaVille(villeId);
    }

    @GetMapping("/types-location")
    public List<TypeLocationResponse> typesLocation() {
        return referentielService.typesLocation();
    }
}
