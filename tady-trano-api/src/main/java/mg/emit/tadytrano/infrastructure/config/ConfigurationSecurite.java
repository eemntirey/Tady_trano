package mg.emit.tadytrano.infrastructure.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;

/**
 * Valide la configuration de securite avant la creation des beans applicatifs.
 *
 * Hors profils {@code local} et {@code test}, les variables DB_URL, DB_USER,
 * DB_PASSWORD et JWT_SECRET doivent etre explicitement fournies. Cela empeche
 * l'application de demarrer en s'appuyant silencieusement sur les valeurs de
 * repli de application.yml.
 */
public final class ConfigurationSecurite
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final int JWT_SECRET_MIN_LENGTH = 32;
    private static final String VALEUR_JWT_PAR_DEFAUT =
            "eX09jEKwZJw105ujFfy9kTXqcce/VG3qSitjNOMbZdcQGhdTcvfl7Vi02J6SvD3LHNepXhrHDPuaTvP8O9pqsg==";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        valider(context.getEnvironment());
    }

    /** Package-private pour permettre des tests unitaires deterministes. */
    void valider(Environment environment) {
        String[] profilsActifs = environment.getActiveProfiles();
        boolean estLocalOuTest = Arrays.stream(profilsActifs)
                .anyMatch(p -> "local".equals(p) || "test".equals(p));
        if (estLocalOuTest) {
            return;
        }

        validerUtilisateurBase(environment);
        validerMotDePasseBase(environment);
        validerSecretJwt(environment);
        validerUrlBaseDonnees(environment);
    }

    private void validerUtilisateurBase(Environment environment) {
        String utilisateur = environment.getProperty("DB_USER");
        if (!StringUtils.hasText(utilisateur)) {
            throw new IllegalStateException(
                    "SECURITE : DB_USER est obligatoire hors des profils local et test. "
                            + "Definissez une variable d'environnement DB_USER.");
        }
    }

    private void validerMotDePasseBase(Environment environment) {
        String password = environment.getProperty("DB_PASSWORD");
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException(
                    "SECURITE : DB_PASSWORD est obligatoire hors des profils local et test. "
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

    private void validerSecretJwt(Environment environment) {
        String secret = environment.getProperty("JWT_SECRET");
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "SECURITE : JWT_SECRET est obligatoire hors des profils local et test. "
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
                            + "Generez un secret fort et definissez la variable d'environnement JWT_SECRET.");
        }
    }

    private void validerUrlBaseDonnees(Environment environment) {
        String url = environment.getProperty("DB_URL");
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException(
                    "SECURITE : DB_URL est obligatoire hors des profils local et test. "
                            + "Definissez une URL JDBC explicite dans la variable d'environnement DB_URL.");
        }

        String normalisee = url.toLowerCase(Locale.ROOT).replace(" ", "");
        if (!normalisee.startsWith("jdbc:")) {
            throw new IllegalStateException(
                    "SECURITE : DB_URL doit etre une URL JDBC valide commencant par 'jdbc:'.");
        }
        if (normalisee.contains("usessl=false")
                || normalisee.contains("sslmode=disabled")
                || normalisee.contains("sslmode=disable")) {
            throw new IllegalStateException(
                    "SECURITE : l'URL de la base de donnees doit activer SSL. "
                            + "Supprimez les parametres qui desactivent SSL de DB_URL.");
        }
        if (normalisee.contains("allowpublickeyretrieval=true")) {
            throw new IllegalStateException(
                    "SECURITE : allowPublicKeyRetrieval=true est interdit. "
                            + "Configurez correctement le certificat SSL et supprimez ce parametre de DB_URL.");
        }
    }
}
