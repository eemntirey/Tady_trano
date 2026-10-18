package mg.emit.tadytrano.client.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import mg.emit.tadytrano.client.dto.AnnonceResumeResponse;
import mg.emit.tadytrano.client.ui.AppContext;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class AnnonceCardController {

    private static final DecimalFormat FORMAT_PRIX;

    static {
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.FRENCH);
        symboles.setGroupingSeparator(' ');
        FORMAT_PRIX = new DecimalFormat("#,### Ar", symboles);
    }

    @FXML private VBox cardRoot;
    @FXML private ImageView photoImageView;
    @FXML private Label typeBadgeLabel;
    @FXML private Label statutBadgeLabel;
    @FXML private Button favButton;
    @FXML private Label prixLabel;
    @FXML private Label distanceBadgeLabel;
    @FXML private Label titreLabel;
    @FXML private Label localisationLabel;
    @FXML private Label piecesLabel;
    @FXML private Label surfaceLabel;

    private AnnonceResumeResponse annonce;
    private boolean estFavori = false;

    @FXML
    public void initialize() {
        // Coin arrondis pour l'image
        Rectangle clip = new Rectangle(300, 170);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        photoImageView.setClip(clip);

        cardRoot.setOnMouseClicked(e -> {
            if (e.getTarget() != favButton && annonce != null) {
                AppContext.get().getNavigationManager().naviguerVersDetail(annonce.id());
            }
        });
    }

    public void initialiser(AnnonceResumeResponse annonce, boolean favori) {
        this.annonce = annonce;
        this.estFavori = favori;

        titreLabel.setText(annonce.titre() != null ? annonce.titre() : "Sans titre");

        BigDecimal prix = annonce.prix() != null ? annonce.prix() : BigDecimal.ZERO;
        prixLabel.setText(FORMAT_PRIX.format(prix));

        if (annonce.typeLocation() != null) {
            String libelle = annonce.typeLocation().libelle();
            typeBadgeLabel.setText(libelle);
            typeBadgeLabel.getStyleClass().removeAll("badge-hotel", "badge-etage", "badge-piece");
            if ("HOTEL".equalsIgnoreCase(annonce.typeLocation().code())) {
                typeBadgeLabel.getStyleClass().add("badge-hotel");
            } else if ("ETAGE".equalsIgnoreCase(annonce.typeLocation().code())) {
                typeBadgeLabel.getStyleClass().add("badge-etage");
            } else {
                typeBadgeLabel.getStyleClass().add("badge-piece");
            }
        }

        if ("LOUEE".equalsIgnoreCase(annonce.statut())) {
            statutBadgeLabel.setText("LOUÉE");
            statutBadgeLabel.setVisible(true);
        } else if ("MASQUEE".equalsIgnoreCase(annonce.statut())) {
            statutBadgeLabel.setText("MASQUÉE");
            statutBadgeLabel.setVisible(true);
        } else {
            statutBadgeLabel.setVisible(false);
        }

        String quartierNom = annonce.quartier() != null ? annonce.quartier().nom() : "";
        localisationLabel.setText(quartierNom);

        int pieces = annonce.nbPieces() != null ? annonce.nbPieces() : 1;
        piecesLabel.setText("🛏️ " + pieces + (pieces > 1 ? " pièces" : " pièce"));

        if (annonce.surface() != null && annonce.surface() > 0) {
            surfaceLabel.setText("📐 " + annonce.surface() + " m²");
            surfaceLabel.setVisible(true);
        } else {
            surfaceLabel.setVisible(false);
        }

        if (annonce.distanceKm() != null) {
            distanceBadgeLabel.setText(String.format(Locale.FRENCH, "📍 %.1f km", annonce.distanceKm()));
            distanceBadgeLabel.setVisible(true);
        } else {
            distanceBadgeLabel.setVisible(false);
        }

        mettreAJourBoutonFavori();

        // Chargement d'image asynchrone
        String urlPhoto = annonce.urlPhotoPrincipale();
        if (urlPhoto != null && !urlPhoto.isBlank()) {
            Image image = new Image(urlPhoto, 300, 170, false, true, true);
            photoImageView.setImage(image);
        }
    }

    private void mettreAJourBoutonFavori() {
        if (estFavori) {
            favButton.setText("❤️");
            favButton.setStyle("-fx-text-fill: #e11d48;");
        } else {
            favButton.setText("♡");
            favButton.setStyle("-fx-text-fill: #64748b;");
        }
    }

    @FXML
    private void surBasculerFavori() {
        if (!AppContext.get().getSession().estConnecte()) {
            AppContext.get().showToast("Connectez-vous pour ajouter aux favoris", true);
            AppContext.get().getNavigationManager().ouvrirDialogueConnexion();
            return;
        }

        if (annonce == null) return;

        new Thread(() -> {
            try {
                if (estFavori) {
                    AppContext.get().getApiClient().retirerFavori(annonce.id());
                    estFavori = false;
                    javafx.application.Platform.runLater(() -> {
                        mettreAJourBoutonFavori();
                        AppContext.get().showSuccess("Retiré des favoris");
                    });
                } else {
                    AppContext.get().getApiClient().ajouterFavori(annonce.id());
                    estFavori = true;
                    javafx.application.Platform.runLater(() -> {
                        mettreAJourBoutonFavori();
                        AppContext.get().showSuccess("Ajouté aux favoris !");
                    });
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> AppContext.get().showError("Erreur : " + e.getMessage()));
            }
        }).start();
    }
}
