package mg.emit.tadytrano.domain.repository;

import mg.emit.tadytrano.domain.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByAnnonceIdOrderByOrdreAsc(Long annonceId);

    List<Photo> findByAnnonceIdIn(Collection<Long> annonceIds);

    Optional<Photo> findByAnnonceIdAndPrincipaleTrue(Long annonceId);

    long countByAnnonceId(Long annonceId);

    void deleteByAnnonceId(Long annonceId);
}
