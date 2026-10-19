package mg.emit.tadytrano.client.ui;

import javafx.application.Platform;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Pont de communication Java ⇄ JavaScript pour la carte Leaflet (WebView).
 */
public class CarteBridge {

    private Consumer<Long> annonceClickListener;
    private BiConsumer<Double, Double> mapClickListener;
    private Runnable mapReadyListener;

    public void setOnAnnonceClick(Consumer<Long> listener) {
        this.annonceClickListener = listener;
    }

    public void setOnMapClick(BiConsumer<Double, Double> listener) {
        this.mapClickListener = listener;
    }

    public void setOnMapReady(Runnable listener) {
        this.mapReadyListener = listener;
    }

    /** Appele depuis le JavaScript Leaflet lorsqu'un marqueur est clique. */
    public void onAnnonceClick(int id) {
        Platform.runLater(() -> {
            if (annonceClickListener != null) {
                annonceClickListener.accept((long) id);
            }
        });
    }

    /** Appele depuis le JavaScript Leaflet lorsqu'un clic est effectue sur la carte. */
    public void onMapClick(double lat, double lng) {
        Platform.runLater(() -> {
            if (mapClickListener != null) {
                mapClickListener.accept(lat, lng);
            }
        });
    }

    /** Appele lorsque la carte Leaflet est prete et chargee (une seule fois,
     *  meme si le JS rappelle : evite de charger les annonces deux fois). */
    public void onMapReady() {
        Platform.runLater(() -> {
            Runnable listener = mapReadyListener;
            // One-shot : on detruit l'ecouteur AVANT l'appel (re-entrant possible).
            mapReadyListener = null;
            if (listener != null) {
                listener.run();
            }
        });
    }
}
