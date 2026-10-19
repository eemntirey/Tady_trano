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
    void latitudeSansLongitudeRetombeSurLeTriParDate() throws Exception {
        deuxAnnoncesDatees();

        // Position incomplete : pas de tri par distance, repli sur datePublication DESC
        mockMvc.perform(get("/api/annonces").param("lat", "-18.91"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.contenu[0].titre").value("Plus recente"))
                .andExpect(jsonPath("$.contenu[1].titre").value("Plus ancienne"));
    }

    @Test
    void longitudeSansLatitudeRetombeSurLeTriParDate() throws Exception {
        deuxAnnoncesDatees();

        mockMvc.perform(get("/api/annonces").param("lng", "47.52"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.contenu[0].titre").value("Plus recente"))
                .andExpect(jsonPath("$.contenu[1].titre").value("Plus ancienne"));
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
}
