package mg.emit.tadytrano.client.util;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import mg.emit.tadytrano.client.api.ErreurApi;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Aides d'affichage partagees par toutes les vues. */
public final class Affichage {

    /** Nombre maximal d'images conservees en memoire (eviction LRU). */
    private static final int TAILLE_CACHE_MAX = 100;

    /** Cache LRU des images chargees ou en cours de chargement. */
    private static final Map<String, Image> CACHE_IMAGES =
            Collections.synchronizedMap(new LinkedHashMap<String, Image>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
                    return size() > TAILLE_CACHE_MAX;
                }
            });

    /** Les placeholders sont reutilises par dimension, sans acces reseau. */
    private static final Map<String, Image> CACHE_PLACEHOLDERS =
            Collections.synchronizedMap(new LinkedHashMap<>());

    private static final AtomicLong GENERATIONS = new AtomicLong();
    private static final Object CLE_GENERATION = new Object();

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

    /** Vignette d'image distante. Le placeholder reste visible pendant le chargement
     * et remplace l'image en cas d'echec. */
    public static ImageView vignette(String url, double largeur, double hauteur) {
        ImageView vue = new ImageView();
        charger(vue, url, largeur, hauteur);
        return vue;
    }

    /**
     * Charge une image dans une ImageView existante. Cette methode est utile pour
     * l'image principale du detail : elle sera mise a jour a la fin du chargement,
     * contrairement a un simple getImage() pris pendant le chargement asynchrone.
     */
    public static void charger(ImageView vue, String url, double largeur, double hauteur) {
        Objects.requireNonNull(vue, "vue");
        vue.setFitWidth(largeur);
        vue.setFitHeight(hauteur);
        vue.setPreserveRatio(true);

        long generation = GENERATIONS.incrementAndGet();
        vue.getProperties().put(CLE_GENERATION, generation);

        Image placeholder = placeholder(largeur, hauteur);
        vue.setImage(placeholder);

        String urlCompatible = urlImageCompatible(url);
        if (urlCompatible == null) {
            return;
        }

        String cle = urlCompatible + "|" + (int) largeur + "x" + (int) hauteur;
        Image image;
        try {
            synchronized (CACHE_IMAGES) {
                image = CACHE_IMAGES.get(cle);
                if (image != null && image.isError()) {
                    CACHE_IMAGES.remove(cle);
                    image = null;
                }
                if (image == null) {
                    image = new Image(urlCompatible, largeur, hauteur, true, true, true);
                    if (!image.isError()) {
                        CACHE_IMAGES.put(cle, image);
                    }
                }
            }
        } catch (RuntimeException ex) {
            retirerImageEnErreur(cle, null);
            return;
        }

        if (image.isError()) {
            retirerImageEnErreur(cle, image);
            return;
        }

        if (image.getProgress() >= 1.0) {
            if (image.getWidth() > 0 && image.getHeight() > 0) {
                vue.setImage(image);
            } else {
                retirerImageEnErreur(cle, image);
            }
            return;
        }

        new SuiviChargement(vue, image, placeholder, cle, generation).attacher();
    }

    /**
     * Valide les URL externes et force JPEG pour images.unsplash.com. Le parametre
     * auto=format peut negocier AVIF/WebP selon le client, formats non pris en charge
     * par tous les decodeurs JavaFX ; fm=jpg rend le resultat deterministe.
     */
    static String urlImageCompatible(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null || uri.getUserInfo() != null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return null;
            }

            if (!"images.unsplash.com".equalsIgnoreCase(uri.getHost())) {
                return uri.toASCIIString();
            }

            List<String> parametres = new ArrayList<>();
            String query = uri.getRawQuery();
            if (query != null && !query.isBlank()) {
                for (String parametre : query.split("&")) {
                    if (parametre.isBlank()) {
                        continue;
                    }
                    int egal = parametre.indexOf('=');
                    String nom = egal >= 0 ? parametre.substring(0, egal) : parametre;
                    String valeur = egal >= 0 ? parametre.substring(egal + 1) : "";
                    if ("fm".equalsIgnoreCase(nom)
                            || ("auto".equalsIgnoreCase(nom) && "format".equalsIgnoreCase(valeur))) {
                        continue;
                    }
                    parametres.add(parametre);
                }
            }
            parametres.add("fm=jpg");

            return uri.getScheme() + "://" + uri.getRawAuthority() + uri.getRawPath()
                    + "?" + String.join("&", parametres);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Image placeholder(double largeur, double hauteur) {
        int w = Math.max(1, (int) Math.ceil(largeur));
        int h = Math.max(1, (int) Math.ceil(hauteur));
        String cle = w + "x" + h;
        synchronized (CACHE_PLACEHOLDERS) {
            return CACHE_PLACEHOLDERS.computeIfAbsent(cle, k -> creerPlaceholder(w, h));
        }
    }

    /** Dessine une petite maison neutre dans une vignette, sans ressource externe. */
    private static Image creerPlaceholder(int largeur, int hauteur) {
        WritableImage image = new WritableImage(largeur, hauteur);
        PixelWriter pixels = image.getPixelWriter();
        Color fond = Color.rgb(241, 244, 248);
        Color maison = Color.rgb(157, 168, 181);
        Color porte = Color.rgb(117, 130, 145);

        double gauche = largeur * 0.30;
        double droite = largeur * 0.70;
        double centre = largeur * 0.50;
        double pointeY = hauteur * 0.25;
        double bordY = hauteur * 0.48;
        double epaisseur = Math.max(1.0, Math.min(largeur, hauteur) * 0.025);

        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                double dx = x + 0.5;
                double dy = y + 0.5;
                boolean toit = false;
                if (dx >= gauche && dx <= centre) {
                    double attendu = pointeY + (dx - gauche) * (bordY - pointeY) / (centre - gauche);
                    toit = Math.abs(dy - attendu) <= epaisseur;
                } else if (dx > centre && dx <= droite) {
                    double attendu = pointeY + (droite - dx) * (bordY - pointeY) / (droite - centre);
                    toit = Math.abs(dy - attendu) <= epaisseur;
                }

                boolean murs = dx >= largeur * 0.35 && dx <= largeur * 0.65
                        && dy >= bordY && dy <= hauteur * 0.76;
                boolean entree = dx >= largeur * 0.46 && dx <= largeur * 0.54
                        && dy >= hauteur * 0.60 && dy <= hauteur * 0.76;
                boolean fenetre = dx >= largeur * 0.385 && dx <= largeur * 0.425
                        && dy >= hauteur * 0.54 && dy <= hauteur * 0.60;

                pixels.setColor(x, y, entree ? porte : (toit || murs || fenetre ? maison : fond));
            }
        }
        return image;
    }

    private static void retirerImageEnErreur(String cle, Image image) {
        synchronized (CACHE_IMAGES) {
            if (image == null || CACHE_IMAGES.get(cle) == image) {
                CACHE_IMAGES.remove(cle);
            }
        }
    }

    private static void surThreadJavaFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    private static final class SuiviChargement
            implements ChangeListener<Number>, ChangeListener<Boolean> {

        private final ImageView vue;
        private final Image image;
        private final Image placeholder;
        private final String cle;
        private final long generation;
        private boolean termine;

        private SuiviChargement(ImageView vue, Image image, Image placeholder,
                                String cle, long generation) {
            this.vue = vue;
            this.image = image;
            this.placeholder = placeholder;
            this.cle = cle;
            this.generation = generation;
        }

        private void attacher() {
            image.progressProperty().addListener(this);
            image.errorProperty().addListener(this);
            verifier();
        }

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number ancien, Number nouveau) {
            verifier();
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean ancien, Boolean nouveau) {
            verifier();
        }

        private void verifier() {
            if (termine) {
                return;
            }
            if (image.isError()) {
                terminer(false);
            } else if (image.getProgress() >= 1.0) {
                terminer(image.getWidth() > 0 && image.getHeight() > 0);
            }
        }

        private void terminer(boolean succes) {
            if (termine) {
                return;
            }
            termine = true;
            image.progressProperty().removeListener(this);
            image.errorProperty().removeListener(this);

            if (!succes) {
                retirerImageEnErreur(cle, image);
            }

            surThreadJavaFx(() -> {
                Object generationCourante = vue.getProperties().get(CLE_GENERATION);
                if (!(generationCourante instanceof Long valeur) || valeur.longValue() != generation) {
                    return; // une autre URL a ete demandee entre-temps
                }
                vue.setImage(succes ? image : placeholder);
            });
        }
    }
}
