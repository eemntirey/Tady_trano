package mg.emit.tadytrano.domain.repository;

import mg.emit.tadytrano.domain.model.TypeLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TypeLocationRepository extends JpaRepository<TypeLocation, Long> {

    List<TypeLocation> findAllByOrderByLibelleAsc();

    Optional<TypeLocation> findByCode(String code);
}
