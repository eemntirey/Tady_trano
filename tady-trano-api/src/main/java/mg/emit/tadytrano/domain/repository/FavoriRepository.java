package mg.emit.tadytrano.domain.repository;

import mg.emit.tadytrano.domain.model.Favori;
import mg.emit.tadytrano.domain.model.FavoriId;
import mg.emit.tadytrano.domain.model.StatutAnnonce;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriRepository extends JpaRepository<Favori, FavoriId> {

    Optional<Favori> findByUtilisateurIdAndAnnonceId(Long utilisateurId, Long annonceId);

    Page<Favori> findByUtilisateurIdAndAnnonce_StatutInOrderByDateAjoutDesc(
            Long utilisateurId, java.util.Collection<StatutAnnonce> statuts, Pageable pageable);

    void deleteByAnnonceId(Long annonceId);

    void deleteByUtilisateurIdAndAnnonceId(Long utilisateurId, Long annonceId);
}
