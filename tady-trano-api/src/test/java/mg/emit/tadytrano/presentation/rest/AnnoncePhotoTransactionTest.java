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
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
@TestPropertySource(properties = "spring.datasource.hikari.transaction-isolation=TRANSACTION_SERIALIZABLE")
class AnnoncePhotoTransactionTest extends AnnonceTestSupport {

    @Autowired
    private AnnonceService annonceService;

    @Autowired
    private PhotoRepository photoRepository;

    record AnnonceFixture(String email, Long annonceId) {}

    @Test
    void deuxAjoutsSimultanesSurNeufPhotosNeDepassentPasLaLimiteDeDix() throws Exception {
        Long typeId = typeLocationId();
        AnnonceFixture annonce = creerAnnonce(emailUnique(), typeId);
        ajouterPhotosExistantes(annonce.annonceId(), 9);

        CyclicBarrier departCommun = new CyclicBarrier(2);
        ExecutorService executeur = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> premier = executeur.submit(() -> ajouterEnParallele(
                    departCommun, annonce.email(), annonce.annonceId(), "https://cdn.tady.mg/concurrente-a.jpg"));
            Future<Boolean> second = executeur.submit(() -> ajouterEnParallele(
                    departCommun, annonce.email(), annonce.annonceId(), "https://cdn.tady.mg/concurrente-b.jpg"));

            boolean premierReussi = premier.get(15, TimeUnit.SECONDS);
            boolean secondReussi = second.get(15, TimeUnit.SECONDS);
            assertTrue(premierReussi ^ secondReussi,
                    "Un seul ajout doit réussir lorsque l'annonce contient déjà neuf photos.");
            assertEquals(10L, photoRepository.countByAnnonceId(annonce.annonceId()),
                    "Le total final ne doit jamais dépasser dix photos.");
        } finally {
            executeur.shutdownNow();
        }
    }

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

    private boolean ajouterEnParallele(CyclicBarrier departCommun,
                                       String email,
                                       Long annonceId,
                                       String url) throws Exception {
        departCommun.await(10, TimeUnit.SECONDS);
        try {
            annonceService.ajouterPhoto(email, annonceId, new PhotoRequest(url, false));
            return true;
        } catch (ApiException erreur) {
            assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatut(),
                    "Le refus concurrent doit venir de la limite de dix photos.");
            return false;
        }
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

    private void ajouterPhotosExistantes(Long annonceId, int nombre) {
        for (int i = 1; i <= nombre; i++) {
            jdbc.update("""
                    INSERT INTO photo (annonce_id, url, principale, ordre)
                    VALUES (?, ?, ?, ?)
                    """, annonceId, "https://cdn.tady.mg/existante-" + i + ".jpg", i == 1, i);
        }
    }
}
