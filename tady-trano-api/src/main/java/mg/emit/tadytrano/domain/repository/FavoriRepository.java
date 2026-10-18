package mg.emit.tadytrano.domain.repository;

import mg.emit.tadytrano.domain.model.Favori;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriRepository extends JpaRepository<Favori, Long> {

    Optional<Favori> findByUtilisateurIdAndAnnonceId(Long utilisateurId, Long annonceId);

    Page<Favori> findByUtilisateurIdOrderByDateAjoutDesc(Long utilisateurId, Pageable pageable);

    void deleteByAnnonceId(Long annonceId);

    void deleteByUtilisateurIdAndAnnonceId(Long utilisateurId, Long annonceId);
}
