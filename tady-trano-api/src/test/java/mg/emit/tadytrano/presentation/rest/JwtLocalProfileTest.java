package mg.emit.tadytrano.presentation.rest;

import tools.jackson.databind.ObjectMapper;
import mg.emit.tadytrano.application.dto.LoginRequest;
import mg.emit.tadytrano.application.dto.RegisterRequest;
import mg.emit.tadytrano.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Vérifie l'authentification avec la configuration locale et ses données de démonstration. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class JwtLocalProfileTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void inscriptionSansConfigurationExterneRetourneUnJeton() throws Exception {
        String email = "local-" + UUID.randomUUID() + "@tady.mg";
        RegisterRequest demande = new RegisterRequest(
                email, "MotDePasse123", "Rakoto", "Jean", Role.LOCATAIRE);

        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demande)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.utilisateur.email").value(email));
    }

    @Test
    void connexionAuCompteAdminDeDemoFonctionne() throws Exception {
        LoginRequest demande = new LoginRequest("admin@tady.test", "admin12345");

        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demande)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.utilisateur.email").value("admin@tady.test"))
                .andExpect(jsonPath("$.utilisateur.role").value("ADMIN"));
    }
}
