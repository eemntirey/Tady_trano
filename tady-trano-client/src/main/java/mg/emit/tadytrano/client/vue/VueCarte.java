package mg.emit.tadytrano.client.vue;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mg.emit.tadytrano.client.TadyTranoApp;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.dto.AnnonceResumeResponse;
import mg.emit.tadytrano.client.ui.CarteView;

import java.util.List;
import java.util.Map;

/**
 * Vue cartographique interactive Leaflet & OpenStreetMap (CDC F7).
 * Affiche la repartition geographique des annonces avec marqueurs et popups.
 */
public class VueCarte extends VBox {

    private final TadyTranoApp app;
    private final ApiClient api;
    private final CarteView carteView = new CarteView();
    private final Label infoLabel = new Label("Chargement de la carte...");

    public VueCarte(TadyTranoApp app, ApiClient api) {
        this.app = app;
        this.api = api;
        construire();
        initialiserCarte();
    }

    private void construire() {
        setSpacing(10);
        setPadding(new Insets(15));
        VBox.setVgrow(carteView, Priority.ALWAYS);

        HBox barreOutils = new HBox(12);
        barreOutils.setAlignment(Pos.CENTER_LEFT);

        Label titre = new Label("Carte Interactive des Logements (Leaflet / OSM)");
        titre.setFont(Font.font("System", FontWeight.BOLD, 18));

        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);

        Button btnFianar = new Button("Fianarantsoa");
        btnFianar.setOnAction(e -> carteView.centrerSur(-21.4536, 47.0858, 14));

        Button btnTana = new Button("Antananarivo");
        btnTana.setOnAction(e -> carteView.centrerSur(-18.8792, 47.5079, 13));

        Button btnRafraichir = new Button("Actualiser");
        btnRafraichir.setOnAction(e -> chargerAnnonces());

        barreOutils.getChildren().addAll(titre, espace, new Label("Centrer sur :"), btnFianar, btnTana, btnRafraichir);

        infoLabel.getStyleClass().add("muted");

        getChildren().addAll(barreOutils, infoLabel, new Separator(), carteView);
    }

    private void initialiserCarte() {
        // Clic sur un marqueur de la carte -> ouverture du detail
        carteView.getBridge().setOnAnnonceClick(annonceId -> {
            app.ouvrirDetail(annonceId, () -> app.ouvrir(this));
        });

        carteView.getBridge().setOnMapReady(this::chargerAnnonces);
    }

    private void chargerAnnonces() {
        infoLabel.setText("Chargement des coordonnées des annonces...");
        app.appeler(() -> api.rechercher(Map.of(), 0, 100), reponse -> {
            List<AnnonceResumeResponse> liste = reponse.contenu();
            carteView.afficherAnnonces(liste);
            infoLabel.setText(liste.size() + " annonce(s) positionnée(s) sur la carte. Cliquez sur un repère pour voir les détails.");
        });
    }
}
