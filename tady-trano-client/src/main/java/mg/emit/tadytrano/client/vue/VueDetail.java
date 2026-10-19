package mg.emit.tadytrano.client.vue;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mg.emit.tadytrano.client.TadyTranoApp;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.dto.AnnonceDetailResponse;
import mg.emit.tadytrano.client.dto.PhotoResponse;
import mg.emit.tadytrano.client.util.Affichage;

import java.util.List;

/** Detail d'une annonce : photos, description, contact, favori. */
public class VueDetail extends VBox {

    private final TadyTranoApp app;
    private final ApiClient api;
    private final long annonceId;
    private final Runnable retour;
    private final Button favori = new Button("Ajouter aux favoris");
    private final ImageView principale = new ImageView();
    private final Button retourBtn = new Button("< Retour");

    public VueDetail(TadyTranoApp app, ApiClient api, long annonceId, Runnable retour) {
        this.app = app;
        this.api = api;
        this.annonceId = annonceId;
        this.retour = retour;
        setSpacing(10);
        setPadding(new Insets(15));

        Button boutonRetour = retourBtn;
        boutonRetour.setOnAction(e -> retour.run());
        getChildren().add(boutonRetour);

        app.appeler(() -> api.detail(annonceId), this::afficher);
    }

    private void afficher(AnnonceDetailResponse annonce) {
        getChildren().removeIf(n -> n != retourBtn);

        principale.setFitWidth(640);
        principale.setFitHeight(380);
        principale.setPreserveRatio(true);
        majPrincipale(annonce.photos());

        HBox vignettes = new HBox(8);
        for (PhotoResponse photo : annonce.photos()) {
            ImageView v = Affichage.vignette(photo.url(), 90, 60);
            // On recharge l'image en taille principale (640x380) depuis l'URL,
            // et non la vignette 90x60 deja decodee (floue).
            v.setOnMouseClicked(e -> principale.setImage(
                    Affichage.vignette(photo.url(), 640, 380).getImage()));
            v.getStyleClass().add("thumb-clickable");
            vignettes.getChildren().add(v);
        }

        Label titre = new Label(annonce.titre());
        titre.getStyleClass().add("detail-title");
        titre.setWrapText(true);

        Label prix = new Label(Affichage.prix(annonce.prix()));
        prix.getStyleClass().add("detail-price");

        Label caract = new Label(annonce.nbPieces() + " pieces"
                + (annonce.surface() != null ? " - " + annonce.surface() + " m2" : "")
                + " - " + annonce.typeLocation().libelle()
                + " - " + annonce.quartier()
                + "   [statut : " + annonce.statut() + "]");
        caract.getStyleClass().add("muted");

        Label description = new Label(annonce.description() == null ? "" : annonce.description());
        description.setWrapText(true);
        description.setMaxWidth(640);

        VBox contact = new VBox(4);
        contact.setPadding(new Insets(10));
        contact.getStyleClass().add(annonce.contact() != null ? "contact-card" : "contact-card-locked");
        if (annonce.contact() != null) {
            Label nom = new Label("Contact : " + annonce.contact().nom());
            nom.setFont(Font.font("System", FontWeight.BOLD, 14));
            Label tel = new Label("Tel : " + annonce.contact().telephone());
            Label mail = new Label(annonce.contact().emailContact() == null
                    ? "" : "Email : " + annonce.contact().emailContact());
            contact.getChildren().addAll(nom, tel, mail);
        } else {
            contact.getChildren().add(new Label(
                    "Connectez-vous pour voir les coordonnees du proprietaire."));
        }

        favori.setOnAction(e -> basculerFavori(annonce));
        HBox actions = new HBox(15, favori);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox colonne = new VBox(10, titre, prix, caract, new Separator(),
                description, contact, actions);
        HBox contenu = new HBox(25, principale, colonne);

        getChildren().addAll(contenu, vignettes);
        majLibelleFavori();
    }

    private void majPrincipale(List<PhotoResponse> photos) {
        photos.stream()
                .filter(PhotoResponse::principale)
                .findFirst()
                .or(() -> photos.stream().findFirst())
                .ifPresent(p -> principale.setImage(
                        Affichage.vignette(p.url(), 640, 380).getImage()));
    }

    private void basculerFavori(AnnonceDetailResponse annonce) {
        if (!app.estConnecte()) {
            Affichage.erreur(new RuntimeException("Connectez-vous pour utiliser les favoris"));
            return;
        }
        boolean ajoute = "Ajouter aux favoris".equals(favori.getText());
        favori.setDisable(true); // anti double-clic pendant l'appel API
        app.appeler(() -> {
            if (ajoute) {
                api.ajouterFavori(annonce.id());
            } else {
                api.retirerFavori(annonce.id());
            }
            return null;
        }, ok -> {
            favori.setText(ajoute ? "Retirer des favoris" : "Ajouter aux favoris");
            favori.setDisable(false);
        }, err -> {
            app.gererErreur(err);
            favori.setDisable(false);
        });
    }

    private void majLibelleFavori() {
        if (!app.estConnecte()) {
            favori.setDisable(true);
            return;
        }
        favori.setDisable(true);
        app.appeler(() -> api.estEnFavori(annonceId),
                dedans -> {
                    favori.setText(dedans ? "Retirer des favoris" : "Ajouter aux favoris");
                    favori.setDisable(false);
                },
                err -> {
                    app.gererErreur(err);
                    favori.setDisable(false);
                });
    }
}
