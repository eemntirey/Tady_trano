package mg.emit.tadytrano.client.vue;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
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

    private static final int TAILLE = 20;
    private static final double PRIX_MIN = 0;
    private static final double PRIX_MAX = 10_000_000;
    private static final double PAS_PRIX = 10_000;
    // Aucun filtre de prix par défaut : ne pas masquer les petites annonces.
    private static final double PRIX_BAS_DEFAUT = PRIX_MIN;
    private static final double PRIX_HAUT_DEFAUT = PRIX_MAX;
    /** Nombre maximal de pastilles de quartiers proposees en raccourci. */
    private static final int PASTILLES_QUARTIER = 10;

    private final TadyTranoApp app;
    private final ApiClient api;

    private final ComboBox<Object> localisation = new ComboBox<>();
    private final ComboBox<TypeLocationResponse> type = new ComboBox<>();
    private final ComboBox<Integer> piecesMin = new ComboBox<>();
    private final ComboBox<Integer> piecesMax = new ComboBox<>();
    private final Button autourQuartier = new Button("Autour du quartier");
    private final Label positionStatut = new Label("Choisissez un quartier pour trier par distance.");
    private boolean triParDistance = false;
    private final CurseurPlage prix = new CurseurPlage(PRIX_MIN, PRIX_MAX, PRIX_BAS_DEFAUT, PRIX_HAUT_DEFAUT, PAS_PRIX);
    private final Label prixBas = new Label();
    private final Label prixHaut = new Label();
    private final FlowPane pastilles = new FlowPane(8, 8);

    private final Map<String, VilleResponse> villesParNom = new LinkedHashMap<>();
    private final Map<String, QuartierResponse> quartiersParNom = new LinkedHashMap<>();
    private final List<ToggleButton> pastillesType = new ArrayList<>();
    private final List<ToggleButton> pastillesQuartier = new ArrayList<>();

    private final FlowPane liste = new FlowPane();
    private final ScrollPane defilement = new ScrollPane();
    private final Label etat = new Label();
    private final Button reessayer = new Button("Reessayer");

    /** Prochaine page à charger (défilement continu). */
    private int page = 0;
    private boolean chargementEnCours = false;
    private boolean toutCharge = false;
    /** Bloque le chargement automatique après un échec, jusqu'à « Reessayer ». */
    private boolean erreurSuite = false;
    /** Filtres figés pour toutes les pages d'une même recherche. */
    private Map<String, String> filtresActifs = new LinkedHashMap<>();
    /** Numero de sequence des recherches : permet d'ignorer les reponses perimees
     *  (filtre ou page change depuis le lancement de la requete). */
    private int generation = 0;
    /** Evite les actions reentrantes lorsque les pastilles pilotent les ComboBox. */
    private boolean miseAJourFiltresEnCours = false;

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
        localisation.setCellFactory(lv -> new ListCell<Object>() {
            private final Label maison = icone("\uE80F", "\u2302");
            private final Label texte = new Label();
            private final HBox ligne = new HBox(8, maison, texte);

            {
                ligne.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    texte.setText(item.toString());
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

        prixBas.getStyleClass().add("range-value");
        prixHaut.getStyleClass().add("range-value");
        prix.setMaxWidth(Double.MAX_VALUE);
        HBox etiquettes = new HBox(prixBas, new Region(), prixHaut);
        HBox.setHgrow(etiquettes.getChildren().get(1), Priority.ALWAYS);
        VBox plage = new VBox(6, prix, etiquettes);
        HBox.setHgrow(plage, Priority.ALWAYS);
        plage.setMinWidth(260);

        HBox ligneFiltres = new HBox(12, champLocalisation, type, plage);
        ligneFiltres.setAlignment(Pos.CENTER_LEFT);

        // Filtres fonctionnels de nombre de pièces et position de référence.
        piecesMin.setPromptText("Pièces min.");
        piecesMax.setPromptText("Pièces max.");
        piecesMin.getItems().setAll(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        piecesMax.getItems().setAll(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        piecesMin.setPrefWidth(110);
        piecesMax.setPrefWidth(110);
        piecesMin.getStyleClass().add("filter-field");
        piecesMax.getStyleClass().add("filter-field");
        autourQuartier.setDisable(true);
        autourQuartier.getStyleClass().add("btn-ghost");
        positionStatut.getStyleClass().add("muted");
        HBox ligneAvancee = new HBox(10, new Label("Nombre de pièces :"),
                piecesMin, piecesMax, autourQuartier, positionStatut);
        ligneAvancee.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(positionStatut, Priority.ALWAYS);

        // --- pastilles de filtres rapides + actions ---
        pastilles.getStyleClass().add("chips-row");
        Button lancer = new Button("Rechercher");
        lancer.setDefaultButton(true);
        lancer.getStyleClass().add("btn-primary-custom");
        lancer.setOnAction(e -> {
            page = 0;
            rechercher();
        });
        Button reinitialiser = new Button("Réinitialiser");
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
            if (miseAJourFiltresEnCours) {
                return;
            }
            triParDistance = false;
            actualiserCommandeDistance();
            page = 0;
            deselecterSauf(pastillesQuartier, null);
            rechercher();
        });
        type.setOnAction(e -> {
            if (miseAJourFiltresEnCours) {
                return;
            }
            page = 0;
            deselecterSauf(pastillesType, null);
            rechercher();
        });

        piecesMin.setOnAction(e -> {
            harmoniserPieces();
            page = 0;
            rechercher();
        });
        piecesMax.setOnAction(e -> {
            harmoniserPieces();
            page = 0;
            rechercher();
        });
        autourQuartier.setOnAction(e -> {
            QuartierResponse quartier = quartierSelectionne();
            if (quartier == null || !coordonneesValides(quartier)) {
                triParDistance = false;
                positionStatut.setText("Ce quartier n'a pas de coordonnées.");
            } else {
                triParDistance = !triParDistance;
                positionStatut.setText(triParDistance
                        ? "Résultats triés par distance de " + quartier.nom()
                        : "Tri par date de publication.");
            }
            actualiserCommandeDistance();
            page = 0;
            rechercher();
        });

        // --- résultats : défilement continu ---
        liste.setHgap(15);
        liste.setVgap(15);
        liste.setPrefWidth(Region.USE_COMPUTED_SIZE);

        etat.getStyleClass().add("muted");
        reessayer.getStyleClass().add("btn-ghost");
        reessayer.setVisible(false);
        reessayer.setManaged(false);
        reessayer.setOnAction(e -> {
            reessayer.setVisible(false);
            reessayer.setManaged(false);
            erreurSuite = false;
            chargerSuite();
        });
        HBox pied = new HBox(12, etat, reessayer);
        pied.setAlignment(Pos.CENTER);
        pied.setPadding(new Insets(10, 0, 16, 0));

        VBox contenu = new VBox(10, liste, pied);
        contenu.setPadding(new Insets(5, 4, 0, 0));
        defilement.setContent(contenu);
        defilement.setFitToWidth(true);
        defilement.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        defilement.getStyleClass().add("scroll-transparent");
        // Charge la page suivante lorsque l'utilisateur approche du bas.
        defilement.vvalueProperty().addListener((o, avant, apres) -> {
            if (apres.doubleValue() >= defilement.getVmax() - 0.15) {
                chargerSuite();
            }
        });
        // Si le contenu ne remplit pas la fenêtre, charger jusqu'à la remplir.
        defilement.viewportBoundsProperty().addListener((o, avant, apres) -> completerSiBesoin());

        getChildren().addAll(titre, new Separator(), filtres, ligneAvancee,
                new Separator(), defilement);
        VBox.setVgrow(defilement, Priority.ALWAYS);
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

    private static volatile Referentiel referentielCache;

    private record Referentiel(List<VilleResponse> villes,
                               List<TypeLocationResponse> types,
                               List<QuartierResponse> quartiers) {
        private Referentiel {
            villes = List.copyOf(villes);
            types = List.copyOf(types);
            quartiers = List.copyOf(quartiers);
        }
    }

    private void chargerReferentiel() {
        // Les villes, quartiers et types changent rarement : eviter de refaire
        // toutes les requetes REST a chaque retour sur l'ecran de recherche.
        Referentiel cache = referentielCache;
        if (cache != null) {
            appliquerReferentiel(cache);
            return;
        }

        app.appeler(() -> {
            List<VilleResponse> villes = api.villes();
            List<TypeLocationResponse> types = api.typesLocation();
            List<QuartierResponse> quartiers = new ArrayList<>();
            for (VilleResponse ville : villes) {
                quartiers.addAll(api.quartiers(ville.id()));
            }
            return new Referentiel(villes, types, quartiers);
        }, referentiel -> {
            referentielCache = referentiel;
            appliquerReferentiel(referentiel);
        }, app::gererErreur);
    }

    private void appliquerReferentiel(Referentiel referentiel) {
        villesParNom.clear();
        quartiersParNom.clear();
        List<Object> items = new ArrayList<>();
        for (VilleResponse ville : referentiel.villes()) {
            villesParNom.putIfAbsent(cle(ville.toString()), ville);
            items.add(ville);
        }
        for (QuartierResponse quartier : referentiel.quartiers()) {
            // Clé composée nom (ville.nom) pour distinguer les quartiers homonymes.
            quartiersParNom.putIfAbsent(cle(quartier.toString()), quartier);
            items.add(quartier);
        }
        localisation.getItems().setAll(items);

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
            ToggleButton pastille = new ToggleButton(Affichage.libelleType(t.libelle()));
            pastille.getStyleClass().add("chip");
            pastille.setOnAction(e -> {
                if (pastille.isSelected()) {
                    appliquerValeurFiltre(() -> type.setValue(t));
                    deselecterSauf(pastillesType, pastille);
                } else if (type.getValue() == t) {
                    appliquerValeurFiltre(() -> type.setValue(null));
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
            ToggleButton pastille = new ToggleButton(q.toString());
            pastille.getStyleClass().add("chip");
            pastille.setOnAction(e -> {
                if (pastille.isSelected()) {
                    appliquerValeurFiltre(() -> localisation.setValue(q));
                    deselecterSauf(pastillesQuartier, pastille);
                } else if (localisation.getValue() != null && localisation.getValue().equals(q)) {
                    appliquerValeurFiltre(() -> localisation.setValue(null));
                }
                actualiserCommandeDistance();
                page = 0;
                rechercher();
            });
            pastillesQuartier.add(pastille);
            pastilles.getChildren().add(pastille);
        }
    }

    /** Modifie un filtre depuis une pastille sans déclencher le gestionnaire ComboBox en doublon. */
    private void appliquerValeurFiltre(Runnable changement) {
        miseAJourFiltresEnCours = true;
        try {
            changement.run();
        } finally {
            miseAJourFiltresEnCours = false;
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
        appliquerValeurFiltre(() -> {
            localisation.setValue(null);
            type.setValue(null);
        });
        prix.reinitialiser(PRIX_BAS_DEFAUT, PRIX_HAUT_DEFAUT);
        piecesMin.setValue(null);
        piecesMax.setValue(null);
        triParDistance = false;
        actualiserCommandeDistance();
        positionStatut.setText("Choisissez un quartier pour trier par distance.");
        deselecterSauf(pastillesType, null);
        deselecterSauf(pastillesQuartier, null);
        page = 0;
        rechercher();
    }

    // ------------------------------------------------------------------
    // Recherche
    // ------------------------------------------------------------------

    /** Nouvelle recherche : réinitialise la liste et fige les filtres pour ses pages. */
    private void rechercher() {
        generation++;
        page = 0;
        toutCharge = false;
        chargementEnCours = false;
        erreurSuite = false;
        reessayer.setVisible(false);
        reessayer.setManaged(false);
        liste.getChildren().clear();
        etat.setText("");
        defilement.setVvalue(0);
        filtresActifs = filtresCourants();
        chargerSuite();
    }

    private Map<String, String> filtresCourants() {
        Map<String, String> filtres = new LinkedHashMap<>();

        Object valeur = localisation.getValue();
        if (valeur instanceof QuartierResponse quartier) {
            // On resout directement l'objet selectionne : pas d'ambiguite sur les
            // quartiers homonymes (meme nom, villes differentes).
            filtres.put("quartierId", String.valueOf(quartier.id()));
        } else if (valeur instanceof VilleResponse ville) {
            filtres.put("villeId", String.valueOf(ville.id()));
        }
        if (type.getValue() != null) {
            filtres.put("typeLocationId", String.valueOf(type.getValue().id()));
        }
        // La plage complète (0 à 10 000 000 Ar) ne limite pas les résultats.
        if (prix.bas() > PRIX_MIN) {
            filtres.put("prixMin", String.valueOf(Math.round(prix.bas())));
        }
        if (prix.haut() < PRIX_MAX) {
            filtres.put("prixMax", String.valueOf(Math.round(prix.haut())));
        }
        if (piecesMin.getValue() != null) {
            filtres.put("nbPiecesMin", String.valueOf(piecesMin.getValue()));
        }
        if (piecesMax.getValue() != null) {
            filtres.put("nbPiecesMax", String.valueOf(piecesMax.getValue()));
        }
        QuartierResponse quartierReference = quartierSelectionne();
        if (triParDistance && quartierReference != null && coordonneesValides(quartierReference)) {
            filtres.put("lat", String.valueOf(quartierReference.latitude()));
            filtres.put("lng", String.valueOf(quartierReference.longitude()));
        }
        return filtres;
    }

    /** Charge la page suivante et l'ajoute à la liste existante. */
    private void chargerSuite() {
        if (chargementEnCours || toutCharge || erreurSuite) {
            return;
        }
        chargementEnCours = true;
        etat.setText(liste.getChildren().isEmpty() ? "Chargement..." : "Chargement de la suite...");
        final Map<String, String> filtres = new LinkedHashMap<>(filtresActifs);
        final int pageDemandee = page;
        final int gen = generation;
        app.appeler(() -> api.rechercher(filtres, pageDemandee, TAILLE),
                reponse -> {
                    if (gen != generation) {
                        return; // une recherche plus récente a été lancée
                    }
                    for (AnnonceResumeResponse annonce : reponse.contenu()) {
                        liste.getChildren().add(carte(annonce));
                    }
                    page = pageDemandee + 1;
                    toutCharge = page >= reponse.totalPages();
                    chargementEnCours = false;
                    majEtat(reponse.totalElements());
                    Platform.runLater(this::completerSiBesoin);
                },
                erreur -> {
                    if (gen != generation) {
                        return; // ignore l'erreur d'une requête périmée
                    }
                    chargementEnCours = false;
                    erreurSuite = true;
                    etat.setText("Impossible de charger la suite.");
                    reessayer.setVisible(true);
                    reessayer.setManaged(true);
                    app.gererErreur(erreur);
                });
    }

    private void majEtat(long total) {
        if (liste.getChildren().isEmpty()) {
            etat.setText("Aucun résultat");
        } else if (toutCharge) {
            etat.setText(total + " annonce(s) - fin des résultats");
        } else {
            etat.setText("");
        }
    }

    /** Complète la liste si elle est trop courte pour créer une barre de défilement. */
    private void completerSiBesoin() {
        if (chargementEnCours || toutCharge || erreurSuite || defilement.getContent() == null) {
            return;
        }
        defilement.applyCss();
        defilement.layout();
        double hauteurContenu = defilement.getContent().getBoundsInLocal().getHeight();
        double hauteurVisible = defilement.getViewportBounds().getHeight();
        if (hauteurVisible > 0 && hauteurContenu <= hauteurVisible) {
            chargerSuite();
        }
    }

    private void harmoniserPieces() {
        Integer min = piecesMin.getValue();
        Integer max = piecesMax.getValue();
        if (min != null && max != null && min > max) {
            // Garder une plage cohérente en ajustant l'autre borne.
            if (piecesMin.isFocused()) {
                piecesMax.setValue(min);
            } else {
                piecesMin.setValue(max);
            }
        }
    }

    private QuartierResponse quartierSelectionne() {
        Object valeur = localisation.getValue();
        return valeur instanceof QuartierResponse q ? q : null;
    }

    private static boolean coordonneesValides(QuartierResponse quartier) {
        return quartier.latitude() != null && quartier.longitude() != null
                && Double.isFinite(quartier.latitude()) && Double.isFinite(quartier.longitude())
                && quartier.latitude() >= -90 && quartier.latitude() <= 90
                && quartier.longitude() >= -180 && quartier.longitude() <= 180;
    }

    private void actualiserCommandeDistance() {
        QuartierResponse quartier = quartierSelectionne();
        boolean disponible = quartier != null && coordonneesValides(quartier);
        autourQuartier.setDisable(!disponible);
        autourQuartier.setText(triParDistance ? "Tri par distance : actif" : "Autour du quartier");
        if (!disponible) {
            triParDistance = false;
            if (positionStatut != null) {
                positionStatut.setText("Choisissez un quartier avec coordonnées.");
            }
        }
    }

    private static String cle(String texte) {
        return texte == null ? "" : texte.trim().toLowerCase(Locale.ROOT);
    }

    /** Carte annonce cliquable (vignette pleine largeur + titre + prix + lieu). */
    private VBox carte(AnnonceResumeResponse annonce) {
        VBox carte = new VBox();
        carte.setPrefWidth(230);
        carte.setMaxWidth(230);
        carte.getStyleClass().add("annonce-card");

        StackPane media = new StackPane(Affichage.vignette(annonce.urlPhotoPrincipale(), 230, 136));
        media.getStyleClass().add("card-media");
        media.setPrefSize(230, 136);
        media.setMaxSize(230, 136);
        Rectangle masqueArrondi = new Rectangle(230, 136);
        masqueArrondi.setArcWidth(18);
        masqueArrondi.setArcHeight(18);
        media.setClip(masqueArrondi);

        VBox corps = new VBox(4);
        corps.getStyleClass().add("card-body");

        Label titre = new Label(annonce.titre());
        titre.getStyleClass().add("card-title");
        titre.setWrapText(true);
        titre.setMaxHeight(42);

        Label prixLabel = new Label(Affichage.prix(annonce.prix()));
        prixLabel.getStyleClass().add("card-price");

        String lieu = annonce.quartier() == null ? "" : annonce.quartier().toString();
        Label quartierLabel = new Label(lieu);
        quartierLabel.getStyleClass().add("card-location");

        Label details = new Label(Affichage.pieces(annonce.nbPieces())
                + (annonce.surface() != null ? " · " + annonce.surface() + " m²" : ""));
        details.getStyleClass().add("card-specs");

        corps.getChildren().addAll(titre, prixLabel, quartierLabel, details);

        HBox badges = new HBox(6);
        if (annonce.typeLocation() != null) {
            Label typeBadge = new Label(Affichage.libelleType(annonce.typeLocation().libelle()));
            typeBadge.getStyleClass().add("badge-info");
            badges.getChildren().add(typeBadge);
        }
        if (annonce.distanceKm() != null) {
            QuartierResponse quartierRef = quartierSelectionne();
            String reference = (quartierRef != null ? quartierRef.toString() : "le quartier de reference");
            Label distance = new Label(Affichage.distance(annonce.distanceKm()) + " du quartier " + reference);
            distance.getStyleClass().add("badge-distance");
            badges.getChildren().add(distance);
        }
        if (!badges.getChildren().isEmpty()) {
            corps.getChildren().add(badges);
        }

        carte.getChildren().addAll(media, corps);
        // Réaffiche le même objet vue pour conserver la liste et sa position de défilement.
        carte.setOnMouseClicked(e -> app.ouvrirDetail(annonce.id(), () -> app.afficherSansHistorique(this)));
        return carte;
    }
}
