package mg.emit.tadytrano.client.vue;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mg.emit.tadytrano.client.TadyTranoApp;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.dto.AnnonceResumeResponse;
import mg.emit.tadytrano.client.dto.FavoriResponse;
import mg.emit.tadytrano.client.dto.PageResponse;
import mg.emit.tadytrano.client.util.Affichage;

/**
 * Consultation et gestion des annonces favorites (CDC F8).
 */
public class VueFavoris extends VBox {

    private static final int TAILLE_PAGE = 12;

    private final TadyTranoApp app;
    private final ApiClient api;

    private final FlowPane conteneurCards = new FlowPane();
    private final Label pageLabel = new Label();
    private final Button precedent = new Button("<");
    private final Button suivant = new Button(">");
    private final Label messageVide = new Label("Aucun favori pour le moment.");

    private int page = 0;
    private int totalPages = 0;
    /** Numero de sequence : ignore les reponses perimee d'une requete anterieure. */
    private int generation = 0;

    public VueFavoris(TadyTranoApp app, ApiClient api) {
        this.app = app;
        this.api = api;
        construire();
        charger();
    }

    private void construire() {
        setSpacing(15);
        setPadding(new Insets(20));

        Label titre = new Label("Mes Annonces Favorites");
        titre.setFont(Font.font("System", FontWeight.BOLD, 22));

        conteneurCards.setHgap(15);
        conteneurCards.setVgap(15);

        messageVide.getStyleClass().add("empty-message");

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

        getChildren().addAll(titre, new Separator(), messageVide, conteneurCards, pagination);
    }

    private void charger() {
        if (!app.estConnecte()) {
            messageVide.setText("Veuillez vous connecter pour accéder à vos favoris.");
            messageVide.setVisible(true);
            conteneurCards.getChildren().clear();
            pageLabel.setText("");
            precedent.setDisable(true);
            suivant.setDisable(true);
            return;
        }

        final int pageDemandee = page;
        final int gen = ++generation;
        app.appeler(() -> api.mesFavoris(pageDemandee, TAILLE_PAGE),
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

    private void afficherResultats(PageResponse<FavoriResponse> reponse) {
        conteneurCards.getChildren().clear();
        totalPages = reponse.totalPages();
        pageLabel.setText("Page " + (totalPages == 0 ? 0 : page + 1) + " / " + totalPages + " (" + reponse.totalElements() + " favoris)");
        precedent.setDisable(page <= 0);
        suivant.setDisable(page >= totalPages - 1 || totalPages == 0);

        if (reponse.contenu().isEmpty()) {
            messageVide.setText("Vous n'avez aucune annonce enregistrée en favoris.");
            messageVide.setVisible(true);
            return;
        }

        messageVide.setVisible(false);
        for (FavoriResponse favori : reponse.contenu()) {
            conteneurCards.getChildren().add(creerCarteFavori(favori));
        }
    }

    private VBox creerCarteFavori(FavoriResponse favori) {
        AnnonceResumeResponse a = favori.annonce();

        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setPrefWidth(260);
        card.getStyleClass().add("list-row");

        boolean louee = "LOUEE".equals(a.statut());
        ImageView image = Affichage.vignette(a.urlPhotoPrincipale(), 236, 140);
        if (!louee) {
            image.getStyleClass().add("thumb-clickable");
            image.setOnMouseClicked(e -> app.ouvrirDetail(a.id(), this::charger));
        }

        Label titre = new Label(a.titre());
        titre.setFont(Font.font("System", FontWeight.BOLD, 14));
        titre.setWrapText(true);
        titre.setMaxHeight(40);

        String localisation = (a.quartier() != null ? a.quartier().nom() + ", " : "")
                + (a.quartier() != null && a.quartier().ville() != null ? a.quartier().ville().nom() : "");
        Label locLabel = new Label(localisation);
        locLabel.getStyleClass().add("card-location");

        Label prix = new Label(Affichage.prix(a.prix()));
        prix.setFont(Font.font("System", FontWeight.BOLD, 15));
        prix.getStyleClass().add("card-price");

        Label statut = new Label("LOUÉE");
        statut.getStyleClass().add("badge-statut-louee");

        Button detailBtn = new Button("Voir détail");
        detailBtn.getStyleClass().add("btn-info-custom");
        detailBtn.setDisable(louee);
        if (louee) {
            detailBtn.setTooltip(new javafx.scene.control.Tooltip("Cette annonce est louée et n'est plus disponible."));
        } else {
            detailBtn.setOnAction(e -> app.ouvrirDetail(a.id(), this::charger));
        }

        Button retirerBtn = new Button("Retirer");
        retirerBtn.getStyleClass().add("btn-danger-custom");
        retirerBtn.setOnAction(e -> app.appeler(() -> {
            api.retirerFavori(a.id());
            return null;
        }, res -> charger()));

        HBox actions = new HBox(8, detailBtn, retirerBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        if (louee) {
            card.getChildren().addAll(image, titre, statut, locLabel, prix, actions);
        } else {
            card.getChildren().addAll(image, titre, locLabel, prix, actions);
        }
        return card;
    }
}
