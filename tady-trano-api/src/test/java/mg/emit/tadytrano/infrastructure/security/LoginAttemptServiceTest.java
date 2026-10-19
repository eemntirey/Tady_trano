package mg.emit.tadytrano.infrastructure.security;

import mg.emit.tadytrano.application.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
