package mg.emit.tadytrano.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationSecuriteTest {

    private final ConfigurationSecurite configuration = new ConfigurationSecurite();

    @Test
    void refuseDemarrageSansDbUrl() {
        MockEnvironment environment = environnementValide();
        environment.getPropertySources().remove("mockProperties");

        environment.setProperty("DB_USER", "tady_app");
        environment.setProperty("DB_PASSWORD", "motdepasse-solide");
        environment.setProperty("JWT_SECRET", "secret-de-test-production-long-de-32-caracteres");
        IllegalStateException erreur = assertThrows(
                IllegalStateException.class, () -> configuration.valider(environment));
        assertTrue(erreur.getMessage().contains("DB_URL"));
    }

    @Test
    void refuseDemarrageAvecDbUrlVide() {
        MockEnvironment environment = environnementValide();
        environment.setProperty("DB_URL", "   ");
        IllegalStateException erreur = assertThrows(
                IllegalStateException.class, () -> configuration.valider(environment));
        assertTrue(erreur.getMessage().contains("DB_URL"));
    }

    @Test
    void refuseDemarrageSansDbUser() {
        MockEnvironment environment = environnementValide();
        environment.getPropertySources().remove("mockProperties");
        environment.setProperty("DB_URL", "jdbc:mysql://db.example:3306/tady_trano?sslMode=REQUIRED");
        environment.setProperty("DB_PASSWORD", "motdepasse-solide");
        environment.setProperty("JWT_SECRET", "secret-de-test-production-long-de-32-caracteres");
        IllegalStateException erreur = assertThrows(
                IllegalStateException.class, () -> configuration.valider(environment));
        assertTrue(erreur.getMessage().contains("DB_USER"));
    }

    @Test
    void refuseDemarrageAvecDbUserVide() {
        MockEnvironment environment = environnementValide();
        environment.setProperty("DB_USER", " ");
        IllegalStateException erreur = assertThrows(
                IllegalStateException.class, () -> configuration.valider(environment));
        assertTrue(erreur.getMessage().contains("DB_USER"));
    }

    @Test
    void refuseUrlQuiNEstPasJdbc() {
        MockEnvironment environment = environnementValide();
        environment.setProperty("DB_URL", "https://db.example/tady_trano");
        IllegalStateException erreur = assertThrows(
                IllegalStateException.class, () -> configuration.valider(environment));
        assertTrue(erreur.getMessage().contains("JDBC"));
    }

    @Test
    void refuseUneUrlQuiDesactiveSslSansTenirCompteDeLaCasse() {
        MockEnvironment environment = environnementValide();
        environment.setProperty("DB_URL", "jdbc:mysql://db.example:3306/tady_trano?USESSL=FALSE");
        IllegalStateException erreur = assertThrows(
                IllegalStateException.class, () -> configuration.valider(environment));
        assertTrue(erreur.getMessage().contains("SSL"));
    }

    @Test
    void refuseDemarrageAvecRecuperationDeClePubliqueActivee() {
        MockEnvironment environment = environnementValide();
        environment.setProperty("DB_URL",
                "jdbc:mysql://db.example:3306/tady_trano?allowPublicKeyRetrieval=true&sslMode=REQUIRED");
        IllegalStateException erreur = assertThrows(
                IllegalStateException.class, () -> configuration.valider(environment));
        assertTrue(erreur.getMessage().contains("allowPublicKeyRetrieval"));
    }

    @Test
    void accepteUneConfigurationProductionExpliciteEtSecurisee() {
        assertDoesNotThrow(() -> configuration.valider(environnementValide()));
    }

    @Test
    void profilsLocalEtTestNeNecessitentPasLesVariablesDeProduction() {
        MockEnvironment local = new MockEnvironment();
        local.setActiveProfiles("local");
        assertDoesNotThrow(() -> configuration.valider(local));

        MockEnvironment test = new MockEnvironment();
        test.setActiveProfiles("test");
        assertDoesNotThrow(() -> configuration.valider(test));
    }

    private static MockEnvironment environnementValide() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        environment.setProperty("DB_URL",
                "jdbc:mysql://db.example:3306/tady_trano?sslMode=REQUIRED&serverTimezone=UTC");
        environment.setProperty("DB_USER", "tady_app");
        environment.setProperty("DB_PASSWORD", "motdepasse-solide");
        environment.setProperty("JWT_SECRET", "secret-de-test-production-long-de-32-caracteres");
        return environment;
    }
}
