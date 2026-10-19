package mg.emit.tadytrano.infrastructure.security;

import mg.emit.tadytrano.application.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Suivi des tentatives de connexion pour limiter les attaques par force brute.
 *
 * Les donnees sont conservees en memoire (ConcurrentHashMap) : cela convient
 * a une instance unique. Pour un déploiement multi-instances, ce suivi devrait
 * etre delegate a un reverse proxy ou a un store partage (Redis).
 *
 * Aucun mot de passe n'est jamais stocke : seules l'email, le nombre d'echecs
 * consecutifs et l'horodatage du premier echec de la serie sont retenus.
 */
@Service
public class LoginAttemptService {

    public static final int MAX_TENTATIVES = 5;
    public static final long DUREE_BLOCAGE_SECONDES = 15 * 60; // 15 minutes

    private final Map<String, Tentative> tentatives = new ConcurrentHashMap<>();

    /**
     * Verifie que l'email n'est pas temporairement bloque.
     * Lance une ApiException (423) si le seuil d'echecs est depasse.
     */
    public void verifierPasBloque(String email) {
        Tentative t = tentatives.get(email);
        if (t == null) {
            return;
        }
        if (t.echecs >= MAX_TENTATIVES
                && Instant.now().isBefore(t.premierEchec.plusSeconds(DUREE_BLOCAGE_SECONDES))) {
            long restant = t.premierEchec.plusSeconds(DUREE_BLOCAGE_SECONDES)
                    .getEpochSecond() - Instant.now().getEpochSecond();
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de tentatives de connexion echec pour ce compte. "
                            + "Nouvel essai possible dans environ %d minute(s)."
                            .formatted(Math.max(1, restant / 60)));
        }
        // Delai de blocage depasse : on reinitialise le compteur.
        tentatives.remove(email);
    }

    /**
     * Enregistre un echec de connexion pour l'email donne.
     * Le mot de passe n'est jamais stocke.
     */
    public void enregistrerEchec(String email) {
        tentatives.compute(email, (cle, existante) -> {
            if (existante == null) {
                return new Tentative(1, Instant.now());
            }
            return new Tentative(existante.echecs + 1, existante.premierEchec);
        });
    }

    /**
     * Reinitialise le compteur d'echecs après une connexion reussie.
     */
    public void reinitialiser(String email) {
        tentatives.remove(email);
    }

    private record Tentative(int echecs, Instant premierEchec) {
    }
}

