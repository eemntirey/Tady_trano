package mg.emit.tadytrano.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationSecuriteTest {

    @Test
    void configurationPersistanteCompleteEstAcceptee() {
        assertDoesNotThrow(() -> valider(environnementValide()));
    }

    @Test
    void chaqueVariableObligatoireAbsenteOuVideEstRefusee() {
        for (String nom : new String[]{"DB_URL", "DB_USER", "DB_PASSWORD", "JWT_SECRET"}) {
            MockEnvironment environment = environnementValide();
            environment.setProperty(nom, "  ");

            IllegalStateException erreur = assertThrows(
                    IllegalStateException.class, () -> valider(environment), nom);
            assertTrue(erreur.getMessage().contains(nom), erreur.getMessage());
        }
    }

    @Test
    void urlQuiNEstPasMysqlOuMariaDbEstRefusee() {
        MockEnvironment environment = environnementValide();
        environment.setProperty("DB_URL", "jdbc:h2:mem:production");

        IllegalStateException erreur = assertThrows(
                IllegalStateException.class, () -> valider(environment));

        assertTrue(erreur.getMessage().contains("DB_URL"), erreur.getMessage());
    }

    @Test
    void urlSansHoteOuBaseEstRefusee() {
        for (String url : new String[]{"jdbc:mysql://", "jdbc:mysql://db.example.com"}) {
            MockEnvironment environment = environnementValide();
            environment.setProperty("DB_URL", url);
            IllegalStateException erreur = assertThrows(
                    IllegalStateException.class, () -> valider(environment), url);
            assertTrue(erreur.getMessage().contains("DB_URL"), erreur.getMessage());
        }
    }

    @Test
    void desactivationTlsEtRecuperationDeClePubliqueSontRefusees() {
        for (String suffixe : new String[]{
                "?useSSL=false",
                "?sslMode=DISABLED",
                "?allowPublicKeyRetrieval=true"}) {
            MockEnvironment environment = environnementValide();
            environment.setProperty("DB_URL", "jdbc:mysql://db.example.com:3306/tady_trano" + suffixe);
            IllegalStateException erreur = assertThrows(
                    IllegalStateException.class, () -> valider(environment), suffixe);
            assertTrue(erreur.getMessage().contains("SECURITE"), erreur.getMessage());
        }
    }

    @Test
    void profilsLocalEtTestNeDependentPasDesVariablesPersistantes() {
        for (String profil : new String[]{"local", "test"}) {
            MockEnvironment environment = new MockEnvironment();
            environment.setActiveProfiles(profil);
            assertDoesNotThrow(() -> valider(environment), profil);
        }
    }

    private MockEnvironment environnementValide() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DB_URL",
                "jdbc:mysql://db.example.com:3306/tady_trano?sslMode=REQUIRED&serverTimezone=Indian/Antananarivo");
        environment.setProperty("DB_USER", "tady_app");
        environment.setProperty("DB_PASSWORD", "mot-de-passe-production-test");
        environment.setProperty("JWT_SECRET", "secret-jwt-de-test-avec-plus-de-32-caracteres");
        return environment;
    }

    private void valider(MockEnvironment environment) throws Exception {
        new ConfigurationSecurite(environment).run(new DefaultApplicationArguments());
    }
}
