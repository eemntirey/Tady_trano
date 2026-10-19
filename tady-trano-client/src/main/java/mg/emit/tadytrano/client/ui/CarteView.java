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

    public CarteView() {
        getChildren().add(webView);

        webEngine.setJavaScriptEnabled(true);
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject win = (JSObject) webEngine.executeScript("window");
                win.setMember("javaBridge", bridge);
                pageChargee = true;

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

    public void centrerSur(Double lat, Double lng, int zoom) {
        if (pageChargee && lat != null && lng != null) {
            webEngine.executeScript("flyTo(" + lat + ", " + lng + ", " + zoom + ");");
        }
    }
}
