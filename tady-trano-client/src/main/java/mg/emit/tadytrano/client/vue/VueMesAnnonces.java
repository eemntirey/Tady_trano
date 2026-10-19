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
        // Le serveur exige le role PROPRIETAIRE pour POST /api/annonces :
        // on bloque tot avec un message clair au lieu du 403.
        nouvelleAnnonce.setOnAction(e -> {
            if (!app.estProprietaire()) {
                Affichage.erreur(new RuntimeException(
                        "La publication est réservée aux comptes Propriétaire."));
                return;
            }
            ouvrirFormulaire(null);
        });

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
        if (!app.estProprietaire()) {
            messageVide.setText("L'espace « Mes annonces » est réservé aux comptes Propriétaire.");
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
        // Apres une suppression : si la page courante n'existe plus, revenir a la derniere valide.
        if (reponse.contenu().isEmpty() && page > 0) {
            int dernierePage = Math.max(0, reponse.totalPages() - 1);
            if (dernierePage != page) {
                page = dernierePage;
                charger();
                return;
            }
        }
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
        statutBtn.setOnAction(e -> ouvrirChangementStatut(a, statutBtn));

        Button modifierBtn = new Button("Modifier");
        modifierBtn.setOnAction(e -> ouvrirFormulaire(a.id()));

        Button supprimerBtn = new Button("Supprimer");
        supprimerBtn.getStyleClass().add("btn-danger-custom");
        supprimerBtn.setOnAction(e -> confirmerSuppression(a, supprimerBtn));

        HBox boutons = new HBox(8, statutBadge, photosBtn, statutBtn, modifierBtn, supprimerBtn);
        boutons.setAlignment(Pos.CENTER_RIGHT);

        ligne.getChildren().addAll(image, infos, boutons);
        return ligne;
    }

    private void ouvrirChangementStatut(AnnonceResumeResponse a, Button bouton) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(a.statut(), "ACTIVE", "LOUEE", "MASQUEE");
        dialog.setTitle("Statut de l'annonce");
        dialog.setHeaderText("Modifier le statut de : " + a.titre());
        dialog.setContentText("Nouveau statut :");

        Optional<String> res = dialog.showAndWait();
        res.ifPresent(nouveauStatut -> {
            bouton.setDisable(true);
            app.appeler(() -> {
                api.changerStatutAnnonce(a.id(), new StatutAnnonceRequest(nouveauStatut));
                return null;
            },
            r -> {
                charger();
                Affichage.succes("Statut mis à jour : " + nouveauStatut + ".");
            },
            err -> {
                bouton.setDisable(false);
                app.gererErreur(err);
            });
        });
    }

    private void confirmerSuppression(AnnonceResumeResponse a, Button bouton) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'annonce : " + a.titre() + " ?");
        alert.setContentText("Cette action supprimera définitivement l'annonce ainsi que ses photos.");

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            bouton.setDisable(true);
            app.appeler(() -> {
                api.supprimerAnnonce(a.id());
                return null;
            },
            r -> {
                charger();
                Affichage.succes("Annonce supprimée.");
            },
            err -> {
                bouton.setDisable(false);
                app.gererErreur(err);
            });
        }
    }

    private void ouvrirFormulaire(Long idModification) {
        // Le menu n'est pas propose aux non-proprietaires ; garde interne aussi.
        if (!app.estProprietaire()) {
            Affichage.erreur(new IllegalArgumentException(
                    "L'espace « Mes annonces » est réservé aux comptes Propriétaire."));
            return;
        }
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

        // Quartier a preselectionner apres chargement (modification uniquement).
        final Long[] quartierCible = { null };
        // Garde : setOnAction peut se declencher lors d'une selection programmatique ;
        // on ne recharge que si l'utilisateur change vraiment de ville.
        final Long[] villeAffichee = { null };

        villeCombo.setOnAction(e -> {
            VilleResponse selection = villeCombo.getValue();
            Long selectionId = selection == null ? null : selection.id();
            if (villeAffichee[0] != null && villeAffichee[0].equals(selectionId)) {
                return;
            }
            villeAffichee[0] = selectionId;
            chargerQuartiers(villeCombo, quartierCombo, quartierCible[0]);
        });

        quartierCombo.setOnAction(e -> {
            QuartierResponse q = quartierCombo.getValue();
            if (q != null && q.latitude() != null && q.longitude() != null && latF.getText().isBlank()) {
                latF.setText(String.valueOf(q.latitude()));
                lngF.setText(String.valueOf(q.longitude()));
            }
        });

        // Prefill si modification : on charge d'abord l'annonce, puis les referentiels
        // en preslectionnant type / ville / quartier de l'annonce existante.
        if (idModification != null) {
            app.appeler(() -> api.detail(idModification), d -> {
                titreF.setText(d.titre());
                descF.setText(d.description());
                prixF.setText(d.prix() != null ? d.prix().toPlainString() : "");
                surfaceF.setText(d.surface() != null ? String.valueOf(d.surface()) : "");
                if (d.nbPieces() != null) piecesF.getValueFactory().setValue(d.nbPieces());
                if (d.latitude() != null) latF.setText(String.valueOf(d.latitude()));
                if (d.longitude() != null) lngF.setText(String.valueOf(d.longitude()));
                quartierCible[0] = d.quartier() != null ? d.quartier().id() : null;
                chargerReferentiels(typeCombo, villeCombo, quartierCombo,
                        d.typeLocation() != null ? d.typeLocation().id() : null,
                        d.quartier() != null && d.quartier().ville() != null
                                ? d.quartier().ville().id() : null,
                        quartierCible, villeAffichee);
            });
        } else {
            chargerReferentiels(typeCombo, villeCombo, quartierCombo, null, null,
                    quartierCible, villeAffichee);
        }

        Label erreurSaisie = new Label();
        erreurSaisie.setWrapText(true);
        erreurSaisie.setStyle("-fx-text-fill: #b91c1c;");
        grid.add(erreurSaisie, 0, 10, 2, 1);
        dialog.getDialogPane().setContent(grid);

        Button boutonEnregistrer = (Button) dialog.getDialogPane().lookupButton(enregistrerType);
        final boolean[] enCours = { false };
        boutonEnregistrer.addEventFilter(ActionEvent.ACTION, event -> {
            if (enCours[0]) {
                event.consume(); // anti double-clic : une seule soumission par ouverture
                return;
            }
            try {
                construireDemande(titreF, descF, typeCombo, quartierCombo,
                        prixF, surfaceF, piecesF, latF, lngF);
                erreurSaisie.setText("");
                enCours[0] = true;
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
                app.appeler(() -> api.creerAnnonce(req), r -> {
                    charger();
                    Affichage.succes("Annonce publiée.");
                });
            } else {
                app.appeler(() -> api.modifierAnnonce(idModification, req), r -> {
                    charger();
                    Affichage.succes("Annonce mise à jour.");
                });
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

    /** Charge types + villes et preslectionne ceux de l'annonce (null = creation). */
    private void chargerReferentiels(ComboBox<TypeLocationResponse> typeCombo,
                                     ComboBox<VilleResponse> villeCombo,
                                     ComboBox<QuartierResponse> quartierCombo,
                                     Long typeId, Long villeId,
                                     Long[] quartierCible, Long[] villeAffichee) {
        app.appeler(api::typesLocation, types -> {
            typeCombo.getItems().setAll(types);
            if (typeId != null) {
                types.stream().filter(t -> typeId.equals(t.id())).findFirst()
                        .ifPresent(typeCombo.getSelectionModel()::select);
            }
        });
        app.appeler(api::villes, villes -> {
            villeCombo.getItems().setAll(villes);
            if (villeId != null) {
                // La selection programmatique ne declenche pas toujours onAction :
                // on force le chargement des quartiers avec le quartier cible.
                villes.stream().filter(v -> villeId.equals(v.id())).findFirst()
                        .ifPresent(v -> {
                            villeCombo.getSelectionModel().select(v);
                            // setOnAction peut avoir charge les quartiers pendant la selection.
                            // Si ce n'est pas le cas, on les charge explicitement une seule fois.
                            if (villeAffichee[0] == null || !villeAffichee[0].equals(v.id())) {
                                villeAffichee[0] = v.id();
                                chargerQuartiers(villeCombo, quartierCombo, quartierCible[0]);
                            }
                        });
            } else if (!villes.isEmpty()) {
                villeCombo.getSelectionModel().select(0);
            }
        });
    }

    private void chargerQuartiers(ComboBox<VilleResponse> villeCombo,
                                  ComboBox<QuartierResponse> quartierCombo,
                                  Long quartierId) {
        VilleResponse v = villeCombo.getValue();
        if (v == null) {
            return;
        }
        app.appeler(() -> api.quartiers(v.id()), quartiers -> {
            quartierCombo.getItems().setAll(quartiers);
            if (quartierId != null) {
                quartiers.stream().filter(q -> quartierId.equals(q.id())).findFirst()
                        .ifPresent(quartierCombo.getSelectionModel()::select);
            } else if (!quartiers.isEmpty()) {
                quartierCombo.getSelectionModel().select(0);
            }
        });
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
        Label retour = new Label();

        ajoutBox.getChildren().addAll(urlF, principaleCb, ajouterBtn);
        VBox listePhotos = new VBox(8);
        ScrollPane scroll = new ScrollPane(listePhotos);
        scroll.setPrefHeight(300);
        scroll.setFitToWidth(true);

        Runnable[] rechargerPhotos = new Runnable[1];
        rechargerPhotos[0] = () -> {
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
                    delBtn.setOnAction(ev -> {
                        delBtn.setDisable(true);
                        app.appeler(() -> {
                            api.supprimerPhoto(annonceId, p.id());
                            return null;
                        },
                        ok -> {
                            // Rafraichir la liste des photos du dialogue ET la liste des annonces.
                            rechargerPhotos[0].run();
                            charger();
                            messageRetour(retour, "Photo supprimée.", true);
                        },
                        err -> {
                            delBtn.setDisable(false);
                            app.gererErreur(err);
                        });
                    });

                    ligne.getChildren().addAll(iv, urlLbl, badge, delBtn);
                    listePhotos.getChildren().add(ligne);
                }
            });
        };

        ajouterBtn.setOnAction(e -> {
            String u = urlF.getText() == null ? "" : urlF.getText().trim();
            if (u.isBlank()) {
                messageRetour(retour, "Renseignez l'URL de la photo avant de cliquer sur Ajouter.", false);
                return;
            }
            messageRetour(retour, "", false);
            ajouterBtn.setDisable(true);
            app.appeler(() -> api.ajouterPhoto(annonceId, new PhotoRequest(u, principaleCb.isSelected())),
                    ok -> {
                        urlF.clear();
                        principaleCb.setSelected(false);
                        rechargerPhotos[0].run();
                        charger();
                        ajouterBtn.setDisable(false);
                        messageRetour(retour, "Photo ajoutée.", true);
                    },
                    err -> {
                        app.gererErreur(err);
                        ajouterBtn.setDisable(false);
                        messageRetour(retour, "Échec de l'ajout de la photo.", false);
                    });
        });

        rechargerPhotos[0].run();
        contenu.getChildren().addAll(ajoutBox, retour, new Separator(), scroll);
        dialog.getDialogPane().setContent(contenu);
        dialog.showAndWait();
    }

    /** Retour visuel dans un dialogue : succes en vert, erreur en rouge. */
    private static void messageRetour(Label label, String texte, boolean succes) {
        label.setText(texte);
        label.setStyle(succes ? "-fx-text-fill: #15803d;" : "-fx-text-fill: #b91c1c;");
    }
}
