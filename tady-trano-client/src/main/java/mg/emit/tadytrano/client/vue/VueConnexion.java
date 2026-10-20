package mg.emit.tadytrano.client.vue;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import mg.emit.tadytrano.client.TadyTranoApp;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.dto.LoginRequest;
import mg.emit.tadytrano.client.dto.RegisterRequest;
import mg.emit.tadytrano.client.util.Travailleur;

/**
 * Ecran d'authentification avec mise en page professionnelle, formulaire lisible
 * et accès explicite à l'assistance.
 */
public class VueConnexion extends VBox {

    private final TadyTranoApp app;
    private final ApiClient api;

    private final RadioButton connexion = new RadioButton("Connexion");
    private final RadioButton inscription = new RadioButton("Créer un compte");
    private final TextField email = new TextField();
    private final PasswordField motDePasse = new PasswordField();
    private final Label labelNom = new Label("Nom");
    private final TextField nom = new TextField();
    private final Label labelPrenom = new Label("Prénom");
    private final TextField prenom = new TextField();
    private final Label labelRole = new Label("Type de compte");
    private final RadioButton roleLocataire = new RadioButton("Locataire");
    private final RadioButton roleProprio = new RadioButton("Propriétaire / Agence");
    private final Label titreFormulaire = new Label();
    private final Label sousTitreFormulaire = new Label();
    private final Label erreur = new Label();
    private final Button valider = new Button("Se connecter");
    private boolean authentificationEnCours = false;

    public VueConnexion(TadyTranoApp app, ApiClient api) {
        this.app = app;
        this.api = api;
        construire();
        basculerMode();
    }

    private void construire() {
        setSpacing(0);
        setPadding(new Insets(24));
        setAlignment(Pos.CENTER);
        setFillWidth(true);
        getStyleClass().add("auth-screen");

        VBox hero = new VBox(18);
        hero.getStyleClass().add("auth-hero");
        Label marque = new Label("TADY TRANO  /  IMMOBILIER");
        marque.getStyleClass().add("auth-eyebrow");

        Label promesse = new Label("Le logement qui vous correspond commence ici.");
        promesse.getStyleClass().add("auth-hero-title");
        promesse.setWrapText(true);
        promesse.setMaxWidth(330);

        Label description = new Label(
                "Explorez des annonces de location à Madagascar, comparez les quartiers "
                        + "et contactez les propriétaires depuis un seul espace.");
        description.getStyleClass().add("auth-hero-copy");
        description.setWrapText(true);
        description.setMaxWidth(330);

        Label avantage1 = new Label("✓  Recherche par ville et quartier");
        Label avantage2 = new Label("✓  Carte et tri par distance");
        Label avantage3 = new Label("✓  Favoris et contacts propriétaires");
        for (Label avantage : java.util.List.of(avantage1, avantage2, avantage3)) {
            avantage.getStyleClass().add("auth-hero-feature");
            avantage.setWrapText(true);
        }
        hero.getChildren().addAll(marque, promesse, description,
                avantage1, avantage2, avantage3);

        VBox formulaire = new VBox(14);
        formulaire.getStyleClass().add("auth-form");

        titreFormulaire.getStyleClass().add("auth-form-heading");
        sousTitreFormulaire.getStyleClass().add("auth-form-copy");
        sousTitreFormulaire.setWrapText(true);

        ToggleGroup groupe = new ToggleGroup();
        connexion.setToggleGroup(groupe);
        inscription.setToggleGroup(groupe);
        connexion.setSelected(true);
        connexion.setOnAction(e -> basculerMode());
        inscription.setOnAction(e -> basculerMode());

        HBox modes = new HBox(8, connexion, inscription);
        modes.setAlignment(Pos.CENTER_LEFT);
        modes.getStyleClass().add("auth-mode-bar");
        HBox.setHgrow(connexion, Priority.ALWAYS);
        HBox.setHgrow(inscription, Priority.ALWAYS);

        email.setPromptText("vous@exemple.com");
        email.setAccessibleText("Adresse e-mail");
        email.setMaxWidth(Double.MAX_VALUE);
        motDePasse.setPromptText("Votre mot de passe");
        motDePasse.setAccessibleText("Mot de passe");
        motDePasse.setMaxWidth(Double.MAX_VALUE);
        motDePasse.setOnAction(e -> envoyer());
        email.setOnAction(e -> motDePasse.requestFocus());

        nom.setPromptText("Votre nom");
        nom.setMaxWidth(Double.MAX_VALUE);
        prenom.setPromptText("Votre prénom");
        prenom.setMaxWidth(Double.MAX_VALUE);

        ToggleGroup groupeRole = new ToggleGroup();
        roleLocataire.setToggleGroup(groupeRole);
        roleProprio.setToggleGroup(groupeRole);
        roleLocataire.setSelected(true);
        HBox rolesBox = new HBox(12, roleLocataire, roleProprio);
        rolesBox.setAlignment(Pos.CENTER_LEFT);

        VBox champs = new VBox(10,
                champ("Adresse e-mail", email),
                champ("Mot de passe", motDePasse),
                champ(labelNom, nom),
                champ(labelPrenom, prenom),
                champ(labelRole, rolesBox));
        champs.setFillWidth(true);

        erreur.getStyleClass().add("error-text");
        erreur.setWrapText(true);
        erreur.setMinHeight(0);
        erreur.setVisible(false);
        erreur.setManaged(false);

        valider.getStyleClass().add("btn-primary-custom");
        valider.setMaxWidth(Double.MAX_VALUE);
        valider.setDefaultButton(true);
        valider.setOnAction(e -> envoyer());

        Button contact = new Button("Contacter le support / Signaler un problème");
        contact.getStyleClass().add("auth-help-button");
        contact.setMaxWidth(Double.MAX_VALUE);
        contact.setOnAction(e -> app.ouvrirContact());
        contact.setTooltip(new javafx.scene.control.Tooltip(
                "Ouvre la page officielle du projet pour contacter l'équipe ou signaler un bug."));

        Label note = new Label("Vos informations servent uniquement à votre compte et à vos annonces.");
        note.getStyleClass().add("muted");
        note.setWrapText(true);

        formulaire.getChildren().addAll(titreFormulaire, sousTitreFormulaire,
                modes, champs, erreur, valider, contact, note);

        HBox miseEnPage = new HBox(26, hero, formulaire);
        miseEnPage.setAlignment(Pos.CENTER);
        miseEnPage.setMaxWidth(960);
        miseEnPage.getStyleClass().add("auth-layout");
        getChildren().add(miseEnPage);
        VBox.setVgrow(miseEnPage, Priority.ALWAYS);
    }

    private VBox champ(String libelle, Node controle) {
        return champ(new Label(libelle), controle);
    }

    private VBox champ(Label libelle, Node controle) {
        libelle.getStyleClass().add("auth-field-label");
        libelle.setLabelFor(controle);
        VBox ligne = new VBox(6, libelle, controle);
        ligne.setFillWidth(true);
        return ligne;
    }

    /** Les champs d'identité et le type de compte ne sont visibles qu'à l'inscription. */
    private void basculerMode() {
        boolean pourInscription = inscription.isSelected();
        for (Node node : java.util.List.of(labelNom, nom, labelPrenom, prenom, labelRole,
                roleLocataire.getParent() == null ? roleLocataire : roleLocataire.getParent())) {
            node.setVisible(pourInscription);
            node.setManaged(pourInscription);
        }
        roleProprio.setVisible(pourInscription);
        roleProprio.setManaged(pourInscription);

        titreFormulaire.setText(pourInscription ? "Créer votre espace" : "Bon retour");
        sousTitreFormulaire.setText(pourInscription
                ? "Renseignez vos informations pour commencer."
                : "Connectez-vous pour retrouver vos recherches et vos favoris.");
        valider.setText(pourInscription ? "Créer mon compte" : "Se connecter");
        effacerErreur();
    }

    private void afficherMessageErreur(String message) {
        erreur.setText(message);
        erreur.setVisible(true);
        erreur.setManaged(true);
    }

    private void effacerErreur() {
        erreur.setText("");
        erreur.setVisible(false);
        erreur.setManaged(false);
    }

    private void envoyer() {
        if (authentificationEnCours) {
            return;
        }
        effacerErreur();
        String adresse = email.getText().trim();
        String secret = motDePasse.getText();
        if (adresse.isEmpty()) {
            afficherMessageErreur("L'adresse e-mail est obligatoire.");
            email.requestFocus();
            return;
        }
        if (!adresse.contains("@") || adresse.startsWith("@") || adresse.endsWith("@")) {
            afficherMessageErreur("Saisissez une adresse e-mail valide (ex. nom@domaine.mg).");
            email.requestFocus();
            return;
        }
        if (secret.isEmpty()) {
            afficherMessageErreur("Le mot de passe est obligatoire.");
            motDePasse.requestFocus();
            return;
        }
        if (inscription.isSelected()) {
            if (nom.getText().isBlank()) {
                afficherMessageErreur("Le nom est obligatoire.");
                nom.requestFocus();
                return;
            }
            if (prenom.getText().isBlank()) {
                afficherMessageErreur("Le prénom est obligatoire.");
                prenom.requestFocus();
                return;
            }
            String roleChoisi = roleProprio.isSelected() ? "PROPRIETAIRE" : "LOCATAIRE";
            RegisterRequest demande = new RegisterRequest(adresse, secret,
                    nom.getText().trim(), prenom.getText().trim(), roleChoisi);
            demarrerAuthentification();
            Travailleur.faire(() -> api.inscription(demande),
                    reponse -> {
                        terminerAuthentification();
                        app.apresAuthentification(reponse);
                    },
                    this::afficherErreur);
        } else {
            demarrerAuthentification();
            Travailleur.faire(() -> api.connexion(new LoginRequest(adresse, secret)),
                    reponse -> {
                        terminerAuthentification();
                        app.apresAuthentification(reponse);
                    },
                    this::afficherErreur);
        }
    }

    private void demarrerAuthentification() {
        authentificationEnCours = true;
        valider.setDisable(true);
        connexion.setDisable(true);
        inscription.setDisable(true);
    }

    private void terminerAuthentification() {
        authentificationEnCours = false;
        valider.setDisable(false);
        connexion.setDisable(false);
        inscription.setDisable(false);
    }

    private void afficherErreur(Exception e) {
        // Message inline uniquement : une modale en plus serait redondante.
        afficherMessageErreur(e.getMessage() == null || e.getMessage().isBlank()
                ? "Impossible de terminer l'authentification." : e.getMessage());
        terminerAuthentification();
    }
}
