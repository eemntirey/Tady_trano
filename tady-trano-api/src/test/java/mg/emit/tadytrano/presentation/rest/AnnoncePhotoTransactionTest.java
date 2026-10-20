package mg.emit.tadytrano.presentation.rest;

import mg.emit.tadytrano.application.dto.PhotoRequest;
import mg.emit.tadytrano.application.exception.ApiException;
import mg.emit.tadytrano.application.service.AnnonceService;
import mg.emit.tadytrano.domain.model.Role;
import mg.emit.tadytrano.domain.repository.PhotoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration des photos : le verrou SQL doit couvrir le comptage,
 * l'ajout et le commit, et ne doit pas créer d'etat en memoire pour un ID absent.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnnoncePhotoTransactionTest extends AnnonceTestSupport {

    @Autowired
    private AnnonceService annonceService;

    @Autowired
    private PhotoRepository photoRepository;

    record AnnonceFixture(String email, Long annonceId) {}

    @Test
    void identifiantsInexistantsSontRefusesSansMapDeVerrousPersistante() throws Exception {
        Long typeId = typeLocationId();
        AnnonceFixture annonce = creerAnnonce(emailUnique(), typeId);

        for (int i = 0; i < 100; i++) {
            Long idInexistant = Long.MAX_VALUE - i;
            ApiException erreur = assertThrows(ApiException.class,
                    () -> annonceService.ajouterPhoto(annonce.email(), idInexistant,
                            new PhotoRequest("https://cdn.tady.mg/inexistante.jpg", false)));
            assertEquals(HttpStatus.FORBIDDEN, erreur.getStatut());
        }
        assertEquals(0L, photoRepository.countByAnnonceId(annonce.annonceId()));
        assertFalse(Arrays.stream(AnnonceService.class.getDeclaredFields())
                        .anyMatch(champ -> champ.getName().equals("verrous")),
                "AnnonceService ne doit plus conserver une map de verrous par ID.");
    }

    @Test
    void annonceDUnAutreProprietaireNePeutPasRecevoirDePhoto() throws Exception {
        Long typeId = typeLocationId();
        AnnonceFixture annonceA = creerAnnonce(emailUnique(), typeId);
        AnnonceFixture proprietaireB = creerAnnonce(emailUnique(), typeId);

        ApiException erreur = assertThrows(ApiException.class,
                () -> annonceService.ajouterPhoto(proprietaireB.email(), annonceA.annonceId(),
                        new PhotoRequest("https://cdn.tady.mg/non-autorisee.jpg", false)));

        assertEquals(HttpStatus.FORBIDDEN, erreur.getStatut());
        assertEquals(0L, photoRepository.countByAnnonceId(annonceA.annonceId()));
    }

    private AnnonceFixture creerAnnonce(String email, Long typeId) throws Exception {
        String jeton = inscrire(email, Role.PROPRIETAIRE);
        String suffixe = UUID.randomUUID().toString().substring(0, 8);
        Long quartierId = villeEtQuartier("Ville-" + suffixe, "Quartier-" + suffixe);

        String profil = """
                {"type":"PARTICULIER","telephone":"0341234567","quartierId":%d}
                """.formatted(quartierId);
        mockMvc.perform(post("/api/proprietaires")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profil))
                .andExpect(status().isCreated());

        String creation = mockMvc.perform(post("/api/annonces")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annonceJson(typeId, quartierId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Long annonceId = objectMapper.readTree(creation).get("id").asLong();
        return new AnnonceFixture(email, annonceId);
    }

}
