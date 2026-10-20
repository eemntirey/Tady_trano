package mg.emit.tadytrano.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.util.ArrayDeque;
import java.util.Deque;
import mg.emit.tadytrano.client.api.ApiClient;
import mg.emit.tadytrano.client.api.Session;
import mg.emit.tadytrano.client.dto.AuthResponse;
import mg.emit.tadytrano.client.ui.ThemeManager;
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
    private final ThemeManager themeManager = new ThemeManager();
    private final java.util.List<Button> boutonsNav = new java.util.ArrayList<>();
    private final Deque<Parent> historiqueVues = new ArrayDeque<>();
    private final BooleanProperty retourDisponible = new SimpleBooleanProperty(false);

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
        // Le libelle de la navigation annonce F7 : enregistrer effectivement le raccourci.
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F7) {
                ouvrir(new VueCarte(this, api));
                e.consume();
            }
        });
        themeManager.initialiser(scene);
        fenetre.setTitle("Tady Trano");
        fenetre.setScene(scene);
        fenetre.show();
    }

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    private HBox barreDeNavigation() {
        Button retour = new Button("Retour");
        retour.getStyleClass().add("nav-button");
        retour.setOnAction(e -> retour());
        retour.visibleProperty().bind(retourDisponible);
        retour.managedProperty().bind(retour.visibleProperty());

        Button recherche = new Button("Rechercher");
        recherche.setOnAction(e -> ouvrir(new VueRecherche(this, api)));

        Button carte = new Button("Carte (F7)");
        carte.setOnAction(e -> ouvrir(new VueCarte(this, api)));

        Button favoris = new Button("Favoris");
        favoris.setOnAction(e -> ouvrir(new VueFavoris(this, api)));

        Button mesAnnonces = new Button("Mes annonces");
        mesAnnonces.setOnAction(e -> ouvrirMesAnnonces());
        // Les routes /api/proprietaires exigent le role PROPRIETAIRE (meme les ADMIN sont refusees).
        mesAnnonces.visibleProperty().bind(session.proprietaireProperty());
        mesAnnonces.managedProperty().bind(mesAnnonces.visibleProperty());

        Button profil = new Button("Profil");
        profil.setOnAction(e -> ouvrir(new VueProfil(this, api)));

        // Visiteur sans session : retour explicite vers l'ecran de connexion.
        Button connexion = new Button("Connexion");
        connexion.setOnAction(e -> ouvrir(new VueConnexion(this, api)));
        connexion.visibleProperty().bind(session.connecteProperty().not());
        connexion.managedProperty().bind(connexion.visibleProperty());
        connexion.getStyleClass().addAll("nav-button", "nav-button-danger");

        Button admin = new Button("Moderation");
        admin.setOnAction(e -> ouvrir(new VueAdmin(this, api)));
        admin.visibleProperty().bind(session.adminProperty());

        Button deconnexion = new Button("Deconnexion");
        deconnexion.setOnAction(e -> {
            session.deconnecter();
            majBarre();
            effacerHistorique();
            afficherSansEmpiler(new VueConnexion(this, api));
        });
        deconnexion.visibleProperty().bind(session.connecteProperty());

        for (Button b : java.util.List.of(recherche, carte, favoris, mesAnnonces, profil, admin)) {
            b.getStyleClass().add("nav-button");
            boutonsNav.add(b);
        }
        admin.managedProperty().bind(admin.visibleProperty());
        deconnexion.getStyleClass().addAll("nav-button", "nav-button-danger");
        deconnexion.managedProperty().bind(deconnexion.visibleProperty());

        Button aide = new Button("Aide");
        aide.getStyleClass().add("nav-button");
        aide.setTooltip(new Tooltip("Contacter le support ou signaler un problème"));
        aide.setOnAction(e -> ouvrirContact());

        Button theme = new Button("Mode sombre");
        theme.getStyleClass().add("nav-button");
        theme.setOnAction(e -> {
            themeManager.basculerTheme();
            theme.setText(themeManager.estModeSombre() ? "Mode clair" : "Mode sombre");
        });

        Label marque = new Label("Tady Trano");
        marque.getStyleClass().add("brand-title");

        // Indicateur global : visible tant qu'un appel API est en vol
        // (Travailleur) — l'utilisateur sait que le bouton a bien reagi.
        ProgressIndicator chargement = new ProgressIndicator();
        chargement.setPrefSize(20, 20);
        chargement.setMaxSize(20, 20);
        chargement.setProgress(-1);
        chargement.visibleProperty().bind(Travailleur.occupeProperty());
        chargement.managedProperty().bind(chargement.visibleProperty());
        chargement.setTooltip(new Tooltip("Appel API en cours..."));

        utilisateur.getStyleClass().add("user-pill");
        utilisateur.visibleProperty().bind(utilisateur.textProperty().isNotEmpty());
        utilisateur.managedProperty().bind(utilisateur.visibleProperty());

        Region poussiere = new Region();
        HBox.setHgrow(poussiere, Priority.ALWAYS);
        HBox barre = new HBox(10, marque, retour, recherche, carte, favoris, mesAnnonces, profil, connexion,
                admin, aide, poussiere, chargement, utilisateur, theme, deconnexion);
        barre.getStyleClass().add("app-header");
        barre.setPrefHeight(48);
        return barre;
    }

    private void majBarre() {
        utilisateur.setText(session.estConnecte()
                ? session.email() + " (" + session.profil().role() + ")"
                : "");
    }

    /** Affiche une vue et mémorise l'écran précédent pour le bouton Retour global. */
    public void ouvrir(Parent vue) {
        if (vue == null) {
            return;
        }
        Parent courante = racine.getCenter();
        if (courante != null && courante != vue) {
            historiqueVues.push(courante);
        }
        afficherSansEmpiler(vue);
    }

    /**
     * Revient vers une vue déjà affichée sans créer une boucle dans l'historique.
     * Supprime la cible du sommet si elle a été mémorisée lors de l'ouverture du détail.
     */
    public void afficherSansHistorique(Parent vue) {
        if (vue == null) {
            return;
        }
        if (!historiqueVues.isEmpty() && historiqueVues.peek() == vue) {
            historiqueVues.pop();
        }
        afficherSansEmpiler(vue);
    }

    /** Retour vers la vue précédente, sans enregistrer la vue quittée à nouveau. */
    public void retour() {
        if (!historiqueVues.isEmpty()) {
            afficherSansEmpiler(historiqueVues.pop());
        } else {
            afficherSansEmpiler(new VueRecherche(this, api));
        }
    }

    private void afficherSansEmpiler(Parent vue) {
        racine.setCenter(vue);
        majBoutonActif(vue);
        retourDisponible.set(!historiqueVues.isEmpty());
    }

    private void effacerHistorique() {
        historiqueVues.clear();
        retourDisponible.set(false);
    }

    /** Ouvre la page officielle de support pour contacter l'équipe et signaler un défaut. */
    public void ouvrirContact() {
        try {
            getHostServices().showDocument("https://github.com/eemntirey/Tady_trano/issues/new");
        } catch (RuntimeException ex) {
            Affichage.erreur(new IllegalStateException(
                    "Impossible d'ouvrir le navigateur. Le support est disponible à l'adresse "
                            + "https://github.com/eemntirey/Tady_trano/issues/new", ex));
        }
    }

    private void majBoutonActif(javafx.scene.Parent vue) {
        String cible = vue instanceof VueRecherche ? "Rechercher"
                : vue instanceof VueCarte ? "Carte (F7)"
                : vue instanceof VueFavoris ? "Favoris"
                : vue instanceof VueMesAnnonces ? "Mes annonces"
                : vue instanceof VueProfil ? "Profil"
                : vue instanceof VueAdmin ? "Moderation"
                : "";
        for (Button b : boutonsNav) {
            b.getStyleClass().remove("active");
            if (b.getText().equals(cible)) {
                b.getStyleClass().add("active");
            }
        }
    }

    /** Passe a la vue suivante apres connexion/inscription. */
    public void apresAuthentification(AuthResponse reponse) {
        session.connecter(reponse.token(), reponse.utilisateur());
        majBarre();
        effacerHistorique();
        afficherSansEmpiler(new VueRecherche(this, api));
    }

    /** Rouvre la recherche (apres une erreur de session par exemple). */
    public void retourConnexion() {
        session.deconnecter();
        majBarre();
        effacerHistorique();
        afficherSansEmpiler(new VueConnexion(this, api));
    }

    /** Vrai si un compte est connecte. */
    public boolean estConnecte() {
        return session.estConnecte();
    }

    /** Profil de l'utilisateur connecte, ou null. */
    public mg.emit.tadytrano.client.dto.ProfilResponse profilCourant() {
        return session.profil();
    }

    /** Met a jour le profil courant (roles inclus) apres un rafraichissement /api/auth/me. */
    public void majProfilCourant(mg.emit.tadytrano.client.dto.ProfilResponse profil) {
        session.majProfil(profil);
        majBarre();
    }

    /** Vrai si le compte courant a le role strict PROPRIETAIRE. */
    public boolean estProprietaire() {
        return session.estProprietaire();
    }

    /** Vrai si le compte courant a le role ADMIN. */
    public boolean estAdmin() {
        return session.estAdmin();
    }

    /** Ouvre le detail d'une annonce depuis n'importe quelle liste. */
    public void ouvrirDetail(long annonceId, Runnable retour) {
        ouvrir(new VueDetail(this, api, annonceId, retour));
    }

    /** Espace proprietaire : refuse poliment les roles non autorises au lieu du 403 serveur. */
    public void ouvrirMesAnnonces() {
        if (!estProprietaire()) {
            Affichage.erreur(new RuntimeException(estConnecte()
                    ? "L'espace « Mes annonces » est réservé aux comptes Propriétaire."
                    : "Connectez-vous avec un compte Propriétaire pour gérer vos annonces."));
            return;
        }
        ouvrir(new VueMesAnnonces(this, api));
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
