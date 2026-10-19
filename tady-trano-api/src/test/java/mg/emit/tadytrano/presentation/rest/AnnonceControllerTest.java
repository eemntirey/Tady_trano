package mg.emit.tadytrano.presentation.rest;

import mg.emit.tadytrano.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours complet : inscription proprietaire, profil, publication d'annonce,
 * recherche publique avec filtres, photos, changement de statut, favoris.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnnonceControllerTest extends AnnonceTestSupport {

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    void profilProprietaireEstReserveAuRoleProprietaire() throws Exception {
        String jeton = inscrire(emailUnique(), Role.LOCATAIRE);
        Long quartierId = villeEtQuartier("Antsirabe", "Centre");

        String profil = """
                {"type":"PARTICULIER","telephone":"0341234567","quartierId":%d}""".formatted(quartierId);
        mockMvc.perform(post("/api/proprietaires")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profil))
                .andExpect(status().isForbidden());
    }

    @Test
    void creationDAnnonceEtRecherchePublique() throws Exception {
        String jeton = proprietaireConnecte();
        Long typeLocationId = typeLocationId();
        Long quartierId = jdbc.queryForObject("SELECT id FROM quartier LIMIT 1", Long.class);

        // Publication
        String creation = mockMvc.perform(post("/api/annonces")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annonceJson(typeLocationId, quartierId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titre").value("Studio centre-ville"))
                .andExpect(jsonPath("$.statut").value("ACTIVE"))
                .andExpect(jsonPath("$.quartier.ville.nom").value("Antananarivo"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Long annonceId = objectMapper.readTree(creation).get("id").asLong();
        assertNotNull(annonceId);

        // Recherche publique sans jeton, filtre par tranche de prix
        mockMvc.perform(get("/api/annonces")
                        .param("prixMax", "500000")
                        .param("nbPiecesMin", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.contenu[0].titre").value("Studio centre-ville"))
                .andExpect(jsonPath("$.contenu[0].quartier.ville.nom").value("Antananarivo"));

        // Filtre qui ne correspond pas
        mockMvc.perform(get("/api/annonces").param("prixMax", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // Tri par distance avec une position fournie
        mockMvc.perform(get("/api/annonces")
                        .param("lat", "-18.91")
                        .param("lng", "47.52"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.contenu[0].distanceKm").value(0.0));
    }

    @Test
    void contactProprietaireVisibleQuandConnecteSeulement() throws Exception {
        String jeton = proprietaireConnecte();
        Long typeLocationId = typeLocationId();
        Long quartierId = jdbc.queryForObject("SELECT id FROM quartier LIMIT 1", Long.class);

        mockMvc.perform(post("/api/annonces")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annonceJson(typeLocationId, quartierId)))
                .andExpect(status().isCreated());

        Long annonceId = jdbc.queryForObject("SELECT id FROM annonce", Long.class);
        String url = "/api/annonces/" + annonceId;

        // Anonyme : pas de contact
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact").doesNotExist());

        // Connecte : contact present
        mockMvc.perform(get(url).header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.telephone").value("0341234567"))
                .andExpect(jsonPath("$.contact.nom").value("Jean Rakoto"));
    }

    @Test
    void gestionPhotosEtStatut() throws Exception {
        String jeton = proprietaireConnecte();
        Long typeLocationId = typeLocationId();
        Long quartierId = jdbc.queryForObject("SELECT id FROM quartier LIMIT 1", Long.class);

        mockMvc.perform(post("/api/annonces")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annonceJson(typeLocationId, quartierId)))
                .andExpect(status().isCreated());
        Long annonceId = jdbc.queryForObject("SELECT id FROM annonce", Long.class);

        // Ajout photo principale
        mockMvc.perform(post("/api/annonces/" + annonceId + "/photos")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://cdn.tady.mg/1.jpg\",\"principale\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.principale").value(true));

        // L'annonce en liste affiche l'url de la photo principale
        mockMvc.perform(get("/api/annonces").param("quartierId", String.valueOf(quartierId)))
                .andExpect(jsonPath("$.contenu[0].urlPhotoPrincipale").value("https://cdn.tady.mg/1.jpg"));

        // Un locataire peut mettre en favori une annonce encore active.
        String jetonLocataire = inscrire(emailUnique(), Role.LOCATAIRE);
        mockMvc.perform(post("/api/favoris/" + annonceId)
                        .header("Authorization", "Bearer " + jetonLocataire))
                .andExpect(status().isCreated());

        // Changement de statut
        mockMvc.perform(patch("/api/annonces/" + annonceId + "/statut")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"LOUEE\"}"))
                .andExpect(status().isNoContent());

        // Une annonce louee n'apparait plus dans la recherche publique.
        mockMvc.perform(get("/api/annonces"))
                .andExpect(jsonPath("$.totalElements").value(0));

        // Un favori devenu inactif est filtre et ne peut plus etre ajoute a nouveau.
        mockMvc.perform(get("/api/favoris")
                        .header("Authorization", "Bearer " + jetonLocataire))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(post("/api/favoris/" + annonceId)
                        .header("Authorization", "Bearer " + jetonLocataire))
                .andExpect(status().isNotFound());

        // Mais reste visible dans l'espace du proprietaire
        mockMvc.perform(get("/api/proprietaires/mes-annonces")
                        .header("Authorization", "Bearer " + jeton))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.contenu[0].statut").value("LOUEE"));
    }

    @Test
    void favorisAjoutRetraitEtListe() throws Exception {
        String jetonProprio = proprietaireConnecte();
        Long typeLocationId = typeLocationId();
        Long quartierId = jdbc.queryForObject("SELECT id FROM quartier LIMIT 1", Long.class);
        mockMvc.perform(post("/api/annonces")
                        .header("Authorization", "Bearer " + jetonProprio)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annonceJson(typeLocationId, quartierId)))
                .andExpect(status().isCreated());
        Long annonceId = jdbc.queryForObject("SELECT id FROM annonce", Long.class);

        // Un locataire ajoute l'annonce en favori
        String jetonLocataire = inscrire(emailUnique(), Role.LOCATAIRE);
        mockMvc.perform(post("/api/favoris/" + annonceId)
                        .header("Authorization", "Bearer " + jetonLocataire))
                .andExpect(status().isCreated());

        // Doublon refuse
        mockMvc.perform(post("/api/favoris/" + annonceId)
                        .header("Authorization", "Bearer " + jetonLocataire))
                .andExpect(status().isConflict());

        // Liste des favoris
        mockMvc.perform(get("/api/favoris")
                        .header("Authorization", "Bearer " + jetonLocataire))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.contenu[0].annonce.titre").value("Studio centre-ville"));

        // Retrait
        mockMvc.perform(delete("/api/favoris/" + annonceId)
                        .header("Authorization", "Bearer " + jetonLocataire))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/favoris")
                        .header("Authorization", "Bearer " + jetonLocataire))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void suppressionPhotoPrincipalePromouvoitLaSuivanteEtReordonne() throws Exception {
        String jeton = proprietaireConnecte();
        Long typeId = typeLocationId();
        Long quartierId = jdbc.queryForObject("SELECT id FROM quartier LIMIT 1", Long.class);
        mockMvc.perform(post("/api/annonces")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annonceJson(typeId, quartierId)))
                .andExpect(status().isCreated());
        Long annonceId = jdbc.queryForObject("SELECT id FROM annonce", Long.class);

        String premiere = mockMvc.perform(post("/api/annonces/" + annonceId + "/photos")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://cdn.tady.mg/a.jpg\",\"principale\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.principale").value(true))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Long premiereId = objectMapper.readTree(premiere).get("id").asLong();

        String seconde = mockMvc.perform(post("/api/annonces/" + annonceId + "/photos")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://cdn.tady.mg/b.jpg\",\"principale\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.principale").value(false))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Long secondeId = objectMapper.readTree(seconde).get("id").asLong();

        mockMvc.perform(delete("/api/annonces/" + annonceId + "/photos/" + premiereId)
                        .header("Authorization", "Bearer " + jeton))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/annonces/" + annonceId + "/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(secondeId))
                .andExpect(jsonPath("$[0].principale").value(true))
                .andExpect(jsonPath("$[0].ordre").value(1));
    }

    @Test
    void annonceDUnAutreProprietaireNestPasModifiable() throws Exception {
        String jetonA = proprietaireConnecte();
        Long typeLocationId = typeLocationId();
        Long quartierId = jdbc.queryForObject("SELECT id FROM quartier LIMIT 1", Long.class);
        mockMvc.perform(post("/api/annonces")
                        .header("Authorization", "Bearer " + jetonA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annonceJson(typeLocationId, quartierId)))
                .andExpect(status().isCreated());
        Long annonceId = jdbc.queryForObject("SELECT id FROM annonce", Long.class);

        // Un second proprietaire tente d'agir sur l'annonce d'autrui
        Long autreQuartierId = villeEtQuartier("Toamasina", "Centre");
        String jetonB = inscrire(emailUnique(), Role.PROPRIETAIRE);
        mockMvc.perform(post("/api/proprietaires")
                        .header("Authorization", "Bearer " + jetonB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"PARTICULIER","telephone":"0349999999","quartierId":%d}"""
                                .formatted(autreQuartierId)))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/annonces/" + annonceId + "/statut")
                        .header("Authorization", "Bearer " + jetonB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"MASQUEE\"}"))
                .andExpect(status().isForbidden());

        // Le proprietaire ne peut pas contourner la moderation ni masquer une annonce.
        mockMvc.perform(patch("/api/annonces/" + annonceId + "/statut")
                        .header("Authorization", "Bearer " + jetonA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"MASQUEE\"}"))
                .andExpect(status().isForbidden());

        // Seule l'administration peut masquer l'annonce.
        String emailAdmin = emailUnique();
        inscrire(emailAdmin, Role.LOCATAIRE);
        jdbc.update("UPDATE utilisateur SET role = 'ADMIN' WHERE email = ?", emailAdmin);
        String jetonAdmin = connexion(emailAdmin);
        mockMvc.perform(patch("/api/admin/annonces/" + annonceId + "/statut")
                        .header("Authorization", "Bearer " + jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"MASQUEE\"}"))
                .andExpect(status().isNoContent());

        // Une annonce masquee par l'admin ne peut pas etre reactivee par le proprietaire.
        mockMvc.perform(patch("/api/annonces/" + annonceId + "/statut")
                        .header("Authorization", "Bearer " + jetonA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"ACTIVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void referentielsPublicsEtAnnoncesAnonymesAccessibles() throws Exception {
        mockMvc.perform(get("/api/villes")).andExpect(status().isOk());
        mockMvc.perform(get("/api/types-location")).andExpect(status().isOk());
        mockMvc.perform(get("/api/annonces")).andExpect(status().isOk());
        // En revanche la creation anonyme est refusee
        mockMvc.perform(post("/api/annonces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annonceJson(1L, 1L)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointsExigentRoleAdminCoteServeur() throws Exception {
        // 1. Acces sans jeton -> 401 Unauthorized
        mockMvc.perform(get("/api/admin/annonces"))
                .andExpect(status().isUnauthorized());

        // 2. Acces avec role LOCATAIRE -> 403 Forbidden
        String jetonLocataire = inscrire(emailUnique(), Role.LOCATAIRE);
        mockMvc.perform(get("/api/admin/annonces")
                        .header("Authorization", "Bearer " + jetonLocataire))
                .andExpect(status().isForbidden());

        // 3. Acces avec role PROPRIETAIRE -> 403 Forbidden
        String jetonProprio = inscrire(emailUnique(), Role.PROPRIETAIRE);
        mockMvc.perform(get("/api/admin/annonces")
                        .header("Authorization", "Bearer " + jetonProprio))
                .andExpect(status().isForbidden());

        // 4. Compte admin cree hors inscription (promotion en base, comme en
        //    production ou l'admin est provisionne manuellement) -> 200 OK
        String emailAdmin = emailUnique();
        inscrire(emailAdmin, Role.LOCATAIRE);
        jdbc.update("UPDATE utilisateur SET role = 'ADMIN' WHERE email = ?", emailAdmin);
        String jetonAdmin = connexion(emailAdmin);
        mockMvc.perform(get("/api/admin/annonces")
                        .header("Authorization", "Bearer " + jetonAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void lectureAnnonceMasqueeParIdentifiantRenvoie404() throws Exception {
        String jeton = proprietaireConnecte();
        Long typeLocationId = typeLocationId();
        Long quartierId = jdbc.queryForObject("SELECT id FROM quartier LIMIT 1", Long.class);
        mockMvc.perform(post("/api/annonces")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annonceJson(typeLocationId, quartierId)))
                .andExpect(status().isCreated());
        Long annonceId = jdbc.queryForObject("SELECT id FROM annonce", Long.class);

        // Seule l'administration peut masquer l'annonce.
        String emailAdmin = emailUnique();
        inscrire(emailAdmin, Role.LOCATAIRE);
        jdbc.update("UPDATE utilisateur SET role = 'ADMIN' WHERE email = ?", emailAdmin);
        String jetonAdmin = connexion(emailAdmin);
        mockMvc.perform(patch("/api/admin/annonces/" + annonceId + "/statut")
                        .header("Authorization", "Bearer " + jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"MASQUEE\"}"))
                .andExpect(status().isNoContent());

        // Un utilisateur anonyme ne doit pas voir l'annonce masquee par id
        mockMvc.perform(get("/api/annonces/" + annonceId))
                .andExpect(status().isNotFound());

        // Un autre proprietaire non plus
        String autreJeton = inscrire(emailUnique(), Role.PROPRIETAIRE);
        mockMvc.perform(get("/api/annonces/" + annonceId).header("Authorization", "Bearer " + autreJeton))
                .andExpect(status().isNotFound());

        // Mais le proprietaire legitime peut
        mockMvc.perform(get("/api/annonces/" + annonceId).header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk());
    }

    @Test
    void triParDistanceCroissanteMultiAnnonces() throws Exception {
        String jeton = proprietaireConnecte();
        Long typeId = typeLocationId();
        Long quartierTana = jdbc.queryForObject("SELECT id FROM quartier LIMIT 1", Long.class);
        Long quartierFianar = villeEtQuartier("Fianarantsoa", "Andrainjato");

        // Annonce 1 : Antananarivo (-18.91, 47.52)
        String jsonTana = """
                {
                  "typeLocationId": %d,
                  "quartierId": %d,
                  "titre": "Villa Antananarivo",
                  "description": "Centre",
                  "prix": 800000,
                  "surface": 100,
                  "nbPieces": 4,
                  "latitude": -18.91,
                  "longitude": 47.52
                }""".formatted(typeId, quartierTana);

        // Annonce 2 : Fianarantsoa (-21.46, 47.10)
        String jsonFianar = """
                {
                  "typeLocationId": %d,
                  "quartierId": %d,
                  "titre": "Maison Fianarantsoa",
                  "description": "Campus",
                  "prix": 300000,
                  "surface": 50,
                  "nbPieces": 2,
                  "latitude": -21.46,
                  "longitude": 47.10
                }""".formatted(typeId, quartierFianar);

        mockMvc.perform(post("/api/annonces").header("Authorization", "Bearer " + jeton)
                .contentType(MediaType.APPLICATION_JSON).content(jsonTana)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/annonces").header("Authorization", "Bearer " + jeton)
                .contentType(MediaType.APPLICATION_JSON).content(jsonFianar)).andExpect(status().isCreated());

        // Recherche depuis Antananarivo (-18.91, 47.52) -> Villa Antananarivo en premier
        mockMvc.perform(get("/api/annonces").param("lat", "-18.91").param("lng", "47.52"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].titre").value("Villa Antananarivo"))
                .andExpect(jsonPath("$.contenu[1].titre").value("Maison Fianarantsoa"));

        // Recherche depuis Fianarantsoa (-21.46, 47.10) -> Maison Fianarantsoa en premier
        mockMvc.perform(get("/api/annonces").param("lat", "-21.46").param("lng", "47.10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].titre").value("Maison Fianarantsoa"))
                .andExpect(jsonPath("$.contenu[1].titre").value("Villa Antananarivo"));
    }

    // ------------------------------------------------------------------
    // Tests de droits (1.3 du document Jhonny)
    // ------------------------------------------------------------------

    @Test
    void autreProprietaireModificationInterditeRenvoie403() throws Exception {
        String jetonA = proprietaireConnecte();
        Long typeLocationId = typeLocationId();
        Long quartierId = jdbc.queryForObject("SELECT id FROM quartier LIMIT 1", Long.class);
        mockMvc.perform(post("/api/annonces")
                        .header("Authorization", "Bearer " + jetonA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annonceJson(typeLocationId, quartierId)))
                .andExpect(status().isCreated());
        Long annonceId = jdbc.queryForObject("SELECT id FROM annonce", Long.class);

        String jetonB = inscrire(emailUnique(), Role.PROPRIETAIRE);
        mockMvc.perform(patch("/api/annonces/" + annonceId + "/statut")
                        .header("Authorization", "Bearer " + jetonB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"MASQUEE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void locataireSurEndpointAdminRenvoie403() throws Exception {
        String jetonLocataire = inscrire(emailUnique(), Role.LOCATAIRE);
        mockMvc.perform(get("/api/admin/annonces")
                        .header("Authorization", "Bearer " + jetonLocataire))
                .andExpect(status().isForbidden());
    }

    @Test
    void requeteProtegeeSansJetonRenvoie401() throws Exception {
        mockMvc.perform(get("/api/admin/annonces"))
                .andExpect(status().isUnauthorized());
    }
}
