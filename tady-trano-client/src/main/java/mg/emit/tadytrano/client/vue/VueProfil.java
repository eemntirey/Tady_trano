package mg.emit.tadytrano.client.vue;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mg.emit.tadytrano.client.TadyTranoApp;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.dto.ProfilResponse;
import mg.emit.tadytrano.client.dto.ProprietaireRequest;
import mg.emit.tadytrano.client.dto.ProprietaireResponse;
import mg.emit.tadytrano.client.dto.QuartierResponse;
import mg.emit.tadytrano.client.dto.VilleResponse;
import mg.emit.tadytrano.client.util.Affichage;

/**
 * Vue du profil utilisateur et coordonnees proprietaire / agence (CDC 4.2).
 */
public class VueProfil extends VBox {

    private final TadyTranoApp app;
    private final ApiClient api;

    private final Label emailVal = new Label();
    private final Label nomVal = new Label();
    private final Label prenomVal = new Label();
    private final Label roleVal = new Label();

    // Section proprietaire
    private final VBox sectionProprio = new VBox(12);
    private final RadioButton typeParticulier = new RadioButton("Particulier");
    private final RadioButton typeAgence = new RadioButton("Agence Immobilière");
    private final Label labelAgence = new Label("Nom de l'agence :");
    private final TextField champNomAgence = new TextField();
    private final TextField champTelephone = new TextField();
    private final TextField champEmailContact = new TextField();
    private final ComboBox<VilleResponse> comboVille = new ComboBox<>();
    private final ComboBox<QuartierResponse> comboQuartier = new ComboBox<>();
    private final Button btnEnregistrerProprio = new Button("Enregistrer les coordonnées");
    private final Label statutProprio = new Label();

    private boolean profilExiste = false;

    public VueProfil(TadyTranoApp app, ApiClient api) {
        this.app = app;
        this.api = api;
        construire();
        charger();
    }

    private void construire() {
        setSpacing(20);
        setPadding(new Insets(25));
        setPrefWidth(600);

        Label titre = new Label("Mon Compte & Profil");
        titre.setFont(Font.font("System", FontWeight.BOLD, 22));

        // Informations de base
        VBox infosCompte = new VBox(10);
        infosCompte.setPadding(new Insets(15));
        infosCompte.getStyleClass().add("detail-box");

        Label titreCompte = new Label("Informations personnelles");
        titreCompte.setFont(Font.font("System", FontWeight.BOLD, 16));

        GridPane gridCompte = new GridPane();
        gridCompte.setHgap(15);
        gridCompte.setVgap(8);
        gridCompte.add(new Label("Email :"), 0, 0);
        gridCompte.add(emailVal, 1, 0);
        gridCompte.add(new Label("Nom :"), 0, 1);
        gridCompte.add(nomVal, 1, 1);
        gridCompte.add(new Label("Prénom :"), 0, 2);
        gridCompte.add(prenomVal, 1, 2);
        gridCompte.add(new Label("Rôle sur la plateforme :"), 0, 3);
        gridCompte.add(roleVal, 1, 3);

        emailVal.getStyleClass().add("strong");
        roleVal.getStyleClass().add("role-value");

        infosCompte.getChildren().addAll(titreCompte, gridCompte);

        // Section Coordonnees proprietaire (F4.2)
        sectionProprio.setPadding(new Insets(15));
        sectionProprio.getStyleClass().add("detail-box");

        Label titreProprio = new Label("Coordonnées Propriétaire / Agence (visibles sur vos annonces)");
        titreProprio.setFont(Font.font("System", FontWeight.BOLD, 16));

        ToggleGroup groupeType = new ToggleGroup();
        typeParticulier.setToggleGroup(groupeType);
        typeAgence.setToggleGroup(groupeType);
        typeParticulier.setSelected(true);
        typeParticulier.setOnAction(e -> basculerType());
        typeAgence.setOnAction(e -> basculerType());
        HBox boxType = new HBox(15, typeParticulier, typeAgence);

        GridPane gridProprio = new GridPane();
        gridProprio.setHgap(15);
        gridProprio.setVgap(10);
        gridProprio.add(new Label("Type de profil :"), 0, 0);
        gridProprio.add(boxType, 1, 0);
        gridProprio.add(labelAgence, 0, 1);
        gridProprio.add(champNomAgence, 1, 1);
        gridProprio.add(new Label("Téléphone :"), 0, 2);
        gridProprio.add(champTelephone, 1, 2);
        gridProprio.add(new Label("Email de contact :"), 0, 3);
        gridProprio.add(champEmailContact, 1, 3);
        gridProprio.add(new Label("Ville :"), 0, 4);
        gridProprio.add(comboVille, 1, 4);
        gridProprio.add(new Label("Quartier :"), 0, 5);
        gridProprio.add(comboQuartier, 1, 5);

        btnEnregistrerProprio.getStyleClass().add("btn-success-custom");
        btnEnregistrerProprio.setOnAction(e -> enregistrerProprio());

        statutProprio.getStyleClass().setAll("label", "success-text");

        comboVille.setOnAction(e -> chargerQuartiers());

        sectionProprio.getChildren().addAll(titreProprio, gridProprio, btnEnregistrerProprio, statutProprio);

        ScrollPane scroll = new ScrollPane(new VBox(15, titre, new Separator(), infosCompte, sectionProprio));
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparent");

        getChildren().add(scroll);
        basculerType();
    }

    private void basculerType() {
        boolean estAgence = typeAgence.isSelected();
        labelAgence.setVisible(estAgence);
        labelAgence.setManaged(estAgence);
        champNomAgence.setVisible(estAgence);
        champNomAgence.setManaged(estAgence);
    }

    private void charger() {
        if (!app.estConnecte()) {
            return;
        }

        // Villes
        app.appeler(api::villes, villes -> {
            comboVille.getItems().setAll(villes);
        });

        // Infos utilisateur depuis session ou profil
        ProfilResponse profil = null;
        try {
            java.lang.reflect.Field field = app.getClass().getDeclaredField("session");
            field.setAccessible(true);
            mg.emit.tadytrano.client.api.Session sess = (mg.emit.tadytrano.client.api.Session) field.get(app);
            profil = sess.profil();
        } catch (Exception ignore) {
        }

        if (profil != null) {
            emailVal.setText(profil.email());
            nomVal.setText(profil.nom());
            prenomVal.setText(profil.prenom());
            roleVal.setText(profil.role());

            boolean estProprio = "PROPRIETAIRE".equals(profil.role()) || "ADMIN".equals(profil.role());
            sectionProprio.setVisible(estProprio);
            sectionProprio.setManaged(estProprio);

            if (estProprio) {
                // Tenter de charger le profil proprietaire
                app.appeler(api::profilProprietaire, this::remplirProprio);
            }
        }
    }

    private void remplirProprio(ProprietaireResponse p) {
        if (p == null) return;
        profilExiste = true;
        if ("AGENCE".equals(p.type())) {
            typeAgence.setSelected(true);
        } else {
            typeParticulier.setSelected(true);
        }
        basculerType();
        champNomAgence.setText(p.nomAgence() != null ? p.nomAgence() : "");
        champTelephone.setText(p.telephone() != null ? p.telephone() : "");
        champEmailContact.setText(p.emailContact() != null ? p.emailContact() : "");

        if (p.quartier() != null && p.quartier().ville() != null) {
            for (VilleResponse v : comboVille.getItems()) {
                if (v.id().equals(p.quartier().ville().id())) {
                    comboVille.getSelectionModel().select(v);
                    chargerQuartiers();
                    break;
                }
            }
        }
    }

    private void chargerQuartiers() {
        VilleResponse v = comboVille.getValue();
        if (v != null) {
            app.appeler(() -> api.quartiers(v.id()), qList -> {
                comboQuartier.getItems().setAll(qList);
                if (!qList.isEmpty()) {
                    comboQuartier.getSelectionModel().select(0);
                }
            });
        }
    }

    private void enregistrerProprio() {
        statutProprio.setText("");
        String telephone = champTelephone.getText().trim();
        if (telephone.isBlank()) {
            statutProprio.getStyleClass().setAll("label", "error-text");
            statutProprio.setText("Le téléphone de contact est obligatoire.");
            return;
        }

        String type = typeAgence.isSelected() ? "AGENCE" : "PARTICULIER";
        String agence = typeAgence.isSelected() ? champNomAgence.getText().trim() : null;
        String emailContact = champEmailContact.getText().trim().isBlank() ? null : champEmailContact.getText().trim();
        Long quartierId = comboQuartier.getValue() != null ? comboQuartier.getValue().id() : null;

        ProprietaireRequest req = new ProprietaireRequest(type, agence, telephone, emailContact, quartierId);

        if (!profilExiste) {
            app.appeler(() -> api.creerProfilProprietaire(req), resp -> {
                profilExiste = true;
                statutProprio.getStyleClass().setAll("label", "success-text");
                statutProprio.setText("Profil propriétaire créé avec succès !");
            });
        } else {
            app.appeler(() -> api.modifierProfilProprietaire(req), resp -> {
                statutProprio.getStyleClass().setAll("label", "success-text");
                statutProprio.setText("Profil propriétaire mis à jour !");
            });
        }
    }
}
