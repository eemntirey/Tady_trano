package mg.emit.tadytrano.client.vue;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mg.emit.tadytrano.client.TadyTranoApp;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.dto.AnnonceResumeResponse;
import mg.emit.tadytrano.client.dto.QuartierResponse;
import mg.emit.tadytrano.client.dto.TypeLocationResponse;
import mg.emit.tadytrano.client.dto.VilleResponse;
import mg.emit.tadytrano.client.util.Affichage;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Recherche publique d'annonces avec filtres et pagination. */
public class VueRecherche extends VBox {

    private static final int TAILLE = 12;

    private final TadyTranoApp app;
    private final ApiClient api;

    private final ComboBox<VilleResponse> ville = new ComboBox<>();
    private final ComboBox<QuartierResponse> quartier = new ComboBox<>();
    private final ComboBox<TypeLocationResponse> type = new ComboBox<>();
    private final Spinner<Integer> prixMin = entier(0, 0, 10_000_000);
    private final Spinner<Integer> prixMax = entier(0, 0, 10_000_000);
    private final Spinner<Integer> piecesMin = entier(1, 1, 20);
    private final Spinner<Integer> piecesMax = entier(1, 1, 20);

    private final FlowPane liste = new FlowPane();
    private final Label pageLabel = new Label();
    private final Button precedent = new Button("<");
    private final Button suivant = new Button(">");

    private int page = 0;
    private int totalPages = 0;

    public VueRecherche(TadyTranoApp app, ApiClient api) {
        this.app = app;
        this.api = api;
        construire();
        chargerReferentiel();
        rechercher();
    }

    private void construire() {
        setSpacing(10);
        setPadding(new Insets(15));

        Label titre = new Label("Rechercher un logement");
        titre.setFont(Font.font("System", FontWeight.BOLD, 20));

        // --- filtres ---
        GridPane filtres = new GridPane();
        filtres.setHgap(10);
        filtres.setVgap(8);
        filtres.setPadding(new Insets(10));
        filtres.setStyle("-fx-background-color: #f4f6f7; -fx-background-radius: 6;");

        ville.setPromptText("Ville");
        ville.setPrefWidth(160);
        quartier.setPromptText("Quartier");
        quartier.setPrefWidth(180);
        type.setPromptText("Type");
        type.setPrefWidth(130);

        filtres.add(new Label("Ville"), 0, 0);
        filtres.add(ville, 1, 0);
        filtres.add(new Label("Quartier"), 2, 0);
        filtres.add(quartier, 3, 0);
        filtres.add(new Label("Type"), 4, 0);
        filtres.add(type, 5, 0);

        filtres.add(new Label("Prix min (Ar)"), 0, 1);
        filtres.add(prixMin, 1, 1);
        filtres.add(new Label("Prix max (Ar)"), 2, 1);
        filtres.add(prixMax, 3, 1);
        filtres.add(new Label("Pieces"), 4, 1);
        HBox pieces = new HBox(8, piecesMin, new Label("a"), piecesMax);
        filtres.add(pieces, 5, 1);

        Button lancer = new Button("Rechercher");
        lancer.setDefaultButton(true);
        lancer.setOnAction(e -> {
            page = 0;
            rechercher();
        });
        Button reinitialiser = new Button("Reinitialiser");
        reinitialiser.setOnAction(e -> {
            ville.getSelectionModel().clearSelection();
            type.getSelectionModel().clearSelection();
            quartier.getItems().clear();
            prixMin.getValueFactory().setValue(0);
            prixMax.getValueFactory().setValue(0);
            piecesMin.getValueFactory().setValue(1);
            piecesMax.getValueFactory().setValue(1);
            page = 0;
            rechercher();
        });
        HBox boutons = new HBox(10, lancer, reinitialiser);
        boutons.setAlignment(Pos.CENTER_RIGHT);
        filtres.add(boutons, 6, 1);

        ville.setOnAction(e -> chargerQuartiers());
        prixMin.valueProperty().addListener((o, a, b) -> {
            if (b != null && prixMax.getValue() < b) {
                prixMax.getValueFactory().setValue(b);
            }
        });
        piecesMin.valueProperty().addListener((o, a, b) -> {
            if (b != null && piecesMax.getValue() < b) {
                piecesMax.getValueFactory().setValue(b);
            }
        });

        // --- resultats ---
        liste.setHgap(15);
        liste.setVgap(15);
        liste.setPrefWidth(Region.USE_COMPUTED_SIZE);
        VBox conteneurListe = new VBox(liste);
        conteneurListe.setPadding(new Insets(5, 0, 0, 0));

        precedent.setOnAction(e -> {
            if (page > 0) {
                page--;
                rechercher();
            }
        });
        suivant.setOnAction(e -> {
            if (page < totalPages - 1) {
                page++;
                rechercher();
            }
        });
        pageLabel.setAlignment(Pos.CENTER);
        HBox pagination = new HBox(15, precedent, pageLabel, suivant);
        pagination.setAlignment(Pos.CENTER);

        getChildren().addAll(titre, new Separator(), filtres, conteneurListe, pagination);
        VBox.setVgrow(conteneurListe, Priority.ALWAYS);
    }

    private void chargerReferentiel() {
        app.appeler(api::villes, villes -> {
            ville.getItems().setAll(villes);
            ville.getItems().add(0, null);
            type.getItems().setAll(api.typesLocation());
            type.getItems().add(0, null);
        });
    }

    private void chargerQuartiers() {
        VilleResponse selection = ville.getValue();
        quartier.getItems().clear();
        if (selection == null) {
            return;
        }
        app.appeler(() -> api.quartiers(selection.id()), quartiers -> {
            quartier.getItems().setAll(quartiers);
            quartier.getItems().add(0, null);
        });
    }

    private void rechercher() {
        Map<String, String> filtres = new LinkedHashMap<>();
        if (ville.getValue() != null) {
            filtres.put("villeId", String.valueOf(ville.getValue().id()));
        }
        if (quartier.getValue() != null) {
            filtres.put("quartierId", String.valueOf(quartier.getValue().id()));
        }
        if (type.getValue() != null) {
            filtres.put("typeLocationId", String.valueOf(type.getValue().id()));
        }
        if (prixMin.getValue() > 0) {
            filtres.put("prixMin", String.valueOf(prixMin.getValue()));
        }
        if (prixMax.getValue() > 0) {
            filtres.put("prixMax", String.valueOf(prixMax.getValue()));
        }
        if (piecesMin.getValue() > 1) {
            filtres.put("nbPiecesMin", String.valueOf(piecesMin.getValue()));
        }
        if (piecesMax.getValue() > piecesMin.getValue()) {
            filtres.put("nbPiecesMax", String.valueOf(piecesMax.getValue()));
        }

        Map<String, String> finaux = filtres;
        app.appeler(() -> api.rechercher(finaux, page, TAILLE), reponse -> {
            totalPages = reponse.totalPages();
            pageLabel.setText(reponse.totalPages() == 0
                    ? "Aucun resultat"
                    : "Page " + (reponse.page() + 1) + " / " + reponse.totalPages());
            precedent.setDisable(reponse.page() <= 0);
            suivant.setDisable(reponse.page() >= reponse.totalPages() - 1);
            afficher(reponse.contenu());
        });
    }

    private void afficher(List<AnnonceResumeResponse> annonces) {
        liste.getChildren().clear();
        for (AnnonceResumeResponse annonce : annonces) {
            liste.getChildren().add(carte(annonce));
        }
    }

    /** Carte annonce cliquable (vignette + titre + prix + quartier). */
    private VBox carte(AnnonceResumeResponse annonce) {
        VBox carte = new VBox(4);
        carte.setPadding(new Insets(8));
        carte.setPrefWidth(240);
        carte.setStyle("-fx-background-color: white; -fx-background-radius: 6;"
                + " -fx-border-color: #ddd; -fx-border-radius: 6; -fx-cursor: hand;");

        carte.getChildren().add(Affichage.vignette(annonce.urlPhotoPrincipale(), 224, 130));

        Label titre = new Label(annonce.titre());
        titre.setFont(Font.font("System", FontWeight.BOLD, 14));
        titre.setWrapText(true);
        titre.setMaxHeight(36);

        Label prix = new Label(Affichage.prix(annonce.prix()));
        prix.setStyle("-fx-text-fill: #1a5276; -fx-font-weight: bold; -fx-font-size: 14;");

        String lieu = annonce.quartier() == null ? "" : annonce.quartier().toString();
        Label quartierLabel = new Label(lieu);
        quartierLabel.setStyle("-fx-text-fill: #777;");

        Label details = new Label(annonce.nbPieces() + " pieces"
                + (annonce.surface() != null ? " - " + annonce.surface() + " m2" : ""));
        details.setStyle("-fx-text-fill: #777;");

        carte.getChildren().addAll(titre, prix, quartierLabel, details);

        if (annonce.distanceKm() != null) {
            Label distance = new Label(Affichage.distance(annonce.distanceKm()) + " de vous");
            distance.setStyle("-fx-text-fill: #27ae60;");
            carte.getChildren().add(distance);
        }

        carte.setOnMouseClicked(e -> app.ouvrirDetail(annonce.id(), this::rechercher));
        return carte;
    }

    private static Spinner<Integer> entier(int valeur, int min, int max) {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, valeur));
        spinner.setEditable(true);
        spinner.setPrefWidth(100);
        return spinner;
    }
}
