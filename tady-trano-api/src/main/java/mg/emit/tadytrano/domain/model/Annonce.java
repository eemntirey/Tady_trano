package mg.emit.tadytrano.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "annonce")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Annonce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private Proprietaire proprietaire;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_location_id", nullable = false)
    private TypeLocation typeLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quartier_id", nullable = false)
    private Quartier quartier;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal prix;

    @Column
    private Integer surface;

    @Column(name = "nb_pieces", nullable = false)
    private Integer nbPieces;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAnnonce statut = StatutAnnonce.ACTIVE;

    /**
     * Distinguishes moderation hiding from a voluntary owner hiding action.
     */
    @Column(name = "masquee_par_admin", nullable = false)
    private boolean masqueeParAdmin = false;

    @Column(name = "date_publication", nullable = false, updatable = false)
    private LocalDateTime datePublication;

    @PrePersist
    void onPrePersist() {
        if (statut == null) {
            statut = StatutAnnonce.ACTIVE;
        }
        if (datePublication == null) {
            datePublication = LocalDateTime.now();
        }
    }
}
