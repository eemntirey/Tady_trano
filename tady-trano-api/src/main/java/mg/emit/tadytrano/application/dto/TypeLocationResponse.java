package mg.emit.tadytrano.application.dto;

import mg.emit.tadytrano.domain.model.TypeLocation;

public record TypeLocationResponse(Long id, String code, String libelle) {

    public static TypeLocationResponse depuis(TypeLocation type) {
        return new TypeLocationResponse(type.getId(), type.getCode(), type.getLibelle());
    }
}
