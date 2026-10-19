package mg.emit.tadytrano.client.vue;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mg.emit.tadytrano.client.TadyTranoApp;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.dto.AnnonceResumeResponse;
import mg.emit.tadytrano.client.dto.PageResponse;
import mg.emit.tadytrano.client.dto.StatutAnnonceRequest;
import mg.emit.tadytrano.client.util.Affichage;

/**
 * Espace moderation et administration (CDC F10).
 * Reserve a l'administrateur de la plateforme Tady Trano.
 */
public class VueAdmin extends VBox {

    private static final int TAILLE_PAGE = 12;

    private final TadyTranoApp app;
    private final ApiClient api;

    private final ComboBox<String> filtreStatut = new ComboBox<>();
    private final VBox listeAdmin = new VBox(10);
    private final Label pageLabel = new Label();
    private final Button precedent = new Button("<");
    private final Button suivant = new Button(">");
    private final Label messageVide = new Label("Aucune annonce dans cette catégorie.");

    private int page = 0;
    private int totalPages = 0;
    /** Numero de sequence : ignore les reponses perimee d'une requete anterieure. */
    private int generation = 0;

    public VueAdmin(TadyTranoApp app, ApiClient api) {
        this.app = app;
        this.api = api;
        construire();
        charger();
    }

    private void construire() {
        setSpacing(15);
        setPadding(new Insets(20));

        HBox enTete = new HBox(15);
        enTete.setAlignment(Pos.CENTER_LEFT);

        Label titre = new Label("Administration & Modération des Annonces");
        titre.setFont(Font.font("System", FontWeight.BOLD, 22));

        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);

        filtreStatut.getItems().addAll("TOUS", "ACTIVE", "LOUEE", "MASQUEE");
        filtreStatut.getSelectionModel().select(0);
        filtreStatut.setOnAction(e -> {
            page = 0;
            charger();
        });

        HBox filtreBox = new HBox(8, new Label("Filtrer par statut :"), filtreStatut);
        filtreBox.setAlignment(Pos.CENTER_RIGHT);

        enTete.getChildren().addAll(titre, espace, filtreBox);

        // Pagination
        precedent.setOnAction(e -> {
            if (page > 0) {
                page--;
                charger();
            }
        });
        suivant.setOnAction(e -> {
            if (page < totalPages - 1) {
                page++;
                charger();
            }
        });

        HBox pagination = new HBox(10, precedent, pageLabel, suivant);
        pagination.setAlignment(Pos.CENTER);
        pagination.setPadding(new Insets(10));

        ScrollPane scroll = new ScrollPane(listeAdmin);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparent");

        getChildren().addAll(enTete, new Separator(), messageVide, scroll, pagination);
    }

    private void charger() {
        if (!app.estAdmin()) {
            messageVide.setText("L'espace de modération est réservé aux administrateurs.");
            messageVide.setVisible(true);
            listeAdmin.getChildren().clear();
            pageLabel.setText("");
            precedent.setDisable(true);
            suivant.setDisable(true);
            return;
        }
        String statutSelectionne = filtreStatut.getValue();
        String param = ("TOUS".equals(statutSelectionne)) ? null : statutSelectionne;
        final int pageDemandee = page;
        final int gen = ++generation;

        app.appeler(() -> api.toutesLesAnnonces(param, pageDemandee, TAILLE_PAGE),
                reponse -> {
                    if (gen != generation) {
                        return; // reponse perimee
                    }
                    afficherResultats(reponse);
                },
                erreur -> {
                    if (gen != generation) {
                        return;
                    }
                    app.gererErreur(erreur);
                });
    }

    private void afficherResultats(PageResponse<AnnonceResumeResponse> reponse) {
        // Apres une moderation : si la page courante n'existe plus, revenir a la derniere valide.
        if (reponse.contenu().isEmpty() && page > 0) {
            int dernierePage = Math.max(0, reponse.totalPages() - 1);
            if (dernierePage != page) {
                page = dernierePage;
                charger();
                return;
            }
        }
        listeAdmin.getChildren().clear();
        totalPages = reponse.totalPages();
        pageLabel.setText("Page " + (totalPages == 0 ? 0 : page + 1) + " / " + totalPages + " (" + reponse.totalElements() + " annonces au total)");
        precedent.setDisable(page <= 0);
        suivant.setDisable(page >= totalPages - 1 || totalPages == 0);

        if (reponse.contenu().isEmpty()) {
            messageVide.setText("Aucune annonce trouvée avec ce statut.");
            messageVide.setVisible(true);
            return;
        }

        messageVide.setVisible(false);
        for (AnnonceResumeResponse annonce : reponse.contenu()) {
            listeAdmin.getChildren().add(creerLigneAdmin(annonce));
        }
    }

    private HBox creerLigneAdmin(AnnonceResumeResponse a) {
        HBox ligne = new HBox(15);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setPadding(new Insets(12));
        ligne.getStyleClass().add("list-row");

        ImageView image = Affichage.vignette(a.urlPhotoPrincipale(), 110, 75);

        VBox infos = new VBox(4);
        Label idTitre = new Label("#" + a.id() + " - " + a.titre());
        idTitre.setFont(Font.font("System", FontWeight.BOLD, 14));

        String loc = (a.quartier() != null ? a.quartier().nom() + ", " : "")
                + (a.quartier() != null && a.quartier().ville() != null ? a.quartier().ville().nom() : "");
        Label locLabel = new Label(loc + " • " + a.nbPieces() + " pièce(s) • " + Affichage.prix(a.prix()));
        locLabel.getStyleClass().add("card-location");

        infos.getChildren().addAll(idTitre, locLabel);
        HBox.setHgrow(infos, Priority.ALWAYS);

        // Badge statut
        Label statutBadge = new Label(a.statut());
        statutBadge.setPadding(new Insets(4, 8, 4, 8));
        switch (a.statut()) {
            case "ACTIVE" -> statutBadge.getStyleClass().add("badge-statut-active");
            case "LOUEE" -> statutBadge.getStyleClass().add("badge-statut-louee");
            default -> statutBadge.getStyleClass().add("badge-statut-masquee");
        }

        // Actions de moderation
        Button detailBtn = new Button("Aperçu");
        detailBtn.setOnAction(e -> app.ouvrirDetail(a.id(), this::charger));

        Button activerBtn = new Button("Valider / Activer");
        activerBtn.getStyleClass().add("btn-success-custom");
        activerBtn.setOnAction(e -> moderer(a.id(), "ACTIVE", activerBtn));
        activerBtn.setDisable("ACTIVE".equals(a.statut()));

        Button masquerBtn = new Button("Masquer (Abus)");
        masquerBtn.getStyleClass().add("btn-warning-custom");
        masquerBtn.setOnAction(e -> moderer(a.id(), "MASQUEE", masquerBtn));
        masquerBtn.setDisable("MASQUEE".equals(a.statut()));

        HBox boutons = new HBox(8, statutBadge, detailBtn, activerBtn, masquerBtn);
        boutons.setAlignment(Pos.CENTER_RIGHT);

        ligne.getChildren().addAll(image, infos, boutons);
        return ligne;
    }

    private void moderer(Long id, String statut, Button bouton) {
        bouton.setDisable(true);
        app.appeler(() -> {
            api.modererAnnonce(id, new StatutAnnonceRequest(statut));
            return null;
        },
        r -> {
            charger();
            Affichage.succes("Annonce #".concat(String.valueOf(id))
                    .concat(" : statut ").concat(statut).concat("."));
        },
        err -> {
            bouton.setDisable(false);
            app.gererErreur(err);
        });
    }
}
