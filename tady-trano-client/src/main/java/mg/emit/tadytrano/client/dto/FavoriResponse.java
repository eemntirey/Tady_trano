package mg.emit.tadytrano.client.dto;

import java.time.LocalDateTime;

/** Favori de l'utilisateur courant, avec le resume de l'annonce associe. */
public record FavoriResponse(Long annonceId, LocalDateTime dateAjout, AnnonceResumeResponse annonce) {
}
