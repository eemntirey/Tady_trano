package mg.emit.tadytrano.infrastructure.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Locale;

/**
 * Valide la configuration de securite au demarrage de l'application.
 *
 * Ce composant est active uniquement hors profils {@code local} et
 * {@code test}. Il refuse le demarrage si :
 * <ul>
 *   <li>{@code DB_URL}, {@code DB_USER} ou {@code DB_PASSWORD} sont absentes
 *       ou vides ;</li>
 *   <li>l'URL n'est pas une URL MySQL/MariaDB complete ou desactive TLS ;</li>
 *   <li>{@code JWT_SECRET} est absent, vide, trop court ou correspond au
 *       secret de developpement inclus dans le depot.</li>
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
        boolean estLocalOuTest = profilsActifs.length > 0
                && java.util.Arrays.stream(profilsActifs)
                        .allMatch(p -> "local".equals(p) || "test".equals(p));
        if (estLocalOuTest) {
            return; // Aucune restriction pour les profils locaux et de tests.
        }

        validerUrlBaseDonnees();
        validerUtilisateurBase();
        validerMotDePasseBase();
        validerSecretJwt();
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

    private void validerUtilisateurBase() {
        obligatoire("DB_USER");
    }

    private void validerUrlBaseDonnees() {
        String url = obligatoire("DB_URL").trim();
        if (!url.startsWith("jdbc:mysql://") && !url.startsWith("jdbc:mariadb://")) {
            throw new IllegalStateException(
                    "SECURITE : DB_URL doit etre une URL JDBC MySQL ou MariaDB complete.");
        }

        final URI uri;
        try {
            // URI attend "mysql://..." ou "mariadb://...", sans le prefixe JDBC.
            uri = URI.create(url.substring("jdbc:".length()));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("SECURITE : DB_URL est mal formee.", e);
        }
        if (!StringUtils.hasText(uri.getHost()) || uri.getPath() == null
                || uri.getPath().length() <= 1 || !StringUtils.hasText(uri.getPath().substring(1))) {
            throw new IllegalStateException(
                    "SECURITE : DB_URL doit preciser un hote et une base de donnees.");
        }

        String normalisee = url.toLowerCase(Locale.ROOT);
        if (normalisee.contains("usessl=false") || normalisee.contains("sslmode=disabled")) {
            throw new IllegalStateException(
                    "SECURITE : l'URL de la base de donnees ne doit pas desactiver TLS.");
        }
        if (normalisee.contains("allowpublickeyretrieval=true")) {
            throw new IllegalStateException(
                    "SECURITE : allowPublicKeyRetrieval=true est interdit. "
                            + "Configurez correctement le chiffrement et les certificats TLS.");
        }
    }

    private String obligatoire(String nom) {
        String valeur = environment.getProperty(nom);
        if (!StringUtils.hasText(valeur)) {
            throw new IllegalStateException(
                    "SECURITE : la variable " + nom + " est obligatoire hors des profils local/test.");
        }
        return valeur;
    }
}
