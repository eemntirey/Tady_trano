package mg.emit.tadytrano.client.vue;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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

    public VueDetail(TadyTranoApp app, ApiClient api, long annonceId, Runnable retour) {
        this.app = app;
        this.api = api;
        this.annonceId = annonceId;
        this.retour = retour;
        setSpacing(10);
        setPadding(new Insets(15));

        app.appeler(() -> api.detail(annonceId), this::afficher);
    }

    private void afficher(AnnonceDetailResponse annonce) {
        getChildren().clear();

        principale.setFitWidth(640);
        principale.setFitHeight(380);
        principale.setPreserveRatio(true);
        majPrincipale(annonce.photos());

        HBox vignettes = new HBox(8);
        for (PhotoResponse photo : annonce.photos()) {
            ImageView v = Affichage.vignette(photo.url(), 90, 60);
            // Recharger dans la vue principale : une ImageView creee en
            // chargement asynchrone ne doit pas etre copiee via getImage().
            v.setOnMouseClicked(e -> Affichage.charger(principale, photo.url(), 640, 380));
            v.getStyleClass().add("thumb-clickable");
            vignettes.getChildren().add(v);
        }

        Label titre = new Label(annonce.titre());
        titre.getStyleClass().add("detail-title");
        titre.setWrapText(true);

        Label prix = new Label(Affichage.prix(annonce.prix()));
        prix.getStyleClass().add("detail-price");

        String metadonnees = Affichage.pieces(annonce.nbPieces())
                + (annonce.surface() != null ? " · " + annonce.surface() + " m²" : "")
                + " · " + Affichage.libelleType(annonce.typeLocation() == null ? "" : annonce.typeLocation().libelle())
                + " · " + annonce.quartier();
        Label caract = new Label(metadonnees);
        caract.getStyleClass().add("muted");
        caract.setWrapText(true);

        Label statut = new Label("Statut : " + annonce.statut());
        statut.getStyleClass().add("muted");

        Label description = new Label(annonce.description() == null ? "" : annonce.description());
        description.setWrapText(true);
        description.setMaxWidth(640);

        VBox contact = new VBox(8);
        contact.setPadding(new Insets(10));
        contact.getStyleClass().add(annonce.contact() != null ? "contact-card" : "contact-card-locked");
        if (annonce.contact() != null) {
            Label nom = new Label("Contact : " + annonce.contact().nom());
            nom.setFont(Font.font("System", FontWeight.BOLD, 14));
            Label tel = new Label("Tél. : " + annonce.contact().telephone());
            Label mail = new Label(annonce.contact().emailContact() == null
                    ? "" : "Email : " + annonce.contact().emailContact());
            contact.getChildren().addAll(nom, tel, mail);
        } else {
            Label verrou = new Label("Connectez-vous pour voir les coordonnées du propriétaire.");
            verrou.setWrapText(true);
            Button ouvrirConnexion = new Button("Se connecter");
            ouvrirConnexion.getStyleClass().add("btn-primary-custom");
            ouvrirConnexion.setOnAction(e -> app.ouvrir(new VueConnexion(app, api)));
            contact.getChildren().addAll(verrou, ouvrirConnexion);
        }

        favori.setOnAction(e -> basculerFavori(annonce));
        HBox actions = new HBox(15, favori);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox colonne = new VBox(10, titre, prix, caract, statut, new Separator(),
                description, contact, actions);
        colonne.setFillWidth(true);
        HBox contenu = new HBox(25, principale, colonne);
        HBox.setHgrow(colonne, Priority.ALWAYS);

        getChildren().addAll(contenu, vignettes);
        majLibelleFavori();
    }

    private void majPrincipale(List<PhotoResponse> photos) {
        PhotoResponse photoPrincipale = photos.stream()
                .filter(PhotoResponse::principale)
                .findFirst()
                .or(() -> photos.stream().findFirst())
                .orElse(null);
        // Afficher un placeholder meme si l'annonce n'a aucune photo.
        // charger() mettra a jour l'ImageView une fois le telechargement termine.
        Affichage.charger(principale,
                photoPrincipale == null ? null : photoPrincipale.url(), 640, 380);
    }

    private void basculerFavori(AnnonceDetailResponse annonce) {
        if (!app.estConnecte()) {
            app.ouvrir(new VueConnexion(app, api));
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
            favori.setText("Se connecter pour gérer les favoris");
            favori.setDisable(false);
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
