package mg.emit.tadytrano.domain.repository;

import jakarta.persistence.LockModeType;
import mg.emit.tadytrano.domain.model.Annonce;
import mg.emit.tadytrano.domain.model.StatutAnnonce;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AnnonceRepository extends JpaRepository<Annonce, Long> {

    /**
     * Recherche filtree par type, quartier, ville et tranches de prix / pieces.
     * Si une position (lat, lng) est fournie, les annonces sont triees par distance
     * croissante (formule de Haversine), les annonces sans coordonnees passant en
     * dernier ; sinon le tri se fait par date de publication decroissante.
     * quartier, ville et typeLocation sont fetchees avec l'annonce (JOIN FETCH) :
     * une seule requete pour les resumes, pas de N+1 ; le comptage utilise une
     * requete dediee sans fetch.
     */
    @Query(value = """
            SELECT a FROM Annonce a
            JOIN FETCH a.quartier q
            JOIN FETCH q.ville v
            JOIN FETCH a.typeLocation t
            WHERE a.statut = :statut
              AND (:typeLocationId IS NULL OR t.id = :typeLocationId)
              AND (:quartierId IS NULL OR q.id = :quartierId)
              AND (:villeId IS NULL OR v.id = :villeId)
              AND (:prixMin IS NULL OR a.prix >= :prixMin)
              AND (:prixMax IS NULL OR a.prix <= :prixMax)
              AND (:nbPiecesMin IS NULL OR a.nbPieces >= :nbPiecesMin)
               AND (:nbPiecesMax IS NULL OR a.nbPieces <= :nbPiecesMax)
            ORDER BY CASE
                       WHEN :lat IS NULL OR :lng IS NULL
                         OR COALESCE(a.latitude, q.latitude) IS NULL
                         OR COALESCE(a.longitude, q.longitude) IS NULL THEN 1000000000.0
                       ELSE 6371.0 * acos(least(1.0, greatest(-1.0,
                             cos(radians(:lat)) * cos(radians(COALESCE(a.latitude, q.latitude))) *
                             cos(radians(COALESCE(a.longitude, q.longitude)) - radians(:lng)) +
                             sin(radians(:lat)) * sin(radians(COALESCE(a.latitude, q.latitude))))))
                     END,
                     a.datePublication DESC
            """,
            countQuery = """
            SELECT count(a) FROM Annonce a
            JOIN a.quartier q
            JOIN q.ville v
            WHERE a.statut = :statut
              AND (:typeLocationId IS NULL OR a.typeLocation.id = :typeLocationId)
              AND (:quartierId IS NULL OR q.id = :quartierId)
              AND (:villeId IS NULL OR v.id = :villeId)
              AND (:prixMin IS NULL OR a.prix >= :prixMin)
              AND (:prixMax IS NULL OR a.prix <= :prixMax)
              AND (:nbPiecesMin IS NULL OR a.nbPieces >= :nbPiecesMin)
              AND (:nbPiecesMax IS NULL OR a.nbPieces <= :nbPiecesMax)
            """)
    Page<Annonce> rechercher(@Param("statut") StatutAnnonce statut,
                             @Param("typeLocationId") Long typeLocationId,
                             @Param("quartierId") Long quartierId,
                             @Param("villeId") Long villeId,
                             @Param("prixMin") BigDecimal prixMin,
                             @Param("prixMax") BigDecimal prixMax,
                             @Param("nbPiecesMin") Integer nbPiecesMin,
                             @Param("nbPiecesMax") Integer nbPiecesMax,
                             @Param("lat") Double lat,
                             @Param("lng") Double lng,
                             Pageable pageable);

    List<Annonce> findByProprietaireIdOrderByDatePublicationDesc(Long proprietaireId);

    @EntityGraph(attributePaths = {"typeLocation", "quartier", "quartier.ville"})
    Page<Annonce> findByProprietaireIdOrderByDatePublicationDesc(Long proprietaireId, Pageable pageable);

    Optional<Annonce> findByIdAndProprietaireId(Long id, Long proprietaireId);

    /** Verrou pessimiste conserve jusqu'au commit pour serialiser les mutations de photos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Annonce> findLockedByIdAndProprietaireId(Long id, Long proprietaireId);



    // Pour l'administration (F10)
    @EntityGraph(attributePaths = {"typeLocation", "quartier", "quartier.ville"})
    Page<Annonce> findByStatutOrderByDatePublicationDesc(StatutAnnonce statut, Pageable pageable);

    @EntityGraph(attributePaths = {"typeLocation", "quartier", "quartier.ville"})
    Page<Annonce> findAllByOrderByDatePublicationDesc(Pageable pageable);

    /** Charge les annonces par ids en une seule requete (favoris) : les associations
     *  lues par le resume sont fetchees pour eviter le N+1. */
    @Override
    @EntityGraph(attributePaths = {"typeLocation", "quartier", "quartier.ville"})
    List<Annonce> findAllById(Iterable<Long> ids);
}
