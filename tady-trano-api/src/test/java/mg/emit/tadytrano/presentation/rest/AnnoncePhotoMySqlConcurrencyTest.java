package mg.emit.tadytrano.presentation.rest;

import mg.emit.tadytrano.application.dto.PhotoRequest;
import mg.emit.tadytrano.application.exception.ApiException;
import mg.emit.tadytrano.application.service.AnnonceService;
import mg.emit.tadytrano.domain.model.Role;
import mg.emit.tadytrano.domain.repository.PhotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de concurrence exécuté contre MySQL réel par l'étape mysql-migration de la CI.
 * Pour les exécutions Maven ordinaires, l'activation exige -Dtady.test.mysql=true.
 */
@EnabledIfSystemProperty(named = "tady.test.mysql", matches = "true")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=${PHOTO_DB_URL}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.datasource.username=${DB_USER}",
        "spring.datasource.password=${DB_PASSWORD}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class AnnoncePhotoMySqlConcurrencyTest extends AnnonceTestSupport {

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
        assertEquals(9L, photoRepository.countByAnnonceId(annonce.annonceId()),
                "La fixture MySQL doit démarrer avec exactement neuf photos.");

        CyclicBarrier departCommun = new CyclicBarrier(2);
        ExecutorService executeur = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> premier = executeur.submit(() -> ajouterEnParallele(
                    departCommun, annonce.email(), annonce.annonceId(),
                    "https://cdn.tady.mg/concurrente-a.jpg"));
            Future<Boolean> second = executeur.submit(() -> ajouterEnParallele(
                    departCommun, annonce.email(), annonce.annonceId(),
                    "https://cdn.tady.mg/concurrente-b.jpg"));

            boolean premierReussi = premier.get(15, TimeUnit.SECONDS);
            boolean secondReussi = second.get(15, TimeUnit.SECONDS);
            assertTrue(premierReussi ^ secondReussi,
                    "Avec neuf photos MySQL, un seul ajout doit réussir; premier=" + premierReussi
                            + ", second=" + secondReussi);
            assertEquals(10L, photoRepository.countByAnnonceId(annonce.annonceId()),
                    "Le total final MySQL ne doit jamais dépasser dix photos.");
        } finally {
            executeur.shutdownNow();
            assertTrue(executeur.awaitTermination(5, TimeUnit.SECONDS),
                    "Les threads du test MySQL doivent terminer.");
        }
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
