package mg.emit.tadytrano.domain.repository;

import mg.emit.tadytrano.domain.model.Ville;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VilleRepository extends JpaRepository<Ville, Long> {

    List<Ville> findAllByOrderByNomAsc();

    Optional<Ville> findByNomIgnoreCase(String nom);

    boolean existsByNomIgnoreCase(String nom);
}
