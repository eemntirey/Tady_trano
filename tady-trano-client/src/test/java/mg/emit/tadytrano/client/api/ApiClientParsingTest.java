package mg.emit.tadytrano.client.api;

import mg.emit.tadytrano.client.dto.AnnonceDetailResponse;
import mg.emit.tadytrano.client.dto.AnnonceResumeResponse;
import mg.emit.tadytrano.client.dto.AuthResponse;
import mg.emit.tadytrano.client.dto.FavoriResponse;
import mg.emit.tadytrano.client.dto.PageResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie que le client parse correctement les payloads reels de l'API
 * (Jackson 3, records Java, LocalDateTime ISO-8601).
 */
class ApiClientParsingTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void parseAuthResponse() {
        String json = """
                {"token":"eyJhbGciOiJIUzI1NiJ9.abc","type":"Bearer","expirationEnSecondes":86400,
                 "utilisateur":{"id":1,"email":"a@b.mg","nom":"Rakoto","prenom":"Jean","role":"LOCATAIRE"}}
                """;
        AuthResponse reponse = ApiClient.analyser(json, AuthResponse.class);
        assertEquals("eyJhbGciOiJIUzI1NiJ9.abc", reponse.token());
        assertEquals(86400, reponse.expirationEnSecondes());
        assertEquals("LOCATAIRE", reponse.utilisateur().role());
    }

    @Test
    void parsePageAnnoncesAvecParametrage() {
        String json = """
                {"contenu":[
                   {"id":7,"titre":"Villa Analakely","prix":450000.00,"surface":120,"nbPieces":3,
                    "statut":"ACTIVE","datePublication":"2026-10-01T10:15:00",
                    "typeLocation":{"id":2,"code":"ETAGE","libelle":"Etage"},
                    "quartier":{"id":5,"nom":"Analakely","ville":{"id":1,"nom":"Antananarivo"},
                                "latitude":-18.91,"longitude":47.52},
                    "urlPhotoPrincipale":"https://img/x.jpg","distanceKm":12.4}],
                 "page":0,"taille":15,"totalElements":1,"totalPages":1}
                """;
        JavaType type = MAPPER.getTypeFactory()
                .constructParametricType(PageResponse.class, AnnonceResumeResponse.class);
        PageResponse<AnnonceResumeResponse> page = MAPPER.readValue(json, type);

        assertEquals(1, page.contenu().size());
        AnnonceResumeResponse annonce = page.contenu().get(0);
        assertEquals(new BigDecimal("450000.00"), annonce.prix());
        assertEquals("Antananarivo", annonce.quartier().ville().nom());
        assertEquals(12.4, annonce.distanceKm());
        assertEquals(1, page.totalPages());
    }

    @Test
    void parseDetailSansContactPourAnonyme() {
        String json = """
                {"id":7,"titre":"Villa","description":"Bel endroit","prix":450000.00,
                 "surface":120,"nbPieces":3,"statut":"ACTIVE","datePublication":"2026-10-01T10:15:00",
                 "latitude":-18.91,"longitude":47.52,
                 "typeLocation":{"id":2,"code":"ETAGE","libelle":"Etage"},
                 "quartier":{"id":5,"nom":"Analakely","ville":{"id":1,"nom":"Antananarivo"},
                             "latitude":null,"longitude":null},
                 "photos":[{"id":1,"url":"https://img/1.jpg","principale":true,"ordre":1}],
                 "contact":null}
                """;
        AnnonceDetailResponse detail = ApiClient.analyser(json, AnnonceDetailResponse.class);
        assertNull(detail.contact(), "contact doit rester null pour un anonyme");
        assertNotNull(detail.photos());
        assertEquals(1, detail.photos().size());
        assertTrue(detail.photos().get(0).principale());
    }

    @Test
    void parseFavoriAvecResumeEmboite() {
        String json = """
                {"annonceId":7,"dateAjout":"2026-10-15T08:00:00",
                 "annonce":{"id":7,"titre":"Villa","prix":450000.00,"surface":null,"nbPieces":3,
                            "statut":"ACTIVE","datePublication":"2026-10-01T10:15:00",
                            "typeLocation":null,"quartier":null,
                            "urlPhotoPrincipale":null,"distanceKm":null}}
                """;
        JavaType type = MAPPER.getTypeFactory()
                .constructParametricType(PageResponse.class, FavoriResponse.class);
        PageResponse<FavoriResponse> page = MAPPER.readValue(
                "{\"contenu\":[" + json + "],\"page\":0,\"taille\":15,"
                        + "\"totalElements\":1,\"totalPages\":1}", type);

        FavoriResponse favori = page.contenu().get(0);
        assertEquals(7L, favori.annonceId());
        assertEquals("Villa", favori.annonce().titre());
    }

    @Test
    void serialiseDemandeCreation() {
        var demande = new mg.emit.tadytrano.client.dto.AnnonceRequest(
                2L, 5L, "Studio Ivato", null, new BigDecimal("250000"), 40, 1, null, null);
        String json = ApiClient.serialiser(demande);
        assertTrue(json.contains("\"titre\":\"Studio Ivato\""));
        assertTrue(json.contains("\"typeLocationId\":2"));
        assertTrue(json.contains("\"quartierId\":5"));
        assertNull(ApiClient.analyser(json, mg.emit.tadytrano.client.dto.AnnonceRequest.class)
                .description());
    }

    @Test
    void listeNonVideAttendue() {
        // garde-fou : la methode getList doit pouvoir reconstruire une liste de records
        String json = "[{\"id\":1,\"nom\":\"Antananarivo\"},{\"id\":2,\"nom\":\"Toamasina\"}]";
        List<mg.emit.tadytrano.client.dto.VilleResponse> villes = MAPPER.readValue(json,
                MAPPER.getTypeFactory().constructCollectionType(List.class,
                        mg.emit.tadytrano.client.dto.VilleResponse.class));
        assertEquals(2, villes.size());
        assertEquals("Toamasina", villes.get(1).nom());
    }
}
