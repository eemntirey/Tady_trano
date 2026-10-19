package mg.emit.tadytrano.presentation.rest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Critere du cahier des charges « liste fluide avec 1 000 annonces » (analyse 3.6).
 * Insere 1 000 annonces actives par lots SQL puis mesure la duree de la premiere
 * page et de la recherche triee par distance. Borne genereuse (2 s) pour rester
 * stable sur machine lente ; les temps reels sont imprimes dans le rapport Surefire.
 * Base H2 dediee pour ne pas interferer avec les autres tests d'integration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:tadyfluidite;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
class AnnonceRechercheFluiditeTest extends AnnonceTestSupport {

    private static final int NB_ANNONCES = 1000;
    private static final int TAILLE_PAGE = 12;
    /** Seuil de fluidite volontairement large (machine CI lente). */
    private static final long SEUIL_MS = 2000;

    @Test
    void rechercheFluideAvecMilleAnnonces() throws Exception {
        String jeton = proprietaireConnecte();
        Long typeId = typeLocationId();
        Long quartierId = jdbc.queryForObject("SELECT id FROM quartier LIMIT 1", Long.class);
        Long proprietaireId = jdbc.queryForObject("SELECT id FROM proprietaire LIMIT 1", Long.class);
        insererAnnonces(proprietaireId, typeId, quartierId);

        // 1. Premiere page de la liste (cas du client)
        long debut = System.nanoTime();
        mockMvc.perform(get("/api/annonces").param("page", "0").param("taille", String.valueOf(TAILLE_PAGE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(NB_ANNONCES))
                .andExpect(jsonPath("$.contenu", hasSize(TAILLE_PAGE)));
        long pageMs = (System.nanoTime() - debut) / 1_000_000;

        // 2. Tri par distance : le cas le plus couteux (Haversine sur toutes les lignes)
        debut = System.nanoTime();
        mockMvc.perform(get("/api/annonces")
                        .param("lat", "-18.91").param("lng", "47.52")
                        .param("page", "0").param("taille", String.valueOf(TAILLE_PAGE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(NB_ANNONCES))
                .andExpect(jsonPath("$.contenu", hasSize(TAILLE_PAGE)));
        long distanceMs = (System.nanoTime() - debut) / 1_000_000;

        // 3. Filtre tranche de prix
        debut = System.nanoTime();
        mockMvc.perform(get("/api/annonces")
                        .param("prixMin", "300000").param("prixMax", "900000")
                        .param("page", "0").param("taille", String.valueOf(TAILLE_PAGE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu", hasSize(TAILLE_PAGE)));
        long prixMs = (System.nanoTime() - debut) / 1_000_000;

        System.out.printf("Fluidite recherche (1000 annonces) : page=%d ms, distance=%d ms, filtre prix=%d ms%n",
                pageMs, distanceMs, prixMs);

        assertTrue(pageMs < SEUIL_MS, "Premiere page trop lente : " + pageMs + " ms (seuil " + SEUIL_MS + " ms)");
        assertTrue(distanceMs < SEUIL_MS, "Tri par distance trop lent : " + distanceMs + " ms (seuil " + SEUIL_MS + " ms)");
        assertTrue(prixMs < SEUIL_MS, "Filtre prix trop lent : " + prixMs + " ms (seuil " + SEUIL_MS + " ms)");
    }

    /** Insertion par lots SQL : 1 000 annonces actives, dates et coordonnees variees. */
    private void insererAnnonces(Long proprietaireId, Long typeId, Long quartierId) {
        String sql = """
                INSERT INTO annonce
                  (proprietaire_id, type_location_id, quartier_id, titre, description,
                   prix, surface, nb_pieces, latitude, longitude, statut, masquee_par_admin, date_publication)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', FALSE,
                        TIMESTAMPADD(SECOND, ?, CURRENT_TIMESTAMP))""";
        jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, proprietaireId);
                ps.setLong(2, typeId);
                ps.setLong(3, quartierId);
                ps.setString(4, "Annonce test %04d".formatted(i));
                ps.setString(5, "Cataloge de test pour la mesure de fluidite");
                ps.setBigDecimal(6, BigDecimal.valueOf(400_000L + (i % 600) * 1_000L));
                ps.setInt(7, 20 + (i % 80));
                ps.setInt(8, 1 + (i % 4));
                ps.setDouble(9, -18.91 + (i % 50) * 0.01);
                ps.setDouble(10, 47.52 + (i % 50) * 0.01);
                ps.setInt(11, -i); // decalage d'une seconde par annonce : tri par date deterministe
            }

            @Override
            public int getBatchSize() {
                return NB_ANNONCES;
            }
        });
    }
}
