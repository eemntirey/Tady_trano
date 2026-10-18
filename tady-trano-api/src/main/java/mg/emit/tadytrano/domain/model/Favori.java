package mg.emit.tadytrano.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "favori")
@IdClass(FavoriId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Favori {

    @Id
    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @Id
    @Column(name = "annonce_id")
    private Long annonceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", insertable = false, updatable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annonce_id", insertable = false, updatable = false)
    private Annonce annonce;

    @Column(name = "date_ajout", nullable = false)
    private LocalDateTime dateAjout;

    @PrePersist
    void onPrePersist() {
        if (dateAjout == null) {
            dateAjout = LocalDateTime.now();
        }
    }
}
