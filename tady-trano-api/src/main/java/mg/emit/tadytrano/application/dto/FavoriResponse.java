package mg.emit.tadytrano.application.dto;

import mg.emit.tadytrano.domain.model.Favori;

import java.time.LocalDateTime;

/**
 * Favori de l'utilisateur courant, avec le resume de l'annonce associe.
 */
public record FavoriResponse(
        Long annonceId,
        LocalDateTime dateAjout,
        AnnonceResumeResponse annonce
) {

    public static FavoriResponse depuis(Favori favori, AnnonceResumeResponse annonce) {
        return new FavoriResponse(favori.getAnnonceId(), favori.getDateAjout(), annonce);
    }
}
