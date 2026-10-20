package mg.emit.tadytrano.infrastructure.security;

import mg.emit.tadytrano.application.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
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
    private final Clock horloge;

    /** Conserve le constructeur sans argument utilisé par Spring en production. */
    @Autowired
    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    // Horloge injectable pour tester l'expiration sans attendre 15 minutes réelles.
    LoginAttemptService(Clock horloge) {
        this.horloge = horloge;
    }

    /**
     * Vérifie si l'email est temporairement bloqué.
     *
     * La fenêtre est FIXE : elle commence au premier échec et dure
     * DUREE_BLOCAGE_SECONDES. Un contrôle intermédiaire ne doit pas effacer
     * le compteur ; seul un succès ou l'expiration de cette fenêtre le réinitialise.
     * Une suppression conditionnelle évite d'effacer un compteur modifié en parallèle.
     */
    public void verifierPasBloque(String email) {
        while (true) {
            Tentative t = tentatives.get(email);
            if (t == null) {
                return;
            }

            Instant maintenant = horloge.instant();
            Instant finFenetre = t.premierEchec.plusSeconds(DUREE_BLOCAGE_SECONDES);
            if (!maintenant.isBefore(finFenetre)) {
                if (tentatives.remove(email, t)) {
                    return;
                }
                // L'entrée a changé après get() : relire avant de décider.
                continue;
            }

            if (t.echecs >= MAX_TENTATIVES) {
                long restant = Duration.between(maintenant, finFenetre).toSeconds();
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Trop de tentatives de connexion echec pour ce compte. "
                                + "Nouvel essai possible dans environ %d minute(s)."
                                .formatted(Math.max(1, restant / 60)));
            }
            return;
        }
    }

    /**
     * Enregistre un echec de connexion pour l'email donne.
     * Le mot de passe n'est jamais stocke.
     */
    public void enregistrerEchec(String email) {
        tentatives.compute(email, (cle, existante) -> {
            if (existante == null) {
                return new Tentative(1, horloge.instant());
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

