package mg.emit.tadytrano.client.util;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import mg.emit.tadytrano.client.api.ErreurApi;

import java.math.BigDecimal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Aides d'affichage partagees par toutes les vues. */
public final class Affichage {

    private static final Map<String, Image> CACHE_IMAGES = new ConcurrentHashMap<>();

    private Affichage() {
    }

    /** Alerte d'erreur avec le message serveur si disponible. */
    public static void erreur(Exception e) {
        String message = (e instanceof ErreurApi api) ? api.getMessage() : e.getMessage();
        Alert alert = new Alert(Alert.AlertType.ERROR,
                message == null || message.isBlank() ? "Erreur inattendue" : message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /** Prix en ariary : 450000 -> "450 000 Ar". */
    public static String prix(BigDecimal valeur) {
        if (valeur == null) {
            return "-";
        }
        return String.format("%,.0f Ar", valeur).replace(',', ' ');
    }

    public static String distance(Double km) {
        return km == null ? null : String.format("%.1f km", km);
    }

    /** Vignette d'image distante avec cache memoire ; placeholder vide si l'URL echoue. */
    public static ImageView vignette(String url, double largeur, double hauteur) {
        ImageView vue = new ImageView();
        vue.setFitWidth(largeur);
        vue.setFitHeight(hauteur);
        vue.setPreserveRatio(true);
        if (url != null && !url.isBlank()) {
            try {
                Image image = CACHE_IMAGES.computeIfAbsent(url, u -> new Image(u, true));
                vue.setImage(image);
            } catch (RuntimeException ignore) {
                // image indisponible : on garde la vignette vide
            }
        }
        return vue;
    }
}
