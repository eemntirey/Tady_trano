package mg.emit.tadytrano.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationDemarrageSecuriseeTest {

    @Test
    void configurationCompleteEtSecretSuffisammentLongAcceptes() {
        ConfigurationDemarrageSecurisee garde =
                new ConfigurationDemarrageSecurisee(environnementValide());

        assertDoesNotThrow(garde::afterPropertiesSet);
    }

    @Test
    void chaqueVariableObligatoireAbsenteOuVideEstRefusee() {
        for (String nom : new String[]{"DB_URL", "DB_USER", "DB_PASSWORD", "JWT_SECRET"}) {
            MockEnvironment environment = environnementValide();
            environment.setProperty(nom, "  ");

            IllegalStateException erreur = assertThrows(
                    IllegalStateException.class,
                    () -> new ConfigurationDemarrageSecurisee(environment).afterPropertiesSet(),
                    nom);
            assertTrue(erreur.getMessage().contains(nom), erreur.getMessage());
        }
    }

    @Test
    void secretJwtTropCourtEstRefuseAuDemarrage() {
        MockEnvironment environment = environnementValide();
        environment.setProperty("JWT_SECRET", "secret-trop-court");

        IllegalStateException erreur = assertThrows(
                IllegalStateException.class,
                () -> new ConfigurationDemarrageSecurisee(environment).afterPropertiesSet());

        assertTrue(erreur.getMessage().contains("32 octets"), erreur.getMessage());
    }

    private MockEnvironment environnementValide() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DB_URL", "jdbc:mysql://localhost/tady_trano");
        environment.setProperty("DB_USER", "tady_app");
        environment.setProperty("DB_PASSWORD", "mot-de-passe-test");
        environment.setProperty("JWT_SECRET", "secret-jwt-de-test-avec-plus-de-32-octets");
        return environment;
    }
}
