package mg.emit.tadytrano.presentation.rest;

import mg.emit.tadytrano.application.dto.LoginRequest;
import mg.emit.tadytrano.application.dto.RegisterRequest;
import mg.emit.tadytrano.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Helpers partages des tests d'integration annonce : nettoyage de la base H2
 * partagee, inscription / connexion, referentiel et payloads JSON.
 * Concreteur : AnnonceControllerTest, AnnonceRechercheCasLimitesTest,
 * AnnonceRechercheFluiditeTest.
 */
abstract class AnnonceTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbc;

    /** La base H2 est partagee par tous les tests : on la vide avant chaque test. */
    @BeforeEach
    void nettoyerBase() {
        jdbc.update("DELETE FROM favori");
        jdbc.update("DELETE FROM photo");
        jdbc.update("DELETE FROM annonce");
        jdbc.update("DELETE FROM proprietaire");
        jdbc.update("DELETE FROM utilisateur");
        jdbc.update("DELETE FROM quartier");
        jdbc.update("DELETE FROM ville");
        jdbc.update("DELETE FROM type_location");
    }

    protected String emailUnique() {
        return "test-" + UUID.randomUUID() + "@tady.mg";
    }

    protected String inscrire(String email, Role role) throws Exception {
        RegisterRequest demande = new RegisterRequest(email, "MotDePasse123", "Rakoto", "Jean", role);
        String json = mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demande)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(json).get("token").asText();
    }

    /** Connexion avec le mot de passe standard des tests, renvoie le jeton. */
    protected String connexion(String email) throws Exception {
        LoginRequest demande = new LoginRequest(email, "MotDePasse123");
        String json = mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demande)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(json).get("token").asText();
    }

    /** Cree un compte proprietaire + son profil, et renvoie le jeton. */
    protected String proprietaireConnecte() throws Exception {
        String email = emailUnique();
        String jeton = inscrire(email, Role.PROPRIETAIRE);

        Long quartierId = villeEtQuartier("Antananarivo", "Analakely");

        String profil = """
                {"type":"PARTICULIER","telephone":"0341234567","quartierId":%d}""".formatted(quartierId);
        mockMvc.perform(post("/api/proprietaires")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profil))
                .andExpect(status().isCreated());

        return jeton;
    }

    /** Insert une ville et son quartier (referentiel vide en memoire), renvoie l'id du quartier. */
    protected Long villeEtQuartier(String ville, String quartier) {
        jdbc.update("INSERT INTO ville (nom) VALUES (?)", ville);
        Long villeId = jdbc.queryForObject("SELECT id FROM ville WHERE nom = ?", Long.class, ville);
        jdbc.update("INSERT INTO quartier (nom, ville_id) VALUES (?, ?)", quartier, villeId);
        return jdbc.queryForObject("SELECT id FROM quartier WHERE nom = ?", Long.class, quartier);
    }

    protected Long typeLocationId() {
        jdbc.update("INSERT INTO type_location (code, libelle) VALUES ('HOTEL', 'Hotel')");
        return jdbc.queryForObject("SELECT id FROM type_location WHERE code = 'HOTEL'", Long.class);
    }

    protected String annonceJson(Long typeLocationId, Long quartierId) {
        return """
                {
                  "typeLocationId": %d,
                  "quartierId": %d,
                  "titre": "Studio centre-ville",
                  "description": "Proche universite",
                  "prix": 450000,
                  "surface": 25,
                  "nbPieces": 1,
                  "latitude": -18.91,
                  "longitude": 47.52
                }""".formatted(typeLocationId, quartierId);
    }
}
