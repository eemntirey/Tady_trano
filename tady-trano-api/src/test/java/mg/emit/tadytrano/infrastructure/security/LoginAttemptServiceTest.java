package mg.emit.tadytrano.infrastructure.security;

import mg.emit.tadytrano.application.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test unitaire de {@link LoginAttemptService} : verifie le seuil de blocage
 * et la reinitialisation apres succes, sans passer par la couche HTTP.
 */
class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    @Test
    void aucunBlocageAvantSeuil() {
        // Quatre echecs : pas encore bloque.
        for (int i = 0; i < 4; i++) {
            assertDoesNotThrow(() -> service.verifierPasBloque("user@tady.mg"));
            service.enregistrerEchec("user@tady.mg");
        }
        assertDoesNotThrow(() -> service.verifierPasBloque("user@tady.mg"));
    }

    @Test
    void blocageApresSeuilAtteint() {
        // Cinq echecs atteignent le seuil.
        for (int i = 0; i < 5; i++) {
            service.enregistrerEchec("user@tady.mg");
        }
        // La verification suivante doit bloquer.
        ApiException ex = assertThrows(ApiException.class,
                () -> service.verifierPasBloque("user@tady.mg"));
        assertEquals(429, ex.getStatut().value());
    }

    @Test
    void succesReinitialiseLeCompteur() {
        service.enregistrerEchec("user@tady.mg");
        service.enregistrerEchec("user@tady.mg");
        service.reinitialiser("user@tady.mg");
        // Apres reinitialisation, aucun blocage.
        assertDoesNotThrow(() -> service.verifierPasBloque("user@tady.mg"));
    }

    @Test
    void emailInconnuPasDeBlocage() {
        assertDoesNotThrow(() -> service.verifierPasBloque("inconnu@tady.mg"));
    }

    @Test
    void cinqCyclesVerificationEtEchecBloqueAuSixiemeControle() {
        String email = "serie@tady.mg";

        for (int i = 0; i < LoginAttemptService.MAX_TENTATIVES; i++) {
            assertDoesNotThrow(() -> service.verifierPasBloque(email));
            service.enregistrerEchec(email);
        }

        ApiException ex = assertThrows(ApiException.class,
                () -> service.verifierPasBloque(email));
        assertEquals(429, ex.getStatut().value());
    }

    @Test
    void verificationSousLeSeuilConserveLeCompteur() {
        String email = "compteur@tady.mg";
        for (int i = 0; i < LoginAttemptService.MAX_TENTATIVES - 1; i++) {
            service.enregistrerEchec(email);
        }

        assertDoesNotThrow(() -> service.verifierPasBloque(email));
        service.enregistrerEchec(email);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.verifierPasBloque(email));
        assertEquals(429, ex.getStatut().value());
    }

    @Test
    void reinitialiserEffaceLeCompteurEtAutoriseUneNouvelleSerie() {
        String email = "succes@tady.mg";
        for (int i = 0; i < LoginAttemptService.MAX_TENTATIVES; i++) {
            service.enregistrerEchec(email);
        }
        service.reinitialiser(email);

        for (int i = 0; i < LoginAttemptService.MAX_TENTATIVES; i++) {
            assertDoesNotThrow(() -> service.verifierPasBloque(email));
            service.enregistrerEchec(email);
        }

        ApiException ex = assertThrows(ApiException.class,
                () -> service.verifierPasBloque(email));
        assertEquals(429, ex.getStatut().value());
    }

    @Test
    void expirationDeLaFenetreFixeReinitialiseLeCompteurAvecClockInjectable() {
        AdjustableClock clock = new AdjustableClock(Instant.parse("2026-10-20T00:00:00Z"));
        LoginAttemptService serviceAvecClock = new LoginAttemptService(clock);
        String email = "expire@tady.mg";

        for (int i = 0; i < LoginAttemptService.MAX_TENTATIVES; i++) {
            serviceAvecClock.enregistrerEchec(email);
        }
        clock.advance(Duration.ofSeconds(LoginAttemptService.DUREE_BLOCAGE_SECONDES));

        // A l'instant exact d'expiration, l'ancien compteur est supprimé.
        assertDoesNotThrow(() -> serviceAvecClock.verifierPasBloque(email));

        for (int i = 0; i < LoginAttemptService.MAX_TENTATIVES; i++) {
            assertDoesNotThrow(() -> serviceAvecClock.verifierPasBloque(email));
            serviceAvecClock.enregistrerEchec(email);
        }

        ApiException ex = assertThrows(ApiException.class,
                () -> serviceAvecClock.verifierPasBloque(email));
        assertEquals(429, ex.getStatut().value());
    }

    private static final class AdjustableClock extends Clock {
        private Instant currentInstant;
        private final ZoneId zone;

        private AdjustableClock(Instant initialInstant) {
            this(initialInstant, ZoneOffset.UTC);
        }

        private AdjustableClock(Instant initialInstant, ZoneId zone) {
            this.currentInstant = initialInstant;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new AdjustableClock(currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }

        private void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }
    }
}
