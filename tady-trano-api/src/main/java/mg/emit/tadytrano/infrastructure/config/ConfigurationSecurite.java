package mg.emit.tadytrano.infrastructure.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Valide la configuration de securite au demarrage de l'application.
 *
 * Ce composant est active uniquement hors profils {@code local} et
 * {@code test}. Il refuse le demarrage si :
 * <ul>
 *   <li>{@code DB_PASSWORD} est absente, vide ou egale a la valeur par defaut
 *       dangereuse {@code rootpass} ;</li>
 *   <li>{@code JWT_SECRET} est absent, vide ou trop court (moins de 32
 *       caracteres) ;</li>
 *   <li>l'URL JDBC desactive SSL ({@code useSSL=false}) ou autorise la
 *       recuperation de cle publique ({@code allowPublicKeyRetrieval=true}).</li>
 * </ul>
 *
 * L'objectif est d'empecher le demarrage d'une instance exposee sur un
 * reseau non fiable avec une configuration par defaut insecure.
 */
@Configuration
public class ConfigurationSecurite implements ApplicationRunner {

    private static final int JWT_SECRET_MIN_LENGTH = 32;
    private static final String VALEUR_JWT_PAR_DEFAUT =
            "eX09jEKwZJw105ujFfy9kTXqcce/VG3qSitjNOMbZdcQGhdTcvfl7Vi02J6SvD3LHNepXhrHDPuaTvP8O9pqsg==";

    private final Environment environment;

    public ConfigurationSecurite(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] profilsActifs = environment.getActiveProfiles();
        boolean estLocalOuTest = java.util.Arrays.stream(profilsActifs)
                .anyMatch(p -> "local".equals(p) || "test".equals(p));
        if (estLocalOuTest) {
            return; // Aucune restriction pour les profils locaux et de tests.
        }

        validerMotDePasseBase();
        validerSecretJwt();
        validerUrlBaseDonnees();
    }

    private void validerMotDePasseBase() {
        String password = environment.getProperty("DB_PASSWORD");
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException(
                    "SECURITE : DB_PASSWORD est obligatoire en production. "
                            + "Definissez une variable d'environnement DB_PASSWORD forte avant de demarrer.");
        }
        if ("rootpass".equals(password)) {
            throw new IllegalStateException(
                    "SECURITE : DB_PASSWORD ne doit pas utiliser la valeur par defaut 'rootpass'. "
                            + "Definissez un mot de passe base de donnees fort.");
        }
        if (password.length() < 8) {
            throw new IllegalStateException(
                    "SECURITE : DB_PASSWORD est trop court (minimum 8 caracteres).");
        }
    }

    private void validerSecretJwt() {
        String secret = environment.getProperty("JWT_SECRET");
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "SECURITE : JWT_SECRET est obligatoire en production. "
                            + "Definissez une variable d'environnement JWT_SECRET forte avant de demarrer.");
        }
        if (secret.length() < JWT_SECRET_MIN_LENGTH) {
            throw new IllegalStateException(
                    "SECURITE : JWT_SECRET est trop court (minimum %d caracteres)."
                            .formatted(JWT_SECRET_MIN_LENGTH));
        }
        if (VALEUR_JWT_PAR_DEFAUT.equals(secret)) {
            throw new IllegalStateException(
                    "SECURITE : JWT_SECRET ne doit pas utiliser la valeur par defaut contenue dans le depot. "
                            + "Generez un secret fort et definiant la variable d'environnement JWT_SECRET.");
        }
    }

    private void validerUrlBaseDonnees() {
        String url = environment.getProperty("DB_URL");
        if (StringUtils.hasText(url)) {
            if (url.contains("useSSL=false")) {
                throw new IllegalStateException(
                        "SECURITE : l'URL de la base de donnees doit activer SSL (useSSL=true). "
                                + "Supprimez useSSL=false de DB_URL.");
            }
            if (url.contains("allowPublicKeyRetrieval=true")) {
                throw new IllegalStateException(
                        "SECURITE : allowPublicKeyRetrieval=true est interdit. "
                                + "Supprimez ce parametre de DB_URL et configurez correctement le certificat SSL.");
            }
        }
    }
}
