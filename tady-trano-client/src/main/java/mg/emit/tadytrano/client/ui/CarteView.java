package mg.emit.tadytrano.client.ui;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.dto.AnnonceResumeResponse;
import netscape.javascript.JSObject;

import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

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
    /** Dernier theme applique en JS (evite d'executer le script pour rien). */
    private Boolean themeApplique;
    private boolean liberee;
    /** Même instance utilisée pour l'inscription et le retrait de l'écouteur. */
    private final Consumer<Boolean> ecouteTheme = sombre -> {
        if (!liberee && pageChargee) {
            appliquerThemeCarte(sombre);
        }
    };
    /** Garder la référence pour pouvoir détacher aussi l'écouteur du chargement WebView. */
    private final ChangeListener<Worker.State> ecouteurChargement;

    public CarteView() {
        getChildren().add(webView);

        webEngine.setJavaScriptEnabled(true);
        this.ecouteurChargement = (obs, oldState, newState) -> {
            if (liberee) {
                return;
            }
            if (newState == Worker.State.SUCCEEDED) {
                JSObject win = (JSObject) webEngine.executeScript("window");
                win.setMember("javaBridge", bridge);
                pageChargee = true;

                // Le evenement window.load de map.html a pu se declencher AVANT
                // l'injection du pont : on notifie explicitement que la carte est prete.
                bridge.onMapReady();

                // Applique le theme courant (clair/sombre) des le chargement.
                themeApplique = null;
                appliquerThemeCarte(ThemeManager.instance().estModeSombre());

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
        };
        webEngine.getLoadWorker().stateProperty().addListener(ecouteurChargement);

        // Suit les bascules de l'application : la carte Leaflet doit rester coherente.
        ThemeManager.instance().ajouterEcouteur(ecouteTheme);

        URL mapUrl = getClass().getResource("/mg/emit/tadytrano/client/web/map.html");
        if (mapUrl != null) {
            webEngine.load(mapUrl.toExternalForm());
        }
    }

    /**
     * Libère la WebView quand sa VueCarte n'est plus ni affichée ni conservée dans l'historique.
     * À appeler sur le JavaFX Application Thread. Les appels répétés sont sans effet.
     */
    public void liberer() {
        if (liberee) {
            return;
        }
        liberee = true;

        ThemeManager.instance().retirerEcouteur(ecouteTheme);
        webEngine.getLoadWorker().stateProperty().removeListener(ecouteurChargement);
        bridge.setOnAnnonceClick(null);
        bridge.setOnMapClick(null);
        bridge.setOnMapReady(null);

        pageChargee = false;
        annoncesEnAttente = null;
        latAttente = null;
        lngAttente = null;
        labelAttente = null;
        centrageEnAttente = null;

        // Décharge le document et son pont JavaScript; cette WebView ne sera plus réutilisée.
        webEngine.load(null);
        getChildren().clear();
    }

    /** Pousse le theme dans map.html (tuiles + CSS popups). */
    private void appliquerThemeCarte(boolean sombre) {
        if (Boolean.valueOf(sombre).equals(themeApplique)) {
            return;
        }
        themeApplique = sombre;
        try {
            webEngine.executeScript("setMapTheme(" + (sombre ? "true" : "false") + ");");
        } catch (Exception e) {
            e.printStackTrace();
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
