package mg.emit.tadytrano.infrastructure.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Refuse de lancer l'API hors des profils de developpement/test avec une
 * configuration persistante incomplète ou un secret trop faible.
 */
@Component
@Profile("!local & !test")
public class ConfigurationDemarrageSecurisee implements InitializingBean {

    private final Environment environment;

    public ConfigurationDemarrageSecurisee(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        String url = obligatoire("DB_URL");
        if (!url.startsWith("jdbc:")) {
            throw new IllegalStateException("DB_URL doit contenir une URL JDBC valide.");
        }

        obligatoire("DB_USER");
        obligatoire("DB_PASSWORD");

        String secretJwt = obligatoire("JWT_SECRET");
        if (secretJwt.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET doit contenir au moins 32 octets UTF-8 pour HS256.");
        }
    }

    private String obligatoire(String nom) {
        String valeur = environment.getProperty(nom);
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalStateException(
                    "Variable d'environnement obligatoire absente ou vide : " + nom);
        }
        return valeur;
    }
}
