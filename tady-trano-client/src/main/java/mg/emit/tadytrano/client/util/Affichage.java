package mg.emit.tadytrano.client.util;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import mg.emit.tadytrano.client.api.ErreurApi;

import java.math.BigDecimal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Aides d'affichage partagees par toutes les vues. */
public final class Affichage {

    /** Nombre maximal d'images conservees en memoire (eviction LRU). */
    private static final int TAILLE_CACHE_MAX = 100;

    /** Cache d'image borne en nombre d'entrees, en ordre d'acces (LRU), synchronise. */
    private static final Map<String, Image> CACHE_IMAGES =
            Collections.synchronizedMap(new LinkedHashMap<String, Image>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
                    return size() > TAILLE_CACHE_MAX;
                }
            });

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

    /** Vignette d'image distante, decodee a la taille demandee (et non en taille
     *  reelle), avec cache memoire LRU borne ; placeholder vide si l'URL echoue. */
    public static ImageView vignette(String url, double largeur, double hauteur) {
        ImageView vue = new ImageView();
        vue.setFitWidth(largeur);
        vue.setFitHeight(hauteur);
        vue.setPreserveRatio(true);
        if (url != null && !url.isBlank()) {
            try {
                String cle = url + "|" + (int) largeur + "x" + (int) hauteur;
                Image image = CACHE_IMAGES.computeIfAbsent(cle,
                        k -> new Image(url, largeur, hauteur, true, true, true));
                vue.setImage(image);
            } catch (RuntimeException ignore) {
                // image indisponible : on garde la vignette vide
            }
        }
        return vue;
    }
}
