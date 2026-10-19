package mg.emit.tadytrano.client.ui;

import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.dto.AnnonceResumeResponse;
import netscape.javascript.JSObject;

import java.net.URL;
import java.util.List;

/**
 * Composant de carte Leaflet OpenStreetMap integre via un WebView JavaFX (CDC F7).
 */
public class CarteView extends StackPane {

    private final WebView webView = new WebView();
    private final WebEngine webEngine = webView.getEngine();
    private final CarteBridge bridge = new CarteBridge();
    private boolean pageChargee = false;

    private List<AnnonceResumeResponse> annoncesEnAttente;
    private Double latAttente;
    private Double lngAttente;
    private String labelAttente;
    /** [lat, lng, zoom] si un centrage est demande avant le chargement de la page. */
    private Double[] centrageEnAttente;

    public CarteView() {
        getChildren().add(webView);

        webEngine.setJavaScriptEnabled(true);
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject win = (JSObject) webEngine.executeScript("window");
                win.setMember("javaBridge", bridge);
                pageChargee = true;

                // Le evenement window.load de map.html a pu se declencher AVANT
                // l'injection du pont : on notifie explicitement que la carte est prete.
                bridge.onMapReady();

                if (annoncesEnAttente != null) {
                    afficherAnnonces(annoncesEnAttente);
                    annoncesEnAttente = null;
                }
                if (latAttente != null && lngAttente != null) {
                    definirPositionUtilisateur(latAttente, lngAttente, labelAttente);
                    latAttente = null;
                    lngAttente = null;
                    labelAttente = null;
                }
                if (centrageEnAttente != null) {
                    webEngine.executeScript("flyTo(" + centrageEnAttente[0] + ", "
                            + centrageEnAttente[1] + ", " + centrageEnAttente[2].intValue() + ");");
                    centrageEnAttente = null;
                }
            }
        });

        URL mapUrl = getClass().getResource("/mg/emit/tadytrano/client/web/map.html");
        if (mapUrl != null) {
            webEngine.load(mapUrl.toExternalForm());
        }
    }

    public CarteBridge getBridge() {
        return bridge;
    }

    public void afficherAnnonces(List<AnnonceResumeResponse> annonces) {
        if (!pageChargee) {
            this.annoncesEnAttente = annonces;
            return;
        }
        try {
            String json = ApiClient.serialiser(annonces);
            webEngine.executeScript("setMarkers(" + json + ");");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void definirPositionUtilisateur(Double lat, Double lng, String label) {
        if (!pageChargee) {
            this.latAttente = lat;
            this.lngAttente = lng;
            this.labelAttente = label;
            return;
        }
        if (lat != null && lng != null
                && Double.isFinite(lat) && Double.isFinite(lng)
                && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {
            String lbl = ApiClient.serialiser(label == null ? "" : label)
                    .replace(String.valueOf((char) 0x2028), "\\u2028")
                    .replace(String.valueOf((char) 0x2029), "\\u2029");
            webEngine.executeScript("setUserLocation(" + lat + ", " + lng + ", " + lbl + ");");
        }
    }

    /** Retire le marqueur de position (point de repere efface). */
    public void effacerPositionUtilisateur() {
        if (!pageChargee) {
            latAttente = null;
            lngAttente = null;
            labelAttente = null;
            return;
        }
        // Cote JS, une latitude non numerique supprime le marqueur existant.
        webEngine.executeScript("setUserLocation(null, null, '');");
    }

    public void centrerSur(Double lat, Double lng, int zoom) {
        if (lat == null || lng == null
                || !Double.isFinite(lat) || !Double.isFinite(lng)
                || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return;
        }
        if (!pageChargee) {
            // Clic trop tot : on memorise le centrage pour l'appliquer apres le chargement.
            centrageEnAttente = new Double[] { lat, lng, (double) zoom };
            return;
        }
        webEngine.executeScript("flyTo(" + lat + ", " + lng + ", " + zoom + ");");
    }
}
