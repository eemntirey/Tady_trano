package mg.emit.tadytrano.application.service;

import lombok.RequiredArgsConstructor;
import mg.emit.tadytrano.application.dto.QuartierResponse;
import mg.emit.tadytrano.application.dto.TypeLocationResponse;
import mg.emit.tadytrano.application.dto.VilleResponse;
import mg.emit.tadytrano.application.exception.ApiException;
import mg.emit.tadytrano.domain.model.Quartier;
import mg.emit.tadytrano.domain.repository.QuartierRepository;
import mg.emit.tadytrano.domain.repository.TypeLocationRepository;
import mg.emit.tadytrano.domain.repository.VilleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Donnees de reference : villes, quartiers et types de location.
 */
@Service
@RequiredArgsConstructor
public class ReferentielService {

    private final VilleRepository villeRepository;
    private final QuartierRepository quartierRepository;
    private final TypeLocationRepository typeLocationRepository;

    @Transactional(readOnly = true)
    public List<VilleResponse> villes() {
        return villeRepository.findAllByOrderByNomAsc().stream().map(VilleResponse::depuis).toList();
    }

    @Transactional(readOnly = true)
    public List<QuartierResponse> quartiersDeLaVille(Long villeId) {
        if (!villeRepository.existsById(villeId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Ville introuvable");
        }
        return quartierRepository.findByVilleIdOrderByNomAsc(villeId).stream()
                .map(QuartierResponse::depuis).toList();
    }

    @Transactional(readOnly = true)
    public Quartier quartier(Long id) {
        return quartierRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Quartier introuvable"));
    }

    @Transactional(readOnly = true)
    public List<TypeLocationResponse> typesLocation() {
        return typeLocationRepository.findAllByOrderByLibelleAsc().stream()
                .map(TypeLocationResponse::depuis).toList();
    }
}
