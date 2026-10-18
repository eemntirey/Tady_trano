package mg.emit.tadytrano.application.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Page de reponse standardisee pour toutes les listes paginnees.
 */
public record PageResponse<T>(
        List<T> contenu,
        int page,
        int taille,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> depuis(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
