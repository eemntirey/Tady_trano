package mg.emit.tadytrano.client.vue;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mg.emit.tadytrano.client.TadyTranoApp;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.dto.*;
import mg.emit.tadytrano.client.util.Affichage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Espace proprietaire / agence : gestion complete de ses annonces (CDC F2).
 * Publication, modification, changement de statut, photos, suppression.
 */
public class VueMesAnnonces extends VBox {

    private static final int TAILLE_PAGE = 10;

    private final TadyTranoApp app;
    private final ApiClient api;

    private final VBox listeAnnonces = new VBox(12);
    private final Label pageLabel = new Label();
    private final Button precedent = new Button("<");
    private final Button suivant = new Button(">");
    private final Label messageVide = new Label("Aucune annonce publiée pour le moment.");

    private int page = 0;
    private int totalPages = 0;
    /** Numero de sequence : ignore les reponses perimee d'une requete anterieure. */
    private int generation = 0;

    public VueMesAnnonces(TadyTranoApp app, ApiClient api) {
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

        Label titre = new Label("Mes Annonces Immobilières");
        titre.setFont(Font.font("System", FontWeight.BOLD, 22));

        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);

        Button nouvelleAnnonce = new Button("+ Publier une annonce");
        nouvelleAnnonce.getStyleClass().add("btn-success-custom");
        nouvelleAnnonce.setOnAction(e -> ouvrirFormulaire(null));

        enTete.getChildren().addAll(titre, espace, nouvelleAnnonce);

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

        ScrollPane scroll = new ScrollPane(listeAnnonces);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparent");

        getChildren().addAll(enTete, new Separator(), messageVide, scroll, pagination);
    }

    private void charger() {
        if (!app.estConnecte()) {
            messageVide.setText("Veuillez vous connecter avec un compte Propriétaire pour voir vos annonces.");
            messageVide.setVisible(true);
            listeAnnonces.getChildren().clear();
            return;
        }

        final int pageDemandee = page;
        final int gen = ++generation;
        app.appeler(() -> api.mesAnnonces(pageDemandee, TAILLE_PAGE),
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
        listeAnnonces.getChildren().clear();
        totalPages = reponse.totalPages();
        pageLabel.setText("Page " + (totalPages == 0 ? 0 : page + 1) + " / " + totalPages + " (" + reponse.totalElements() + " annonces)");
        precedent.setDisable(page <= 0);
        suivant.setDisable(page >= totalPages - 1 || totalPages == 0);

        if (reponse.contenu().isEmpty()) {
            messageVide.setText("Vous n'avez pas encore publié d'annonce. Cliquez sur '+ Publier une annonce' pour commencer.");
            messageVide.setVisible(true);
            return;
        }

        messageVide.setVisible(false);
        for (AnnonceResumeResponse annonce : reponse.contenu()) {
            listeAnnonces.getChildren().add(creerLigneAnnonce(annonce));
        }
    }

    private HBox creerLigneAnnonce(AnnonceResumeResponse a) {
        HBox ligne = new HBox(15);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setPadding(new Insets(12));
        ligne.getStyleClass().add("list-row");

        ImageView image = Affichage.vignette(a.urlPhotoPrincipale(), 120, 80);

        VBox infos = new VBox(4);
        Label titre = new Label(a.titre());
        titre.setFont(Font.font("System", FontWeight.BOLD, 15));

        String loc = (a.quartier() != null ? a.quartier().nom() + ", " : "")
                + (a.quartier() != null && a.quartier().ville() != null ? a.quartier().ville().nom() : "");
        Label locLabel = new Label(loc + " • " + a.nbPieces() + " pièce(s)");
        locLabel.getStyleClass().add("card-location");

        Label prix = new Label(Affichage.prix(a.prix()));
        prix.setFont(Font.font("System", FontWeight.BOLD, 14));
        prix.getStyleClass().add("card-price");

        infos.getChildren().addAll(titre, locLabel, prix);
        HBox.setHgrow(infos, Priority.ALWAYS);

        // Badge statut
        Label statutBadge = new Label(a.statut());
        statutBadge.setPadding(new Insets(4, 8, 4, 8));
        switch (a.statut()) {
            case "ACTIVE" -> statutBadge.getStyleClass().add("badge-statut-active");
            case "LOUEE" -> statutBadge.getStyleClass().add("badge-statut-louee");
            default -> statutBadge.getStyleClass().add("badge-statut-masquee");
        }

        // Actions
        Button photosBtn = new Button("Photos");
        photosBtn.setOnAction(e -> ouvrirGestionPhotos(a.id()));

        Button statutBtn = new Button("Statut");
        boolean masqueeParModeration = "MASQUEE".equals(a.statut());
        statutBtn.setDisable(masqueeParModeration);
        if (masqueeParModeration) {
            statutBtn.setTooltip(new Tooltip("Annonce masquee par l'administration : contactez le support."));
        }
        statutBtn.setOnAction(e -> ouvrirChangementStatut(a));

        Button modifierBtn = new Button("Modifier");
        modifierBtn.setOnAction(e -> ouvrirFormulaire(a.id()));

        Button supprimerBtn = new Button("Supprimer");
        supprimerBtn.getStyleClass().add("btn-danger-custom");
        supprimerBtn.setOnAction(e -> confirmerSuppression(a));

        HBox boutons = new HBox(8, statutBadge, photosBtn, statutBtn, modifierBtn, supprimerBtn);
        boutons.setAlignment(Pos.CENTER_RIGHT);

        ligne.getChildren().addAll(image, infos, boutons);
        return ligne;
    }

    private void ouvrirChangementStatut(AnnonceResumeResponse a) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(a.statut(), "ACTIVE", "LOUEE");
        dialog.setTitle("Statut de l'annonce");
        dialog.setHeaderText("Modifier le statut de : " + a.titre());
        dialog.setContentText("Nouveau statut :");

        Optional<String> res = dialog.showAndWait();
        res.ifPresent(nouveauStatut -> {
            app.appeler(() -> {
                api.changerStatutAnnonce(a.id(), new StatutAnnonceRequest(nouveauStatut));
                return null;
            }, r -> charger());
        });
    }

    private void confirmerSuppression(AnnonceResumeResponse a) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'annonce : " + a.titre() + " ?");
        alert.setContentText("Cette action supprimera définitivement l'annonce ainsi que ses photos.");

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            app.appeler(() -> {
                api.supprimerAnnonce(a.id());
                return null;
            }, r -> charger());
        }
    }

    private void ouvrirFormulaire(Long idModification) {
        Dialog<AnnonceRequest> dialog = new Dialog<>();
        dialog.setTitle(idModification == null ? "Publier une nouvelle annonce" : "Modifier l'annonce");
        dialog.setHeaderText(idModification == null ? "Renseignez les détails du bien à louer" : "Mise à jour des informations");

        ButtonType enregistrerType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(enregistrerType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titreF = new TextField();
        titreF.setPrefWidth(300);
        TextArea descF = new TextArea();
        descF.setPrefRowCount(3);
        descF.setWrapText(true);

        ComboBox<TypeLocationResponse> typeCombo = new ComboBox<>();
        ComboBox<VilleResponse> villeCombo = new ComboBox<>();
        ComboBox<QuartierResponse> quartierCombo = new ComboBox<>();

        TextField prixF = new TextField();
        TextField surfaceF = new TextField();
        Spinner<Integer> piecesF = new Spinner<>(1, 30, 1);
        TextField latF = new TextField();
        TextField lngF = new TextField();

        grid.add(new Label("Titre :"), 0, 0);
        grid.add(titreF, 1, 0);
        grid.add(new Label("Description :"), 0, 1);
        grid.add(descF, 1, 1);
        grid.add(new Label("Type de location :"), 0, 2);
        grid.add(typeCombo, 1, 2);
        grid.add(new Label("Ville :"), 0, 3);
        grid.add(villeCombo, 1, 3);
        grid.add(new Label("Quartier :"), 0, 4);
        grid.add(quartierCombo, 1, 4);
        grid.add(new Label("Prix mensuel (Ar) :"), 0, 5);
        grid.add(prixF, 1, 5);
        grid.add(new Label("Surface (m²) :"), 0, 6);
        grid.add(surfaceF, 1, 6);
        grid.add(new Label("Nombre de pièces :"), 0, 7);
        grid.add(piecesF, 1, 7);
        grid.add(new Label("Latitude GPS :"), 0, 8);
        grid.add(latF, 1, 8);
        grid.add(new Label("Longitude GPS :"), 0, 9);
        grid.add(lngF, 1, 9);

        // Chargement du referentiel
        app.appeler(api::typesLocation, typeCombo.getItems()::setAll);
        app.appeler(api::villes, villes -> {
            villeCombo.getItems().setAll(villes);
            if (!villes.isEmpty()) {
                villeCombo.getSelectionModel().select(0);
            }
        });

        villeCombo.setOnAction(e -> {
            VilleResponse v = villeCombo.getValue();
            if (v != null) {
                app.appeler(() -> api.quartiers(v.id()), quartiers -> {
                    quartierCombo.getItems().setAll(quartiers);
                    if (!quartiers.isEmpty()) {
                        quartierCombo.getSelectionModel().select(0);
                    }
                });
            }
        });

        quartierCombo.setOnAction(e -> {
            QuartierResponse q = quartierCombo.getValue();
            if (q != null && q.latitude() != null && q.longitude() != null && latF.getText().isBlank()) {
                latF.setText(String.valueOf(q.latitude()));
                lngF.setText(String.valueOf(q.longitude()));
            }
        });

        // Prefill si modification
        if (idModification != null) {
            app.appeler(() -> api.detail(idModification), d -> {
                titreF.setText(d.titre());
                descF.setText(d.description());
                prixF.setText(d.prix() != null ? d.prix().toPlainString() : "");
                surfaceF.setText(d.surface() != null ? String.valueOf(d.surface()) : "");
                if (d.nbPieces() != null) piecesF.getValueFactory().setValue(d.nbPieces());
                if (d.latitude() != null) latF.setText(String.valueOf(d.latitude()));
                if (d.longitude() != null) lngF.setText(String.valueOf(d.longitude()));
            });
        }

        Label erreurSaisie = new Label();
        erreurSaisie.setWrapText(true);
        erreurSaisie.setStyle("-fx-text-fill: #b91c1c;");
        grid.add(erreurSaisie, 0, 10, 2, 1);
        dialog.getDialogPane().setContent(grid);

        Button boutonEnregistrer = (Button) dialog.getDialogPane().lookupButton(enregistrerType);
        boutonEnregistrer.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                construireDemande(titreF, descF, typeCombo, quartierCombo,
                        prixF, surfaceF, piecesF, latF, lngF);
                erreurSaisie.setText("");
            } catch (IllegalArgumentException ex) {
                erreurSaisie.setText(ex.getMessage());
                event.consume();
            }
        });

        dialog.setResultConverter(bouton -> {
            if (bouton == enregistrerType) {
                return construireDemande(titreF, descF, typeCombo, quartierCombo,
                        prixF, surfaceF, piecesF, latF, lngF);
            }
            return null;
        });

        Optional<AnnonceRequest> resultat = dialog.showAndWait();
        resultat.ifPresent(req -> {
            if (idModification == null) {
                app.appeler(() -> api.creerAnnonce(req), r -> charger());
            } else {
                app.appeler(() -> api.modifierAnnonce(idModification, req), r -> charger());
            }
        });
    }

    private AnnonceRequest construireDemande(TextField titreF, TextArea descF,
                                                ComboBox<TypeLocationResponse> typeCombo,
                                                ComboBox<QuartierResponse> quartierCombo,
                                                TextField prixF, TextField surfaceF,
                                                Spinner<Integer> piecesF, TextField latF, TextField lngF) {
        String titre = titreF.getText() == null ? "" : titreF.getText().trim();
        if (titre.isBlank()) {
            throw new IllegalArgumentException("Le titre de l'annonce est obligatoire.");
        }
        TypeLocationResponse type = typeCombo.getValue();
        if (type == null) {
            throw new IllegalArgumentException("Sélectionnez un type de location.");
        }
        QuartierResponse quartier = quartierCombo.getValue();
        if (quartier == null) {
            throw new IllegalArgumentException("Sélectionnez un quartier.");
        }

        String textePrix = normaliserNombre(prixF.getText());
        if (textePrix.isBlank()) {
            throw new IllegalArgumentException("Le prix mensuel est obligatoire.");
        }
        BigDecimal prix;
        try {
            prix = new BigDecimal(textePrix);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Prix invalide. Exemple : 80 000 ou 12,5.");
        }
        if (prix.signum() <= 0 || prix.scale() > 2 || prix.precision() - prix.scale() > 10) {
            throw new IllegalArgumentException("Le prix doit etre positif et comporter au maximum 2 decimales.");
        }

        Integer surface = null;
        if (surfaceF.getText() != null && !surfaceF.getText().isBlank()) {
            try {
                surface = Integer.parseInt(surfaceF.getText().trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("La surface doit etre un nombre entier.");
            }
            if (surface < 0) {
                throw new IllegalArgumentException("La surface ne peut pas etre negative.");
            }
        }

        Double lat = lireCoordonnee(latF.getText(), "latitude");
        Double lng = lireCoordonnee(lngF.getText(), "longitude");
        if ((lat == null) != (lng == null)) {
            throw new IllegalArgumentException("Renseignez les deux coordonnees GPS, ou laissez-les toutes les deux vides.");
        }
        if (lat != null && (!Double.isFinite(lat) || lat < -90 || lat > 90
                || !Double.isFinite(lng) || lng < -180 || lng > 180)) {
            throw new IllegalArgumentException("Coordonnees GPS hors limites (latitude -90..90, longitude -180..180).");
        }

        Integer pieces = piecesF.getValue();
        return new AnnonceRequest(type.id(), quartier.id(), titre,
                descF.getText() == null ? null : descF.getText().trim(),
                prix, surface, pieces, lat, lng);
    }

    private static String normaliserNombre(String texte) {
        if (texte == null) {
            return "";
        }
        return texte.trim().replace(" ", "").replace("\u00A0", "")
                .replace("\u202F", "").replace(',', '.');
    }

    private static Double lireCoordonnee(String texte, String nom) {
        String normalise = normaliserNombre(texte);
        if (normalise.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(normalise);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("La " + nom + " doit etre un nombre valide.");
        }
    }

    private void ouvrirGestionPhotos(Long annonceId) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Gestion des photos de l'annonce #" + annonceId);
        dialog.setHeaderText("Photos associées au bien (max 10 photos)");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox contenu = new VBox(12);
        contenu.setPadding(new Insets(15));
        contenu.setPrefWidth(500);

        HBox ajoutBox = new HBox(8);
        TextField urlF = new TextField();
        urlF.setPromptText("URL de la photo (ex: https://images.unsplash.com/...)");
        urlF.setPrefWidth(320);
        CheckBox principaleCb = new CheckBox("Principale");
        Button ajouterBtn = new Button("Ajouter");
        ajouterBtn.getStyleClass().add("btn-success-custom");

        ajoutBox.getChildren().addAll(urlF, principaleCb, ajouterBtn);

        VBox listePhotos = new VBox(8);
        ScrollPane scroll = new ScrollPane(listePhotos);
        scroll.setPrefHeight(300);
        scroll.setFitToWidth(true);

        Runnable rechargerPhotos = () -> {
            app.appeler(() -> api.photos(annonceId), photos -> {
                listePhotos.getChildren().clear();
                if (photos.isEmpty()) {
                    listePhotos.getChildren().add(new Label("Aucune photo pour le moment."));
                }
                for (PhotoResponse p : photos) {
                    HBox ligne = new HBox(10);
                    ligne.setAlignment(Pos.CENTER_LEFT);
                    ligne.setPadding(new Insets(6));
                    ligne.getStyleClass().add("photo-row");

                    ImageView iv = Affichage.vignette(p.url(), 70, 50);
                    Label badge = new Label(p.principale() ? "★ Principale" : "");
                    badge.getStyleClass().add("badge-principale");

                    Label urlLbl = new Label(p.url());
                    urlLbl.setMaxWidth(200);
                    HBox.setHgrow(urlLbl, Priority.ALWAYS);

                    Button delBtn = new Button("✕");
                    delBtn.getStyleClass().add("btn-danger-custom");
                    delBtn.setOnAction(ev -> app.appeler(() -> {
                        api.supprimerPhoto(annonceId, p.id());
                        return null;
                    }, ok -> charger()));

                    ligne.getChildren().addAll(iv, urlLbl, badge, delBtn);
                    listePhotos.getChildren().add(ligne);
                }
            });
        };

        ajouterBtn.setOnAction(e -> {
            String u = urlF.getText().trim();
            if (!u.isBlank()) {
                app.appeler(() -> api.ajouterPhoto(annonceId, new PhotoRequest(u, principaleCb.isSelected())), ok -> {
                    urlF.clear();
                    principaleCb.setSelected(false);
                    rechargerPhotos.run();
                    charger();
                });
            }
        });

        rechargerPhotos.run();
        contenu.getChildren().addAll(ajoutBox, new Separator(), scroll);
        dialog.getDialogPane().setContent(contenu);
        dialog.showAndWait();
    }
}
