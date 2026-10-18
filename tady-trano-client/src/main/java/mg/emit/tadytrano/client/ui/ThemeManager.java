package mg.emit.tadytrano.client.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;

/**
 * Gestion du theme de l'application (CDC 8.3 : theme clair et sombre).
 * Bascule dynamiquement entre Primer Light et Primer Dark d'AtlantaFX.
 */
public class ThemeManager {

    private boolean modeSombre = false;
    private Scene scene;

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

    private void appliquerTheme() {
        if (modeSombre) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }

        if (scene != null) {
            String cssPath = getClass().getResource("/mg/emit/tadytrano/client/css/styles.css").toExternalForm();
            if (!scene.getStylesheets().contains(cssPath)) {
                scene.getStylesheets().add(cssPath);
            }
        }
    }
}
