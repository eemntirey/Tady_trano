package mg.emit.tadytrano.presentation.rest;

import tools.jackson.databind.ObjectMapper;
import mg.emit.tadytrano.application.dto.LoginRequest;
import mg.emit.tadytrano.application.dto.RegisterRequest;
import mg.emit.tadytrano.infrastructure.security.LoginAttemptService;
import mg.emit.tadytrano.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    private String emailUnique() {
        return "test-" + UUID.randomUUID() + "@tady.mg";
    }

    private String inscrireEtRecupererToken(String email, String motDePasse, Role role) throws Exception {
        RegisterRequest demande = new RegisterRequest(email, motDePasse, "Rakoto", "Jean", role);
        String json = mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demande)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(json).get("token").asText();
    }

    @Test
    void inscriptionValideCreeLeCompteEtRetourneUnJeton() throws Exception {
        RegisterRequest demande = new RegisterRequest(emailUnique(), "MotDePasse123", "Rakoto", "Jean", Role.LOCATAIRE);

        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demande)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.utilisateur.email").value(demande.email()))
                .andExpect(jsonPath("$.utilisateur.role").value("LOCATAIRE"));
    }

    @Test
    void inscriptionAvecEmailDejaUtiliseRepond409() throws Exception {
        String email = emailUnique();
        inscrireEtRecupererToken(email, "MotDePasse123", Role.LOCATAIRE);

        RegisterRequest doublon = new RegisterRequest(email, "MotDePasse123", "Rakoto", "Jean", Role.LOCATAIRE);
        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doublon)))
                .andExpect(status().isConflict());
    }

    @Test
    void inscriptionAvecEmailInvalideRepond400() throws Exception {
        RegisterRequest demande = new RegisterRequest("pas-un-email", "MotDePasse123", "Rakoto", "Jean", Role.LOCATAIRE);

        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demande)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inscriptionAvecRoleAdminEstRefusee() throws Exception {
        RegisterRequest demande = new RegisterRequest(emailUnique(), "MotDePasse123", "Admin", "Super", Role.ADMIN);

        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demande)))
                .andExpect(status().isForbidden());
    }

    @Test
    void connexionValideRetourneUnJeton() throws Exception {
        String email = emailUnique();
        inscrireEtRecupererToken(email, "MotDePasse123", Role.LOCATAIRE);

        LoginRequest demande = new LoginRequest(email, "MotDePasse123");
        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demande)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.utilisateur.email").value(email));
    }

    @Test
    void connexionAvecMotDePasseIncorrectRepond401() throws Exception {
        String email = emailUnique();
        inscrireEtRecupererToken(email, "MotDePasse123", Role.LOCATAIRE);

        LoginRequest demande = new LoginRequest(email, "MauvaisMotDePasse");
        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demande)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meSansJetonRepond401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mAvecJetonValideRetourneLeProfil() throws Exception {
        String email = emailUnique();
        String jeton = inscrireEtRecupererToken(email, "MotDePasse123", Role.PROPRIETAIRE);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("PROPRIETAIRE"));
    }

    @Test
    void jetonDeCompteDesactiveEstRefuse() throws Exception {
        String email = emailUnique();
        String jeton = inscrireEtRecupererToken(email, "MotDePasse123", Role.LOCATAIRE);
        jdbc.update("UPDATE utilisateur SET actif = FALSE WHERE email = ?", email);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meAvecJetonInvalideRepond401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer jeton-absurde"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- SEC-03 : limitation des tentatives de connexion ----------

    @Test
    void connexionReussieReinitialiseLeCompteurDEchecs() throws Exception {
        String email = emailUnique();
        inscrireEtRecupererToken(email, "MotDePasse123", Role.LOCATAIRE);

        // Deux echecs puis une succes : le compteur est remis a zero.
        for (int i = 0; i < 2; i++) {
            LoginRequest demande = new LoginRequest(email, "MauvaisMotDePasse");
            mockMvc.perform(post("/api/auth/connexion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(demande)))
                    .andExpect(status().isUnauthorized());
        }

        // Connexion reussie : reinitialise le compteur.
        LoginRequest demandeOk = new LoginRequest(email, "MotDePasse123");
        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demandeOk)))
                .andExpect(status().isOk());
    }

    @Test
    void cinqEchecsReelsDeConnexionDeclenchent429ALaTentativeSuivante() throws Exception {
        String email = emailUnique();
        inscrireEtRecupererToken(email, "MotDePasse123", Role.LOCATAIRE);

        // Cinq mauvais mots de passe enregistrent cinq échecs réels.
        for (int i = 0; i < LoginAttemptService.MAX_TENTATIVES; i++) {
            LoginRequest demande = new LoginRequest(email, "MauvaisMotDePasse");
            mockMvc.perform(post("/api/auth/connexion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(demande)))
                    .andExpect(status().isUnauthorized());
        }

        // La sixième tentative est bloquée avant l'authentification.
        LoginRequest sixiemeDemande = new LoginRequest(email, "MauvaisMotDePasse");
        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sixiemeDemande)))
                .andExpect(status().isTooManyRequests());
    }

}
