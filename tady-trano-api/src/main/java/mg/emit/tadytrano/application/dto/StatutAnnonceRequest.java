package mg.emit.tadytrano.application.dto;

import jakarta.validation.constraints.NotNull;
import mg.emit.tadytrano.domain.model.StatutAnnonce;

/**
 * Changement de statut d'une annonce (ACTIVE, LOUEE, MASQUEE).
 */
public record StatutAnnonceRequest(@NotNull StatutAnnonce statut) {
}
