package mg.emit.tadytrano.presentation.rest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cas limites de la recherche publique (analyse 3.6) : tranche de prix incoherente
 * (min superieur a max), position incomplete (lat sans lng / lng sans lat) et
 * valeurs negatives de prix / pieces. La recherche doit toujours repondre 200 avec
 * un resultat coherent, jamais une erreur 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnnonceRechercheCasLimitesTest extends AnnonceTestSupport {

    /** Cree une annonce active de reference (450 000 Ar, 1 piece). */
    private void uneAnnonce() throws Exception {
        String jeton = proprietaireConnecte();
        Long typeId = typeLocationId();
        Long quartierId = jdbc.queryForObject("SELECT id FROM quartier LIMIT 1", Long.class);
        mockMvc.perform(post("/api/annonces")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annonceJson(typeId, quartierId)))
                .andExpect(status().isCreated());
    }

    /** Deux annonces actives, la plus ancienne repoussee de 2 h en arriere. */
    private void deuxAnnoncesDatees() throws Exception {
        String jeton = proprietaireConnecte();
        Long typeId = typeLocationId();
        Long quartierId = jdbc.queryForObject("SELECT id FROM quartier LIMIT 1", Long.class);

        for (String titre : new String[]{"Plus ancienne", "Plus recente"}) {
            mockMvc.perform(post("/api/annonces")
                            .header("Authorization", "Bearer " + jeton)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "typeLocationId": %d,
                                      "quartierId": %d,
                                      "titre": "%s",
                                      "description": "Cas limite recherche",
                                      "prix": 450000,
                                      "surface": 25,
                                      "nbPieces": 1,
                                      "latitude": -18.91,
                                      "longitude": 47.52
                                    }""".formatted(typeId, quartierId, titre)))
                    .andExpect(status().isCreated());
        }
        jdbc.update("UPDATE annonce SET date_publication = TIMESTAMPADD(HOUR, -2, CURRENT_TIMESTAMP)"
                + " WHERE titre = ?", "Plus ancienne");
    }

    @Test
    void prixMinSuperieurAPrixMaxRenvoieUneListeVideSansErreur() throws Exception {
        uneAnnonce();

        mockMvc.perform(get("/api/annonces")
                        .param("prixMin", "500000")
                        .param("prixMax", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.contenu").isEmpty());
    }

    @Test
    void latitudeSansLongitudeRepond400() throws Exception {
        deuxAnnoncesDatees();

        // Position incomplete : lat sans lng est refuse (BUG-04)
        mockMvc.perform(get("/api/annonces").param("lat", "-18.91"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void longitudeSansLatitudeRepond400() throws Exception {
        deuxAnnoncesDatees();

        mockMvc.perform(get("/api/annonces").param("lng", "47.52"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void valeursNegativesDePrixEtDePiecesNeProvoquentPasDErreur() throws Exception {
        uneAnnonce();

        // prixMin negatif : n'exclut aucune annonce (toutes ont un prix positif)
        mockMvc.perform(get("/api/annonces").param("prixMin", "-1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // prixMax negatif : aucune annonce ne peut correspondre
        mockMvc.perform(get("/api/annonces").param("prixMax", "-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // nbPiecesMin negatif : n'exclut aucune annonce
        mockMvc.perform(get("/api/annonces").param("nbPiecesMin", "-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // nbPiecesMax negatif : aucune annonce ne peut correspondre
        mockMvc.perform(get("/api/annonces").param("nbPiecesMax", "-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void coordonneesNegativesHemisphereSudTrieesParDistance() throws Exception {
        // Madagascar est dans l'hemisphere sud : latitude negative doit fonctionner
        deuxAnnoncesDatees();

        mockMvc.perform(get("/api/annonces")
                        .param("lat", "-18.91")
                        .param("lng", "47.52"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.contenu[0].distanceKm").value(0.0));
    }

    // ---------- BUG-04 : validation des parametres lat / lng ----------

    @Test
    void latitudeHorsBornesRepond400() throws Exception {
        mockMvc.perform(get("/api/annonces")
                        .param("lat", "91")
                        .param("lng", "47.52"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void latitudeMoinsHuitanteDixRepond400() throws Exception {
        mockMvc.perform(get("/api/annonces")
                        .param("lat", "-91")
                        .param("lng", "47.52"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void longitudeHorsBornesRepond400() throws Exception {
        mockMvc.perform(get("/api/annonces")
                        .param("lat", "-18.91")
                        .param("lng", "181"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void longitudeMoinsHuitanteDixRepond400() throws Exception {
        mockMvc.perform(get("/api/annonces")
                        .param("lat", "-18.91")
                        .param("lng", "-181"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void latitudeNonFinieRepond400() throws Exception {
        // Double.parseDouble("NaN") donne NaN, Spring le lie en tant que Double.
        mockMvc.perform(get("/api/annonces")
                        .param("lat", "NaN")
                        .param("lng", "47.52"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void longitudeNonFinieRepond400() throws Exception {
        mockMvc.perform(get("/api/annonces")
                        .param("lat", "-18.91")
                        .param("lng", "Infinity"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void coordonneesValidesDansBornesRepond200() throws Exception {
        deuxAnnoncesDatees();

        mockMvc.perform(get("/api/annonces")
                        .param("lat", "-18.91")
                        .param("lng", "47.52"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void limiteSuperieureLatitudeAcceptee() throws Exception {
        mockMvc.perform(get("/api/annonces")
                        .param("lat", "90")
                        .param("lng", "0"))
                .andExpect(status().isOk());
    }

    @Test
    void limiteSuperieureLongitudeAcceptee() throws Exception {
        mockMvc.perform(get("/api/annonces")
                        .param("lat", "0")
                        .param("lng", "180"))
                .andExpect(status().isOk());
    }

    // ---------- TEST-01 / BUG-01 : contrat DTO avec coordonnees ----------

    @Test
    void resumeAnnonceContientLatitudeEtLongitude() throws Exception {
        String jeton = proprietaireConnecte();
        Long typeId = typeLocationId();
        Long quartierId = jdbc.queryForObject("SELECT id FROM quartier LIMIT 1", Long.class);

        mockMvc.perform(post("/api/annonces")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annonceJson(typeId, quartierId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/annonces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].latitude").value(-18.91))
                .andExpect(jsonPath("$.contenu[0].longitude").value(47.52));
    }
}
