package mg.emit.tadytrano.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.api.Session;
import mg.emit.tadytrano.client.dto.AuthResponse;
import mg.emit.tadytrano.client.util.Affichage;
import mg.emit.tadytrano.client.util.Travailleur;
import mg.emit.tadytrano.client.vue.VueAdmin;
import mg.emit.tadytrano.client.vue.VueCarte;
import mg.emit.tadytrano.client.vue.VueConnexion;
import mg.emit.tadytrano.client.vue.VueDetail;
import mg.emit.tadytrano.client.vue.VueFavoris;
import mg.emit.tadytrano.client.vue.VueMesAnnonces;
import mg.emit.tadytrano.client.vue.VueProfil;
import mg.emit.tadytrano.client.vue.VueRecherche;

/**
 * Client desktop JavaFX de Tady Trano : recherche publique, favoris,
 * espace proprietaire et moderation admin, au-dessus de l'API Spring Boot.
 */
public class TadyTranoApp extends Application {

    private final Session session = new Session();
    private ApiClient api;
    private BorderPane racine;
    private Label utilisateur = new Label();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage fenetre) {
        // URL de l'API : -Dtady.api.url, puis variable TADY_API_URL, puis defaut local
        String base = System.getProperty("tady.api.url",
                System.getenv().getOrDefault("TADY_API_URL", "http://localhost:8081"));
        api = new ApiClient(base, session);

        racine = new BorderPane();
        racine.setTop(barreDeNavigation());
        racine.setCenter(new VueConnexion(this, api));

        Scene scene = new Scene(racine, 1100, 750);
        fenetre.setTitle("Tady Trano");
        fenetre.setScene(scene);
        fenetre.show();
    }

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    private HBox barreDeNavigation() {
        Button recherche = new Button("Rechercher");
        recherche.setOnAction(e -> ouvrir(new VueRecherche(this, api)));

        Button carte = new Button("Carte (F7)");
        carte.setOnAction(e -> ouvrir(new VueCarte(this, api)));

        Button favoris = new Button("Favoris");
        favoris.setOnAction(e -> ouvrir(new VueFavoris(this, api)));

        Button mesAnnonces = new Button("Mes annonces");
        mesAnnonces.setOnAction(e -> ouvrir(new VueMesAnnonces(this, api)));

        Button profil = new Button("Profil");
        profil.setOnAction(e -> ouvrir(new VueProfil(this, api)));

        Button admin = new Button("Moderation");
        admin.setOnAction(e -> ouvrir(new VueAdmin(this, api)));
        admin.visibleProperty().bind(session.adminProperty());

        Button deconnexion = new Button("Deconnexion");
        deconnexion.setOnAction(e -> {
            session.deconnecter();
            majBarre();
            ouvrir(new VueConnexion(this, api));
        });
        deconnexion.visibleProperty().bind(session.connecteProperty());

        utilisateur.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");

        Region poussiere = new Region();
        HBox.setHgrow(poussiere, Priority.ALWAYS);
        HBox barre = new HBox(10, recherche, carte, favoris, mesAnnonces, profil, admin, poussiere, utilisateur, deconnexion);
        barre.setStyle("-fx-padding: 10; -fx-background-color: #2c3e50;");
        barre.setPrefHeight(48);
        return barre;
    }

    private void majBarre() {
        utilisateur.setText(session.estConnecte()
                ? session.email() + " (" + session.profil().role() + ")"
                : "");
    }

    /** Affiche une vue dans la zone centrale. */
    public void ouvrir(javafx.scene.Parent vue) {
        racine.setCenter(vue);
    }

    /** Passe a la vue suivante apres connexion/inscription. */
    public void apresAuthentification(AuthResponse reponse) {
        session.connecter(reponse.token(), reponse.utilisateur());
        majBarre();
        ouvrir(new VueRecherche(this, api));
    }

    /** Rouvre la recherche (apres une erreur de session par exemple). */
    public void retourConnexion() {
        session.deconnecter();
        majBarre();
        ouvrir(new VueConnexion(this, api));
    }

    /** Vrai si un compte est connecte. */
    public boolean estConnecte() {
        return session.estConnecte();
    }

    /** Ouvre le detail d'une annonce depuis n'importe quelle liste. */
    public void ouvrirDetail(long annonceId, Runnable retour) {
        ouvrir(new VueDetail(this, api, annonceId, retour));
    }

    /** Tache API avec erreur affichee automatiquement. */
    public <T> void appeler(java.util.concurrent.Callable<T> tache,
                            java.util.function.Consumer<T> succes) {
        Travailleur.faire(tache, succes, this::gererErreur);
    }

    /** Tache API avec gestion explicite de l'erreur (hors cas 401 traite par l'appelant). */
    public <T> void appeler(java.util.concurrent.Callable<T> tache,
                            java.util.function.Consumer<T> succes,
                            java.util.function.Consumer<Exception> echec) {
        Travailleur.faire(tache, succes, echec);
    }

    /** Erreur 401 : la session est expiree, on renvoie a la connexion. */
    public void gererErreur(Exception e) {
        Affichage.erreur(e);
        if (e instanceof mg.emit.tadytrano.client.api.ErreurApi apiErreur
                && apiErreur.estNonConnecte()) {
            retourConnexion();
        }
    }
}
