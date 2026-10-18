package mg.emit.tadytrano.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.api.Session;

/**
 * Ecran principal : recherche d'annonces (F3), connexion (F1), favoris (F8).
 * CDC sections 5.2, 6, 7.
 */
public class RechercheController {

    @FXML private ComboBox<String> comboType;
    @FXML private TextField champPrixMin;
    @FXML private TextField champPrixMax;
    @FXML private Spinner<Integer> spinnerPiecesMin;
    @FXML private ComboBox<String> comboQuartier;
    @FXML private Label labelStatutConnexion;

    private ApiClient api;
    private Session session = new Session();

    public void setApi(ApiClient api) {
        this.api = api;
    }

    @FXML
    public void initialize() {
        spinnerPiecesMin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        labelStatutConnexion.setText(session.estConnecte() ? "Connecte : " + session.email() : "Non connecte");
    }

    @FXML
    public void rechercher() {
        System.out.println("Recherche lancee avec filtres : type=" + comboType.getValue()
                + ", prixMin=" + champPrixMin.getText() + ", prixMax=" + champPrixMax.getText()
                + ", pieces=" + spinnerPiecesMin.getValue());
        // Appel a api.rechercher(...) a implementer avec pagination continue (F5)
    }
}
