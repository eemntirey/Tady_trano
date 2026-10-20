package mg.emit.tadytrano.client.vue;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vue cartographique interactive Leaflet & OpenStreetMap (CDC F7).
 * Affiche la repartition geographique des annonces avec marqueurs et popups.
 *
 * Seules les annonces positionnees sont marquees (coordonnees propres, avec
 * repli sur celles du quartier) : le message de la vue le dit et signale toute
 * troncature. Le chargement parcourt toutes les pages (D4). Un point de repere
 * pose par clic trie les annonces par distance (D9).
 */
public class VueCarte extends VBox {

    /** Taille de page cote API (plafond serveur : TAILLE_MAX_PAGE = 50). */
    private static final int TAILLE_PAGE = 50;
    /** Plafond de pages chargees : au-dela, le message signale la troncature. */
    private static final int PAGES_MAX = 20;

    private final TadyTranoApp app;
    private final ApiClient api;
    private final CarteView carteView = new CarteView();
    private final Label infoLabel = new Label("Chargement de la carte...");
    private final ToggleButton repere = new ToggleButton("Point de repère");
    /** Evite le double appel API : onMapReady() peut etre notifie deux fois
     *  (sondage JS + notification Java apres SUCCEEDED). */
    private boolean chargementLance = false;
    /** Position de reference posee sur la carte (D9) : tri par distance. */
    private Double latRepere;
    private Double lngRepere;
    /** Sequence des chargements : ignore les reponses perimees. */
    private int generation = 0;
    private boolean liberee;

    public VueCarte(TadyTranoApp app, ApiClient api) {
        this.app = app;
        this.api = api;
        construire();
        initialiserCarte();
    }

    /**
     * À appeler uniquement lorsque cette vue est retirée de l'historique et abandonnée.
     * Invalide les réponses API encore en vol avant de libérer la WebView.
     */
    public void liberer() {
        if (liberee) {
            return;
        }
        liberee = true;
        generation++;
        carteView.liberer();
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

        // D9 : le point de repere sert aussi de position de reference pour le tri.
        repere.getStyleClass().add("chip");
        repere.setTooltip(new Tooltip(
                "Activez, puis cliquez sur la carte : les annonces sont triées par distance de ce point."));
        repere.setOnAction(e -> {
            if (repere.isSelected()) {
                infoLabel.setText("Cliquez sur la carte pour poser le point de repère.");
            } else {
                latRepere = null;
                lngRepere = null;
                carteView.effacerPositionUtilisateur();
                chargerAnnonces();
            }
        });

        barreOutils.getChildren().addAll(titre, espace, new Label("Centrer sur :"),
                btnFianar, btnTana, repere, btnRafraichir);

        infoLabel.getStyleClass().add("muted");

        getChildren().addAll(barreOutils, infoLabel, new Separator(), carteView);
    }

    private void initialiserCarte() {
        // Clic sur un marqueur de la carte -> ouverture du detail
        carteView.getBridge().setOnAnnonceClick(annonceId -> {
            app.ouvrirDetail(annonceId, () -> app.afficherSansHistorique(this));
        });

        // D9 : le pont onMapClick etait declare sans ecoutant ; il pose le repere.
        carteView.getBridge().setOnMapClick((lat, lng) -> {
            if (!repere.isSelected()) {
                return;
            }
            latRepere = lat;
            lngRepere = lng;
            carteView.definirPositionUtilisateur(lat, lng, "Point de repère");
            chargerAnnonces();
        });

        carteView.getBridge().setOnMapReady(() -> {
            if (chargementLance) {
                return; // double notification (sondage JS + bridge Java) : on ignore la seconde
            }
            chargementLance = true;
            chargerAnnonces();
        });
    }

    private void chargerAnnonces() {
        infoLabel.setText(latRepere == null
                ? "Chargement des annonces positionnées..."
                : "Chargement des annonces par distance du point de repère...");
        Map<String, String> filtres = new LinkedHashMap<>();
        if (latRepere != null && lngRepere != null) {
            filtres.put("lat", String.valueOf(latRepere));
            filtres.put("lng", String.valueOf(lngRepere));
        }
        chargerPage(0, new ArrayList<>(), filtres, ++generation);
    }

    /** Parcourt les pages une par une : l'API plafonne a TAILLE_PAGE par appel. */
    private void chargerPage(int numero, List<AnnonceResumeResponse> chargees,
                             Map<String, String> filtres, int gen) {
        app.appeler(() -> api.rechercher(filtres, numero, TAILLE_PAGE), reponse -> {
            if (gen != generation) {
                return; // chargement perime (nouveau clic ou Actualiser plus recent)
            }
            chargees.addAll(reponse.contenu());
            int suivante = numero + 1;
            if (suivante < reponse.totalPages() && suivante < PAGES_MAX) {
                chargerPage(suivante, chargees, filtres, gen);
            } else {
                afficherSurCarte(chargees, reponse.totalElements());
            }
        }, err -> {
            if (gen != generation) {
                return;
            }
            // Laisser un retour visible sur la carte plutot qu'un libelle fige.
            infoLabel.setText("Impossible de charger les annonces sur la carte : "
                    + (err.getMessage() == null ? "erreur inattendue" : err.getMessage()));
            chargementLance = false;
        });
    }

    /** Affiche les marqueurs et dit ce qui est affiche, ce qui ne l'est pas,
     *  et si le chargement a du etre tronque (D4). */
    private void afficherSurCarte(List<AnnonceResumeResponse> chargees, long total) {
        carteView.afficherAnnonces(chargees);
        long positionnees = chargees.stream()
                .filter(a -> a.latitude() != null && a.longitude() != null)
                .count();

        StringBuilder message = new StringBuilder();
        if (latRepere != null) {
            message.append("Tri par distance du point de repère — ");
        }
        message.append(positionnees).append(" annonce(s) positionnée(s) sur la carte");
        if (positionnees < chargees.size()) {
            message.append(" (").append(chargees.size() - positionnees)
                    .append(" annonce(s) sans coordonnées : non affichables)");
        }
        if (chargees.size() < total) {
            message.append(" — chargement limité à ").append(chargees.size())
                    .append(" sur ").append(total).append(" annonces");
        }
        message.append(". Cliquez sur un marqueur pour voir les détails.");
        infoLabel.setText(message.toString());
        chargementLance = false; // "Actualiser" reste possible
    }
}
