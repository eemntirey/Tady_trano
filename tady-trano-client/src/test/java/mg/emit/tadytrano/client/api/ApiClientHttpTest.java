package mg.emit.tadytrano.client.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import mg.emit.tadytrano.client.dto.ProfilResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comportement réel du client HTTP face à un serveur : pagination des favoris
 * (au-delà de la première page), en-tête Authorization, mapping des erreurs
 * HTTP 401/403 vers ErreurApi.
 */
class ApiClientHttpTest {

    private HttpServer serveur;
    private String base;

    /** Réponses JSON : clé « chemin » (tout statut) ou « chemin#page » (pagination). */
    private final Map<String, Reponse> reponses = new ConcurrentHashMap<>();
    /** Chemins effectivement appelés, dans l'ordre. */
    private final List<String> chemins = Collections.synchronizedList(new ArrayList<>());
    private final AtomicReference<String> enTeteAutorization = new AtomicReference<>();

    private record Reponse(int statut, String corps) {
    }

    private Session session;

    @BeforeEach
    void demarrerServeur() throws IOException {
        serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serveur.createContext("/", this::traiter);
        serveur.start();
        base = "http://127.0.0.1:" + serveur.getAddress().getPort();
        session = new Session();
        reponses.clear();
        chemins.clear();
        enTeteAutorization.set(null);
    }

    @AfterEach
    void arreterServeur() {
        serveur.stop(0);
    }

    private void traiter(HttpExchange echange) throws IOException {
        String chemin = echange.getRequestURI().getPath();
        String question = echange.getRequestURI().getQuery();
        chemins.add(chemin + (question == null ? "" : "?" + question));
        enTeteAutorization.set(echange.getRequestHeaders().getFirst("Authorization"));

        int page = parametre(question, "page", 0);
        Reponse reponse = reponses.get(chemin + "#" + page);
        if (reponse == null) {
            reponse = reponses.get(chemin);
        }
        if (reponse == null) {
            reponse = new Reponse(404, "{\"detail\":\"Introuvable\"}");
        }
        byte[] octets = reponse.corps().getBytes(StandardCharsets.UTF_8);
        echange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        echange.sendResponseHeaders(reponse.statut(), octets.length);
        try (OutputStream sort = echange.getResponseBody()) {
            sort.write(octets);
        }
    }

    private static int parametre(String question, String nom, int defaut) {
        if (question == null) {
            return defaut;
        }
        for (String paire : question.split("&")) {
            String[] morceaux = paire.split("=", 2);
            if (morceaux.length == 2 && nom.equals(morceaux[0])) {
                return Integer.parseInt(morceaux[1]);
            }
        }
        return defaut;
    }

    private ApiClient client() {
        return new ApiClient(base, session);
    }

    private static String pageFavoris(String annonceIds, int page, int total) {
        StringBuilder contenu = new StringBuilder();
        for (String id : annonceIds.split(",")) {
            if (!id.isBlank()) {
                if (contenu.length() > 0) {
                    contenu.append(',');
                }
                contenu.append("{\"annonceId\":").append(id)
                        .append(",\"dateAjout\":\"2026-10-15T08:00:00\",\"annonce\":null}");
            }
        }
        return "{\"contenu\":[" + contenu + "],\"page\":" + page
                + ",\"taille\":100,\"totalElements\":" + (annonceIds.isBlank() ? 0 : annonceIds.split(",").length)
                + ",\"totalPages\":" + total + "}";
    }

    // ------------------------------------------------------------------
    // Favoris : endpoint de presence dedie (D5)
    // ------------------------------------------------------------------

    @Test
    void estEnFavoriInterrogeLEndpointDedie() {
        reponses.put("/api/favoris/404", new Reponse(200, "true"));

        assertTrue(client().estEnFavori(404L));
        // Une seule requete sur /api/favoris/{id} : pas de parcours des pages.
        assertEquals(List.of("/api/favoris/404"), List.copyOf(chemins));
    }

    @Test
    void estEnFavoriRetourneFaux() {
        reponses.put("/api/favoris/404", new Reponse(200, "false"));

        assertFalse(client().estEnFavori(404L));
        assertEquals(List.of("/api/favoris/404"), List.copyOf(chemins));
    }

    @Test
    void estEnFavoriSansJetonProduitSessionExpiree() {
        reponses.put("/api/favoris/9", new Reponse(401, "{\"message\":\"Jeton invalide\"}"));

        ErreurApi erreur = assertThrows(ErreurApi.class, () -> client().estEnFavori(9L));
        assertEquals(401, erreur.statut());
        assertTrue(erreur.estNonConnecte());
    }

    // ------------------------------------------------------------------
    // Profil : rafraichissement via /api/auth/me (D8)
    // ------------------------------------------------------------------

    @Test
    void moiInterrogeAuthMeEtExposeLesRolesAJour() {
        reponses.put("/api/auth/me", new Reponse(200,
                "{\"id\":7,\"email\":\"a@b.mg\",\"nom\":\"N\",\"prenom\":\"P\",\"role\":\"ADMIN\"}"));
        session.connecter("jeton-secret", new ProfilResponse(7L, "a@b.mg", "N", "P", "LOCATAIRE"));

        ProfilResponse profil = client().moi();

        assertEquals("/api/auth/me", chemins.get(0));
        assertEquals("ADMIN", profil.role());
        assertEquals("Bearer jeton-secret", enTeteAutorization.get());
        // La session n'est pas touchee par la lecture seule du profil.
        assertFalse(session.estAdmin());
        session.majProfil(profil);
        assertTrue(session.estAdmin());
    }

    // ------------------------------------------------------------------
    // Erreurs HTTP
    // ------------------------------------------------------------------

    @Test
    void erreur403ExposeLeDetailServeurEtLeStatut() {
        reponses.put("/api/proprietaires/mes-annonces",
                new Reponse(403, "{\"detail\":\"Acces refuse : role proprietaire requis\"}"));

        ErreurApi erreur = assertThrows(ErreurApi.class, () -> client().mesAnnonces(0, 10));
        assertEquals(403, erreur.statut());
        assertTrue(erreur.getMessage().contains("Acces refuse"));
        assertFalse(erreur.estNonConnecte());
    }

    @Test
    void erreur401EstReconnueCommeSessionExpiree() {
        reponses.put("/api/favoris", new Reponse(401, "{\"message\":\"Jeton invalide\"}"));

        ErreurApi erreur = assertThrows(ErreurApi.class, () -> client().mesFavoris(0, 10));
        assertEquals(401, erreur.statut());
        assertTrue(erreur.estNonConnecte());
        assertEquals("Jeton invalide", erreur.getMessage());
    }

    @Test
    void erreurSansCorpsTombeSurLeCodeHttp() {
        reponses.put("/api/favoris", new Reponse(500, ""));

        ErreurApi erreur = assertThrows(ErreurApi.class, () -> client().mesFavoris(0, 10));
        assertEquals(500, erreur.statut());
        assertEquals("Erreur 500", erreur.getMessage());
    }

    // ------------------------------------------------------------------
    // En-tete Authorization
    // ------------------------------------------------------------------

    @Test
    void enTeteBearerEnvoyeSiConnecte() {
        reponses.put("/api/favoris", new Reponse(200, pageFavoris("", 0, 0)));
        session.connecter("jeton-secret", new ProfilResponse(1L, "a@b.mg", "N", "P", "LOCATAIRE"));

        client().mesFavoris(0, 10);

        assertEquals("Bearer jeton-secret", enTeteAutorization.get());
    }

    @Test
    void aucunEnTeteAuthSansSession() {
        reponses.put("/api/favoris", new Reponse(200, pageFavoris("", 0, 0)));

        client().mesFavoris(0, 10);

        assertNull(enTeteAutorization.get());
    }

    @Test
    void apiInjoignableProduitErreurApiZero() {
        ApiClient horsLigne = new ApiClient("http://127.0.0.1:1", session);

        ErreurApi erreur = assertThrows(ErreurApi.class, () -> horsLigne.villes());
        assertEquals(0, erreur.statut());
        assertTrue(erreur.getMessage().contains("API injoignable"));
    }
}
