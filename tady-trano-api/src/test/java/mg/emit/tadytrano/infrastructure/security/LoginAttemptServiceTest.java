package mg.emit.tadytrano.infrastructure.security;

import mg.emit.tadytrano.application.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests unitaires de la fenêtre fixe de limitation des connexions.
 * L'horloge contrôlée permet de vérifier l'expiration sans temporisation réelle.
 */
class LoginAttemptServiceTest {

    private MutableClock horloge;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        horloge = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        service = new LoginAttemptService(horloge);
    }

    @Test
    void verifierPasBloqueConserveLeCompteurSousLeSeuil() {
        String email = "user@tady.mg";

        // Chaque contrôle précède un échec réel, comme dans AuthService.connecter().
        for (int i = 0; i < LoginAttemptService.MAX_TENTATIVES - 1; i++) {
            assertDoesNotThrow(() -> service.verifierPasBloque(email));
            service.enregistrerEchec(email);
        }

        // Le contrôle à quatre échecs ne doit pas effacer le compteur.
        assertDoesNotThrow(() -> service.verifierPasBloque(email));
        service.enregistrerEchec(email);

        ApiException ex = assertThrows(ApiException.class, () -> service.verifierPasBloque(email));
        assertEquals(429, ex.getStatut().value());
    }

    @Test
    void cinqEchecsSuccessifsBloqueLaSixiemeTentative() {
        String email = "user@tady.mg";

        for (int i = 0; i < LoginAttemptService.MAX_TENTATIVES; i++) {
            assertDoesNotThrow(() -> service.verifierPasBloque(email));
            service.enregistrerEchec(email);
        }

        ApiException ex = assertThrows(ApiException.class, () -> service.verifierPasBloque(email));
        assertEquals(429, ex.getStatut().value());
    }

    @Test
    void blocageApresSeuilAtteint() {
        for (int i = 0; i < LoginAttemptService.MAX_TENTATIVES; i++) {
            service.enregistrerEchec("autre@tady.mg");
        }

        ApiException ex = assertThrows(ApiException.class,
                () -> service.verifierPasBloque("autre@tady.mg"));
        assertEquals(429, ex.getStatut().value());
    }

    @Test
    void succesReinitialiseLeCompteur() {
        String email = "user@tady.mg";
        service.enregistrerEchec(email);
        service.enregistrerEchec(email);
        service.reinitialiser(email);

        // Après réinitialisation, la série suivante repart de zéro.
        for (int i = 0; i < LoginAttemptService.MAX_TENTATIVES - 1; i++) {
            assertDoesNotThrow(() -> service.verifierPasBloque(email));
            service.enregistrerEchec(email);
        }
        assertDoesNotThrow(() -> service.verifierPasBloque(email));
        service.enregistrerEchec(email);

        ApiException ex = assertThrows(ApiException.class, () -> service.verifierPasBloque(email));
        assertEquals(429, ex.getStatut().value());
    }

    @Test
    void expirationDeLaFenetreReinitialiseLeCompteur() {
        String email = "expire@tady.mg";
        for (int i = 0; i < LoginAttemptService.MAX_TENTATIVES; i++) {
            service.verifierPasBloque(email);
            service.enregistrerEchec(email);
        }
        assertThrows(ApiException.class, () -> service.verifierPasBloque(email));

        // À l'échéance exacte, l'ancien compteur est expiré et retiré.
        horloge.avancer(Duration.ofSeconds(LoginAttemptService.DUREE_BLOCAGE_SECONDES));
        assertDoesNotThrow(() -> service.verifierPasBloque(email));

        // Le prochain échec démarre une nouvelle fenêtre fixe.
        service.enregistrerEchec(email);
        for (int i = 1; i < LoginAttemptService.MAX_TENTATIVES; i++) {
            assertDoesNotThrow(() -> service.verifierPasBloque(email));
            service.enregistrerEchec(email);
        }
        assertThrows(ApiException.class, () -> service.verifierPasBloque(email));
    }

    @Test
    void emailInconnuPasDeBlocage() {
        assertDoesNotThrow(() -> service.verifierPasBloque("inconnu@tady.mg"));
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;
        private final ZoneId zone;

        private MutableClock(Instant initial, ZoneId zone) {
            this.instant = new AtomicReference<>(initial);
            this.zone = zone;
        }

        private MutableClock(Instant initial) {
            this(initial, ZoneOffset.UTC);
        }

        void avancer(Duration duree) {
            instant.updateAndGet(moment -> moment.plus(duree));
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant(), zone);
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}