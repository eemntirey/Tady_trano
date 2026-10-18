package mg.emit.tadytrano.domain.repository;

import mg.emit.tadytrano.domain.model.Proprietaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProprietaireRepository extends JpaRepository<Proprietaire, Long> {

    Optional<Proprietaire> findByUtilisateurId(Long utilisateurId);

    boolean existsByUtilisateurId(Long utilisateurId);
}
