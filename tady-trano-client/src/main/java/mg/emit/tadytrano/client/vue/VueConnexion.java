package mg.emit.tadytrano.client.vue;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mg.emit.tadytrano.client.TadyTranoApp;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.dto.LoginRequest;
import mg.emit.tadytrano.client.dto.RegisterRequest;
import mg.emit.tadytrano.client.util.Affichage;
import mg.emit.tadytrano.client.util.Travailleur;

/** Ecran de connexion / inscription. */
public class VueConnexion extends VBox {

    private final TadyTranoApp app;
    private final ApiClient api;

    private final RadioButton connexion = new RadioButton("Connexion");
    private final RadioButton inscription = new RadioButton("Inscription");
    private final TextField email = new TextField();
    private final PasswordField motDePasse = new PasswordField();
    private final Label labelNom = new Label("Nom");
    private final TextField nom = new TextField();
    private final Label labelPrenom = new Label("Prénom");
    private final TextField prenom = new TextField();
    private final Label labelRole = new Label("Rôle");
    private final RadioButton roleLocataire = new RadioButton("Locataire");
    private final RadioButton roleProprio = new RadioButton("Propriétaire / Agence");
    private final Label erreur = new Label();
    private final Button valider = new Button("Valider");

    public VueConnexion(TadyTranoApp app, ApiClient api) {
        this.app = app;
        this.api = api;
        construire();
        basculerMode();
    }

    private void construire() {
        setSpacing(12);
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);
        setPrefWidth(420);
        getStyleClass().add("auth-pane");

        Label titre = new Label("Tady Trano");
        titre.getStyleClass().add("auth-title");
        Label sousTitre = new Label("Trouvez votre maison à Madagascar");
        sousTitre.getStyleClass().add("muted");

        ToggleGroup groupe = new ToggleGroup();
        connexion.setToggleGroup(groupe);
        inscription.setToggleGroup(groupe);
        connexion.setSelected(true);
        connexion.setOnAction(e -> basculerMode());
        inscription.setOnAction(e -> basculerMode());
        HBox modes = new HBox(20, connexion, inscription);
        modes.setAlignment(Pos.CENTER);

        ToggleGroup groupeRole = new ToggleGroup();
        roleLocataire.setToggleGroup(groupeRole);
        roleProprio.setToggleGroup(groupeRole);
        roleLocataire.setSelected(true);
        HBox rolesBox = new HBox(12, roleLocataire, roleProprio);

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(10);
        grille.add(new Label("Email"), 0, 0);
        grille.add(email, 1, 0);
        grille.add(new Label("Mot de passe"), 0, 1);
        grille.add(motDePasse, 1, 1);
        grille.add(labelNom, 0, 2);
        grille.add(nom, 1, 2);
        grille.add(labelPrenom, 0, 3);
        grille.add(prenom, 1, 3);
        grille.add(labelRole, 0, 4);
        grille.add(rolesBox, 1, 4);
        email.setPrefWidth(220);

        erreur.getStyleClass().add("error-text");
        erreur.setWrapText(true);
        valider.getStyleClass().add("btn-primary-custom");
        valider.setMaxWidth(Double.MAX_VALUE);
        valider.setDefaultButton(true);
        valider.setOnAction(e -> envoyer());

        getChildren().addAll(titre, sousTitre, modes, grille, erreur, valider);
    }

    /** Les champs nom/prenom et role ne servent qu'a l'inscription. */
    private void basculerMode() {
        boolean pourInscription = inscription.isSelected();
        labelNom.setVisible(pourInscription);
        labelNom.setManaged(pourInscription);
        nom.setVisible(pourInscription);
        nom.setManaged(pourInscription);

        labelPrenom.setVisible(pourInscription);
        labelPrenom.setManaged(pourInscription);
        prenom.setVisible(pourInscription);
        prenom.setManaged(pourInscription);

        labelRole.setVisible(pourInscription);
        labelRole.setManaged(pourInscription);
        roleLocataire.setVisible(pourInscription);
        roleLocataire.setManaged(pourInscription);
        roleProprio.setVisible(pourInscription);
        roleProprio.setManaged(pourInscription);

        valider.setText(pourInscription ? "Créer mon compte" : "Se connecter");
        erreur.setText("");
    }

    private void envoyer() {
        erreur.setText("");
        String adresse = email.getText().trim();
        String secret = motDePasse.getText();
        if (adresse.isEmpty() || secret.isEmpty()) {
            erreur.setText("Email et mot de passe obligatoires");
            return;
        }
        if (inscription.isSelected()) {
            if (nom.getText().isBlank() || prenom.getText().isBlank()) {
                erreur.setText("Nom et prénom obligatoires");
                return;
            }
            String roleChoisi = roleProprio.isSelected() ? "PROPRIETAIRE" : "LOCATAIRE";
            RegisterRequest demande = new RegisterRequest(adresse, secret,
                    nom.getText().trim(), prenom.getText().trim(), roleChoisi);
            Travailleur.faire(() -> api.inscription(demande),
                    reponse -> app.apresAuthentification(reponse),
                    this::afficherErreur);
        } else {
            Travailleur.faire(() -> api.connexion(new LoginRequest(adresse, secret)),
                    reponse -> app.apresAuthentification(reponse),
                    this::afficherErreur);
        }
    }

    private void afficherErreur(Exception e) {
        Affichage.erreur(e);
        erreur.setText(e.getMessage());
    }
}
