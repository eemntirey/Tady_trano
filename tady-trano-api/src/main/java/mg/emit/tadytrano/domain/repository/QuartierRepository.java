package mg.emit.tadytrano.domain.repository;

import mg.emit.tadytrano.domain.model.Quartier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuartierRepository extends JpaRepository<Quartier, Long> {

    List<Quartier> findByVilleIdOrderByNomAsc(Long villeId);

    Optional<Quartier> findByVilleIdAndNomIgnoreCase(Long villeId, String nom);

    boolean existsByNomIgnoreCaseAndVilleId(String nom, Long villeId);
}
