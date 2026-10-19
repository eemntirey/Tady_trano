package mg.emit.tadytrano.client.vue;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import mg.emit.tadytrano.client.TadyTranoApp;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.dto.AnnonceResumeResponse;
import mg.emit.tadytrano.client.dto.QuartierResponse;
import mg.emit.tadytrano.client.dto.TypeLocationResponse;
import mg.emit.tadytrano.client.dto.VilleResponse;
import mg.emit.tadytrano.client.ui.CurseurPlage;
import mg.emit.tadytrano.client.util.Affichage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Recherche publique d'annonces : filtres Ville/Quartier, Type, prix et
 *  pastilles de filtres rapides (CDC 8.3 / maquette Disign4e). */
public class VueRecherche extends VBox {

    private static final int TAILLE = 12;
    private static final double PRIX_MIN = 0;
    private static final double PRIX_MAX = 10_000_000;
    private static final double PAS_PRIX = 10_000;
    private static final double PRIX_BAS_DEFAUT = 0;
    private static final double PRIX_HAUT_DEFAUT = 10_000_000;
    /** Nombre maximal de pastilles de quartiers proposees en raccourci. */
    private static final int PASTILLES_QUARTIER = 10;

    private final TadyTranoApp app;
    private final ApiClient api;

    private final ComboBox<String> localisation = new ComboBox<>();
    private final ComboBox<TypeLocationResponse> type = new ComboBox<>();
    private final ComboBox<Integer> nbPiecesMin = new ComboBox<>();
    private final ComboBox<Integer> nbPiecesMax = new ComboBox<>();
    private final CurseurPlage prix = new CurseurPlage(PRIX_MIN, PRIX_MAX, PRIX_BAS_DEFAUT, PRIX_HAUT_DEFAUT, PAS_PRIX);
    private final Label prixBas = new Label();
    private final Label prixHaut = new Label();
    private final FlowPane pastilles = new FlowPane(8, 8);

    private final Map<String, VilleResponse> villesParNom = new LinkedHashMap<>();
    private final Map<String, QuartierResponse> quartiersParNom = new LinkedHashMap<>();
    private final List<ToggleButton> pastillesType = new ArrayList<>();
    private final List<ToggleButton> pastillesQuartier = new ArrayList<>();

    private final FlowPane liste = new FlowPane();
    private final Label pageLabel = new Label();
    private final Button precedent = new Button("\u2039");
    private final Button suivant = new Button("\u203A");

    private int page = 0;
    private int totalPages = 0;
    /** Numero de sequence des recherches : permet d'ignorer les reponses perimees
     *  (filtre ou page change depuis le lancement de la requete). */
    private int generation = 0;

    public VueRecherche(TadyTranoApp app, ApiClient api) {
        this.app = app;
        this.api = api;
        construire();
        chargerReferentiel();
        rechercher();
    }

    private void construire() {
        setSpacing(12);
        setPadding(new Insets(15));

        Label titre = new Label("Rechercher un logement");
        titre.getStyleClass().add("page-title");

        // --- barre de filtres (maquette : champ, type, curseur de prix) ---
        VBox filtres = new VBox(12);
        filtres.setPadding(new Insets(14, 16, 14, 16));
        filtres.getStyleClass().add("filter-bar");

        localisation.setPromptText("Ville/Quartier");
        localisation.getStyleClass().add("filter-field");
        localisation.setCellFactory(lv -> new ListCell<>() {
            private final Label maison = icone("\uE80F", "\u2302");
            private final Label texte = new Label();
            private final HBox ligne = new HBox(8, maison, texte);

            {
                ligne.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    texte.setText(item);
                    setGraphic(ligne);
                }
            }
        });
        HBox.setHgrow(localisation, Priority.ALWAYS);

        Label epingle = icone("\uE707", "\u25CE");
        HBox champLocalisation = new HBox(6, epingle, localisation);
        champLocalisation.getStyleClass().add("champ-icone");
        champLocalisation.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(champLocalisation, Priority.ALWAYS);

        type.setPromptText("Type");
        type.setPrefWidth(160);
        type.getStyleClass().add("filter-field");

        nbPiecesMin.getStyleClass().add("filter-field");
        nbPiecesMin.setPromptText("Pièces min");
        nbPiecesMin.setPrefWidth(100);
        nbPiecesMin.getItems().add(null);
        nbPiecesMin.getItems().addAll(1, 2, 3, 4, 5, 6);
        nbPiecesMin.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Integer v) {
                return v == null ? "" : String.valueOf(v);
            }

            @Override
            public Integer fromString(String s) {
                return (s == null || s.isBlank()) ? null : Integer.valueOf(s.trim());
            }
        });
        nbPiecesMax.getStyleClass().add("filter-field");
        nbPiecesMax.setPromptText("Pièces max");
        nbPiecesMax.setPrefWidth(100);
        nbPiecesMax.getItems().add(null);
        nbPiecesMax.getItems().addAll(1, 2, 3, 4, 5, 6);
        nbPiecesMax.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Integer v) {
                return v == null ? "" : String.valueOf(v);
            }

            @Override
            public Integer fromString(String s) {
                return (s == null || s.isBlank()) ? null : Integer.valueOf(s.trim());
            }
        });

        prixBas.getStyleClass().add("range-value");
        prixHaut.getStyleClass().add("range-value");
        prix.setMaxWidth(Double.MAX_VALUE);
        HBox etiquettes = new HBox(prixBas, new Region(), prixHaut);
        HBox.setHgrow(etiquettes.getChildren().get(1), Priority.ALWAYS);
        VBox plage = new VBox(6, prix, etiquettes);
        HBox.setHgrow(plage, Priority.ALWAYS);
        plage.setMinWidth(260);

        HBox ligneFiltres = new HBox(12, champLocalisation, type, nbPiecesMin, nbPiecesMax, plage);
        ligneFiltres.setAlignment(Pos.CENTER_LEFT);

        // --- pastilles de filtres rapides + actions ---
        pastilles.getStyleClass().add("chips-row");
        Button lancer = new Button("Rechercher");
        lancer.setDefaultButton(true);
        lancer.getStyleClass().add("btn-primary-custom");
        lancer.setOnAction(e -> {
            page = 0;
            rechercher();
        });
        Button reinitialiser = new Button("Reinitialiser");
        reinitialiser.getStyleClass().add("btn-ghost");
        reinitialiser.setOnAction(e -> reinitialiser());
        HBox actions = new HBox(10, reinitialiser, lancer);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox lignePastilles = new HBox(12, pastilles, actions);
        HBox.setHgrow(pastilles, Priority.ALWAYS);
        lignePastilles.setAlignment(Pos.CENTER_LEFT);

        filtres.getChildren().addAll(ligneFiltres, lignePastilles);

        prix.basProperty().addListener((o, a, b) -> majEtiquettesPrix());
        prix.hautProperty().addListener((o, a, b) -> majEtiquettesPrix());
        majEtiquettesPrix();

        localisation.setOnAction(e -> {
            page = 0;
            deselecterSauf(pastillesQuartier, null);
            rechercher();
        });
        type.setOnAction(e -> {
            page = 0;
            deselecterSauf(pastillesType, null);
            rechercher();
        });
        nbPiecesMin.setOnAction(e -> {
            page = 0;
            rechercher();
        });
        nbPiecesMax.setOnAction(e -> {
            page = 0;
            rechercher();
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
        pageLabel.getStyleClass().add("pagination-label");
        precedent.getStyleClass().add("pagination-btn");
        suivant.getStyleClass().add("pagination-btn");
        HBox pagination = new HBox(15, precedent, pageLabel, suivant);
        pagination.setAlignment(Pos.CENTER);
        pagination.getStyleClass().add("pagination-bar");

        getChildren().addAll(titre, new Separator(), filtres, new Separator(), conteneurListe, pagination);
        VBox.setVgrow(conteneurListe, Priority.ALWAYS);
    }

    private void majEtiquettesPrix() {
        prixBas.setText(arriary(prix.bas()));
        prixHaut.setText(arriary(prix.haut()));
    }

    private static String arriary(double valeur) {
        return "Ar " + String.format("%,.0f", valeur).replace(',', ' ');
    }

    /** Icone monochrome (Segoe MDL2 Assets sous Windows) avec repli texte. */
    private static Label icone(String glyphe, String repli) {
        boolean disponible = Font.getFamilies().contains("Segoe MDL2 Assets");
        Label etiquette = new Label(disponible ? glyphe : repli);
        etiquette.getStyleClass().add("champ-icone-glyphe");
        if (disponible) {
            etiquette.setStyle("-fx-font-family: 'Segoe MDL2 Assets'; -fx-font-size: 15px;");
        }
        return etiquette;
    }

    // ------------------------------------------------------------------
    // Referentiel (villes, quartiers, types) : champ, listes et pastilles
    // ------------------------------------------------------------------

    private record Referentiel(List<VilleResponse> villes,
                               List<TypeLocationResponse> types,
                               List<QuartierResponse> quartiers) {
    }

    private void chargerReferentiel() {
        app.appeler(() -> {
            List<VilleResponse> villes = api.villes();
            List<TypeLocationResponse> types = api.typesLocation();
            List<QuartierResponse> quartiers = new ArrayList<>();
            for (VilleResponse ville : villes) {
                quartiers.addAll(api.quartiers(ville.id()));
            }
            return new Referentiel(villes, types, quartiers);
        }, this::appliquerReferentiel, app::gererErreur);
    }

    private void appliquerReferentiel(Referentiel referentiel) {
        villesParNom.clear();
        quartiersParNom.clear();
        Set<String> noms = new LinkedHashSet<>();
        for (VilleResponse ville : referentiel.villes()) {
            villesParNom.putIfAbsent(cle(ville.nom()), ville);
            noms.add(ville.nom());
        }
        for (QuartierResponse quartier : referentiel.quartiers()) {
            quartiersParNom.putIfAbsent(cle(quartier.nom()), quartier);
            noms.add(quartier.nom());
        }
        localisation.getItems().setAll(noms);

        type.getItems().setAll(referentiel.types());
        type.getItems().add(0, null);

        construirePastilles(referentiel.types(), referentiel.quartiers());
    }

    private void construirePastilles(List<TypeLocationResponse> types,
                                     List<QuartierResponse> quartiers) {
        pastilles.getChildren().clear();
        pastillesType.clear();
        pastillesQuartier.clear();

        for (TypeLocationResponse t : types) {
            ToggleButton pastille = new ToggleButton(t.libelle());
            pastille.getStyleClass().add("chip");
            pastille.setOnAction(e -> {
                if (pastille.isSelected()) {
                    type.setValue(t);
                    deselecterSauf(pastillesType, pastille);
                } else if (type.getValue() == t) {
                    type.setValue(null);
                }
                page = 0;
                rechercher();
            });
            pastillesType.add(pastille);
            pastilles.getChildren().add(pastille);
        }

        int reste = PASTILLES_QUARTIER;
        for (QuartierResponse q : quartiers) {
            if (reste-- <= 0) {
                break;
            }
            ToggleButton pastille = new ToggleButton(q.nom());
            pastille.getStyleClass().add("chip");
            pastille.setOnAction(e -> {
                if (pastille.isSelected()) {
                    localisation.setValue(q.nom());
                    deselecterSauf(pastillesQuartier, pastille);
                } else if (q.nom().equals(localisation.getValue())) {
                    localisation.setValue(null);
                }
                page = 0;
                rechercher();
            });
            pastillesQuartier.add(pastille);
            pastilles.getChildren().add(pastille);
        }
    }

    private static void deselecterSauf(List<ToggleButton> boutons, ToggleButton garde) {
        for (ToggleButton bouton : boutons) {
            if (bouton != garde) {
                bouton.setSelected(false);
            }
        }
    }

    private void reinitialiser() {
        localisation.setValue(null);
        type.setValue(null);
        nbPiecesMin.setValue(null);
        nbPiecesMax.setValue(null);
        prix.reinitialiser(PRIX_BAS_DEFAUT, PRIX_HAUT_DEFAUT);
        deselecterSauf(pastillesType, null);
        deselecterSauf(pastillesQuartier, null);
        page = 0;
        rechercher();
    }

    // ------------------------------------------------------------------
    // Recherche
    // ------------------------------------------------------------------

    private void rechercher() {
        Map<String, String> filtres = new LinkedHashMap<>();

        String lieu = localisation.getValue() == null ? "" : localisation.getValue().trim();
        if (!lieu.isEmpty()) {
            VilleResponse ville = villesParNom.get(cle(lieu));
            QuartierResponse quartier = ville == null ? quartiersParNom.get(cle(lieu)) : null;
            if (ville != null) {
                filtres.put("villeId", String.valueOf(ville.id()));
            } else if (quartier != null) {
                filtres.put("quartierId", String.valueOf(quartier.id()));
                if (quartier.latitude() != null && quartier.longitude() != null) {
                    filtres.put("lat", String.valueOf(quartier.latitude()));
                    filtres.put("lng", String.valueOf(quartier.longitude()));
                }
            }
        }
        if (nbPiecesMin.getValue() != null) {
            filtres.put("nbPiecesMin", String.valueOf(nbPiecesMin.getValue()));
        }
        if (nbPiecesMax.getValue() != null) {
            filtres.put("nbPiecesMax", String.valueOf(nbPiecesMax.getValue()));
        }
        if (type.getValue() != null) {
            filtres.put("typeLocationId", String.valueOf(type.getValue().id()));
        }
        if (prix.bas() > PRIX_MIN) {
            filtres.put("prixMin", String.valueOf(Math.round(prix.bas())));
        }
        if (prix.haut() < PRIX_MAX) {
            filtres.put("prixMax", String.valueOf(Math.round(prix.haut())));
        }

        Map<String, String> finaux = filtres;
        final int pageDemandee = page;
        final int gen = ++generation;
        app.appeler(() -> api.rechercher(finaux, pageDemandee, TAILLE),
                reponse -> {
                    if (gen != generation) {
                        return; // reponse perimee : une recherche plus recente a ete lancee
                    }
                    totalPages = reponse.totalPages();
                    pageLabel.setText(reponse.totalPages() == 0
                            ? "Aucun resultat"
                            : "Page " + (reponse.page() + 1) + " / " + reponse.totalPages());
                    precedent.setDisable(reponse.page() <= 0);
                    suivant.setDisable(reponse.page() >= reponse.totalPages() - 1);
                    afficher(reponse.contenu());
                },
                erreur -> {
                    if (gen != generation) {
                        return; // erreur d'une recherche perimee : ignoree egalement
                    }
                    app.gererErreur(erreur);
                });
    }

    private static String cle(String texte) {
        return texte == null ? "" : texte.trim().toLowerCase(Locale.ROOT);
    }

    private void afficher(List<AnnonceResumeResponse> annonces) {
        liste.getChildren().clear();
        for (AnnonceResumeResponse annonce : annonces) {
            liste.getChildren().add(carte(annonce));
        }
    }

    /** Carte annonce cliquable (vignette pleine largeur + titre + prix + lieu). */
    private VBox carte(AnnonceResumeResponse annonce) {
        VBox carte = new VBox();
        carte.setPrefWidth(190);
        carte.setMaxWidth(190);
        carte.getStyleClass().add("annonce-card");

        StackPane media = new StackPane(Affichage.vignette(annonce.urlPhotoPrincipale(), 190, 112));
        media.getStyleClass().add("card-media");
        media.setPrefSize(190, 112);
        media.setMaxSize(190, 112);

        VBox corps = new VBox(4);
        corps.getStyleClass().add("card-body");

        Label titre = new Label(annonce.titre());
        titre.getStyleClass().add("card-title");
        titre.setWrapText(true);
        titre.setMaxHeight(36);

        Label prixLabel = new Label(Affichage.prix(annonce.prix()));
        prixLabel.getStyleClass().add("card-price");

        String lieu = annonce.quartier() == null ? "" : annonce.quartier().toString();
        Label quartierLabel = new Label(lieu);
        quartierLabel.getStyleClass().add("card-location");

        Label details = new Label(annonce.nbPieces() + " pieces"
                + (annonce.surface() != null ? " - " + annonce.surface() + " m2" : ""));
        details.getStyleClass().add("card-specs");

        corps.getChildren().addAll(titre, prixLabel, quartierLabel, details);

        HBox badges = new HBox(6);
        if (annonce.typeLocation() != null) {
            Label typeBadge = new Label(annonce.typeLocation().libelle());
            typeBadge.getStyleClass().add("badge-info");
            badges.getChildren().add(typeBadge);
        }
        if (annonce.distanceKm() != null) {
            Label distance = new Label(Affichage.distance(annonce.distanceKm()) + " de vous");
            distance.getStyleClass().add("badge-distance");
            badges.getChildren().add(distance);
        }
        if (!badges.getChildren().isEmpty()) {
            corps.getChildren().add(badges);
        }

        carte.getChildren().addAll(media, corps);
        carte.setOnMouseClicked(e -> app.ouvrirDetail(annonce.id(), this::rechercher));
        return carte;
    }
}
