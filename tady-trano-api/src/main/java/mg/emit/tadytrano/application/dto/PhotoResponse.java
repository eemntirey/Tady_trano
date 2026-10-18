package mg.emit.tadytrano.application.dto;

import mg.emit.tadytrano.domain.model.Photo;

public record PhotoResponse(
        Long id,
        String url,
        boolean principale,
        int ordre
) {

    public static PhotoResponse depuis(Photo photo) {
        return new PhotoResponse(photo.getId(), photo.getUrl(), photo.isPrincipale(), photo.getOrdre());
    }
}
