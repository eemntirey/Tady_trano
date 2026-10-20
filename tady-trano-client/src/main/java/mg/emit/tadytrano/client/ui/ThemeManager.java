package mg.emit.tadytrano.client.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.Parent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Gestion du theme global. La classe CSS du root suit le theme courant afin
 * que les styles a contraste renforce restent coherents lors de la bascule.
 * Les composants WebView (carte) s'abonnent pour appliquer le meme theme.
 */
public class ThemeManager {

    private static final ThemeManager INSTANCE = new ThemeManager();

    private boolean modeSombre = false;
    private Scene scene;
    private final List<Consumer<Boolean>> ecouteurs = new CopyOnWriteArrayList<>();

    /** Instance partagee : la carte doit suivre la bascule meme hors de Application. */
    public static ThemeManager instance() {
        return INSTANCE;
    }

    public void initialiser(Scene scene) {
        this.scene = scene;
        appliquerTheme();
    }

    public void basculerTheme() {
        modeSombre = !modeSombre;
        appliquerTheme();
    }

    public boolean estModeSombre() {
        return modeSombre;
    }

    /** Abonne un composant (ex. WebView carte) aux changements de theme. */
    public void ajouterEcouteur(Consumer<Boolean> ecouteur) {
        ecouteurs.add(ecouteur);
        ecouteur.accept(modeSombre);
    }

    private void appliquerTheme() {
        Application.setUserAgentStylesheet(modeSombre
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());

        if (scene == null) {
            return;
        }

        Parent root = scene.getRoot();
        if (root != null) {
            root.getStyleClass().removeAll("theme-dark", "theme-light");
            root.getStyleClass().add(modeSombre ? "theme-dark" : "theme-light");
        }

        var css = getClass().getResource("/mg/emit/tadytrano/client/css/styles.css");
        if (css != null) {
            String cssPath = css.toExternalForm();
            if (!scene.getStylesheets().contains(cssPath)) {
                scene.getStylesheets().add(cssPath);
            }
        }

        for (Consumer<Boolean> ecouteur : ecouteurs) {
            ecouteur.accept(modeSombre);
        }
    }
}
