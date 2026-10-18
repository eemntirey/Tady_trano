package mg.emit.tadytrano.client.dto;

import java.util.List;

/** Page standardisee de toutes les listes paginnees de l'API. */
public record PageResponse<T>(List<T> contenu, int page, int taille, long totalElements, int totalPages) {
}
