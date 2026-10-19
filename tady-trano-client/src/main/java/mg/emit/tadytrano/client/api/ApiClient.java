package mg.emit.tadytrano.client.api;

import mg.emit.tadytrano.client.dto.AnnonceDetailResponse;
import mg.emit.tadytrano.client.dto.AnnonceRequest;
import mg.emit.tadytrano.client.dto.AnnonceResumeResponse;
import mg.emit.tadytrano.client.dto.AuthResponse;
import mg.emit.tadytrano.client.dto.FavoriResponse;
import mg.emit.tadytrano.client.dto.LoginRequest;
import mg.emit.tadytrano.client.dto.PageResponse;
import mg.emit.tadytrano.client.dto.PhotoRequest;
import mg.emit.tadytrano.client.dto.PhotoResponse;
import mg.emit.tadytrano.client.dto.ProfilResponse;
import mg.emit.tadytrano.client.dto.ProprietaireRequest;
import mg.emit.tadytrano.client.dto.ProprietaireResponse;
import mg.emit.tadytrano.client.dto.QuartierResponse;
import mg.emit.tadytrano.client.dto.RegisterRequest;
import mg.emit.tadytrano.client.dto.StatutAnnonceRequest;
import mg.emit.tadytrano.client.dto.TypeLocationResponse;
import mg.emit.tadytrano.client.dto.VilleResponse;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client HTTP de l'API Tady Trano : java.net.http (JDK) + Jackson 3.
 * Attache le jeton JWT de la session a chaque requete.
 */
public class ApiClient {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final String base;
    private final Session session;

    public ApiClient(String base, Session session) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        this.session = session;
    }

    // ------------------------------------------------------------------
    // Auth
    // ------------------------------------------------------------------

    public AuthResponse connexion(LoginRequest demande) {
        return post("/api/auth/connexion", demande, AuthResponse.class);
    }

    public AuthResponse inscription(RegisterRequest demande) {
        return post("/api/auth/inscription", demande, AuthResponse.class);
    }

    // ------------------------------------------------------------------
    // Referentiel
    // ------------------------------------------------------------------

    public List<VilleResponse> villes() {
        return getList("/api/villes", VilleResponse.class);
    }

    public List<QuartierResponse> quartiers(Long villeId) {
        return getList("/api/villes/" + villeId + "/quartiers", QuartierResponse.class);
    }

    public List<TypeLocationResponse> typesLocation() {
        return getList("/api/types-location", TypeLocationResponse.class);
    }

    // ------------------------------------------------------------------
    // Annonces
    // ------------------------------------------------------------------

    public PageResponse<AnnonceResumeResponse> rechercher(Map<String, String> filtres, int page, int taille) {
        Map<String, String> params = new LinkedHashMap<>(filtres);
        params.put("page", String.valueOf(page));
        params.put("taille", String.valueOf(taille));
        return getPagin("/api/annonces?" + encoder(params), AnnonceResumeResponse.class);
    }

    public AnnonceDetailResponse detail(Long id) {
        return get("/api/annonces/" + id, AnnonceDetailResponse.class);
    }

    public AnnonceDetailResponse creerAnnonce(AnnonceRequest demande) {
        return post("/api/annonces", demande, AnnonceDetailResponse.class);
    }

    public AnnonceDetailResponse modifierAnnonce(Long id, AnnonceRequest demande) {
        return put("/api/annonces/" + id, demande, AnnonceDetailResponse.class);
    }

    public void changerStatutAnnonce(Long id, StatutAnnonceRequest demande) {
        patch("/api/annonces/" + id + "/statut", demande);
    }

    public void supprimerAnnonce(Long id) {
        delete("/api/annonces/" + id);
    }

    public List<PhotoResponse> photos(Long annonceId) {
        return getList("/api/annonces/" + annonceId + "/photos", PhotoResponse.class);
    }

    public PhotoResponse ajouterPhoto(Long annonceId, PhotoRequest demande) {
        return post("/api/annonces/" + annonceId + "/photos", demande, PhotoResponse.class);
    }

    public void supprimerPhoto(Long annonceId, Long photoId) {
        delete("/api/annonces/" + annonceId + "/photos/" + photoId);
    }

    public PageResponse<AnnonceResumeResponse> mesAnnonces(int page, int taille) {
        return getPagin("/api/proprietaires/mes-annonces?page=" + page + "&taille=" + taille,
                AnnonceResumeResponse.class);
    }

    // ------------------------------------------------------------------
    // Favoris
    // ------------------------------------------------------------------

    public PageResponse<FavoriResponse> mesFavoris(int page, int taille) {
        return getPagin("/api/favoris?page=" + page + "&taille=" + taille, FavoriResponse.class);
    }

    public void ajouterFavori(Long annonceId) {
        request("POST", "/api/favoris/" + annonceId, null);
    }

    public void retirerFavori(Long annonceId) {
        request("DELETE", "/api/favoris/" + annonceId, null);
    }

    // ------------------------------------------------------------------
    // Profil proprietaire
    // ------------------------------------------------------------------

    public ProprietaireResponse profilProprietaire() {
        return get("/api/proprietaires/moi", ProprietaireResponse.class);
    }

    public ProprietaireResponse creerProfilProprietaire(ProprietaireRequest demande) {
        return post("/api/proprietaires", demande, ProprietaireResponse.class);
    }

    public ProprietaireResponse modifierProfilProprietaire(ProprietaireRequest demande) {
        return put("/api/proprietaires/moi", demande, ProprietaireResponse.class);
    }

    // ------------------------------------------------------------------
    // Administration
    // ------------------------------------------------------------------

    public PageResponse<AnnonceResumeResponse> toutesLesAnnonces(String statut, int page, int taille) {
        Map<String, String> params = new LinkedHashMap<>();
        if (statut != null && !statut.isBlank()) {
            params.put("statut", statut);
        }
        params.put("page", String.valueOf(page));
        params.put("taille", String.valueOf(taille));
        return getPagin("/api/admin/annonces?" + encoder(params), AnnonceResumeResponse.class);
    }

    public void modererAnnonce(Long id, StatutAnnonceRequest demande) {
        patch("/api/admin/annonces/" + id + "/statut", demande);
    }

    // ------------------------------------------------------------------
    // Prive : verbs HTTP + serialization
    // ------------------------------------------------------------------

    public static String serialiser(Object valeur) {
        return MAPPER.writeValueAsString(valeur);
    }

    public static <T> T analyser(String json, Class<T> type) {
        return MAPPER.readValue(json, type);
    }

    private <T> T get(String chemin, Class<T> type) {
        return analyser(request("GET", chemin, null), type);
    }

    @SuppressWarnings("unchecked")
    private <T> PageResponse<T> getPagin(String chemin, Class<T> type) {
        JavaType pageTypee = MAPPER.getTypeFactory().constructParametricType(PageResponse.class, type);
        return MAPPER.readValue(request("GET", chemin, null), pageTypee);
    }

    private <T> List<T> getList(String chemin, Class<T> type) {
        JavaType liste = MAPPER.getTypeFactory().constructCollectionType(List.class, type);
        return MAPPER.readValue(request("GET", chemin, null), liste);
    }

    private <T> T post(String chemin, Object corps, Class<T> type) {
        return analyser(request("POST", chemin, corps), type);
    }

    private <T> T put(String chemin, Object corps, Class<T> type) {
        return analyser(request("PUT", chemin, corps), type);
    }

    private void patch(String chemin, Object corps) {
        request("PATCH", chemin, corps);
    }

    private void delete(String chemin) {
        request("DELETE", chemin, null);
    }

    /** Envoie la requete et renvoie le corps JSON (vide = chaine vide). */
    private String request(String methode, String chemin, Object corps) {
        HttpRequest.BodyPublisher publisher = corps == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(serialiser(corps), StandardCharsets.UTF_8);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(base + chemin))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .method(methode, publisher);
        if (session != null && session.estConnecte()) {
            builder.header("Authorization", "Bearer " + session.jeton());
        }

        HttpResponse<String> reponse;
        try {
            reponse = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ErreurApi(0, "API injoignable : " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErreurApi(0, "Requete interrompue");
        }

        int statut = reponse.statusCode();
        String corpsReponse = reponse.body() == null ? "" : reponse.body();
        if (statut >= 400) {
            throw new ErreurApi(statut, messageDerreur(corpsReponse, statut));
        }
        return corpsReponse;
    }

    static String messageDerreur(String json, int statut) {
        if (json == null || json.isBlank()) {
            return "Erreur " + statut;
        }
        try {
            var noeud = MAPPER.readTree(json);
            // Pour une erreur de validation, preferer le detail du champ au detail generique.
            for (String cle : List.of("erreurs", "errors")) {
                var erreurs = noeud.get(cle);
                if (erreurs != null && erreurs.isArray() && erreurs.size() > 0) {
                    var premiere = erreurs.get(0);
                    if (!premiere.isNull()) {
                        if (premiere.isTextual()) return premiere.asText();
                        var detailErreur = premiere.get("defaultMessage");
                        if (detailErreur == null) detailErreur = premiere.get("message");
                        if (detailErreur != null && !detailErreur.isNull()) return detailErreur.asText();
                        return premiere.toString();
                    }
                }
            }
            for (String cle : List.of("detail", "message", "title")) {
                var valeur = noeud.get(cle);
                if (valeur != null && !valeur.isNull() && !valeur.asText().isBlank()) {
                    return valeur.asText();
                }
            }
        } catch (RuntimeException ignore) {
            // corps non JSON : on retombe sur le code HTTP
        }
        return "Erreur " + statut;
    }

    private static String encoder(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
