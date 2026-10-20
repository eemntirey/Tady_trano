package mg.emit.tadytrano.presentation.rest;

import tools.jackson.databind.ObjectMapper;
import mg.emit.tadytrano.application.dto.RegisterRequest;
import mg.emit.tadytrano.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Le placeholder JWT_SECRET doit primer sur la valeur locale de repli. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = "JWT_SECRET=local-test-secret-override-at-least-32-bytes")
class JwtLocalProfileSecretOverrideTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String secretCharge;

    @Test
    void secretJwtExternePrendPrioriteEtPermetDeSignerUnJeton() throws Exception {
        assertEquals("local-test-secret-override-at-least-32-bytes", secretCharge);

        String email = "local-override-" + UUID.randomUUID() + "@tady.mg";
        RegisterRequest demande = new RegisterRequest(
                email, "MotDePasse123", "Rakoto", "Jean", Role.LOCATAIRE);

        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demande)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
