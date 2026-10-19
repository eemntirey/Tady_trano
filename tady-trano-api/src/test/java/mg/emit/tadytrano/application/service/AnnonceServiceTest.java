package mg.emit.tadytrano.application.service;

import mg.emit.tadytrano.domain.model.Annonce;
import mg.emit.tadytrano.domain.model.Quartier;
import mg.emit.tadytrano.domain.model.Ville;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du calcul Haversine (BUG-04 / A-04) : protection numerique
 * contre le depassement de 1 et comportement sur des points presque antipodaux.
 */
class AnnonceServiceTest {

    /** Point antipodal exact : (0,0) et (0,180) -> environ 20 015 km, jamais 0. */
    @Test
    void distanceAntipodaleExacteNonNulle() {
        Quartier q = new Quartier();
        q.setLatitude(0.0);
        q.setLongitude(180.0);
        Ville ville = new Ville();
        ville.setNom("Test");
        q.setVille(ville);

        Annonce annonce = new Annonce();
        annonce.setLatitude(0.0);
        annonce.setLongitude(180.0);

        Double distance = AnnonceService.distanceKm(0.0, 0.0, annonce);
        assertNotNull(distance);
        assertTrue(distance > 19_000.0,
                "La distance antipodale devrait etre proche de 20 015 km, pas 0.0 : " + distance);
    }

    /** Points proches mais presque antipodaux : ne doit pas produire NaN -> 0. */
    @Test
    void distancePresqueAntipodaleNonNulle() {
        // Coordonnees proches du meridien de Greenwich et de son antipode.
        Annonce annonce = new Annonce();
        annonce.setLatitude(0.001);
        annonce.setLongitude(179.999);

        Double distance = AnnonceService.distanceKm(0.0, 0.0, annonce);
        assertNotNull(distance);
        assertTrue(distance > 19_000.0,
                "La distance presque antipodale ne doit pas etre 0.0 : " + distance);
    }

    @Test
    void distanceIdentiqueEstZero() {
        Annonce annonce = new Annonce();
        annonce.setLatitude(-18.914);
        annonce.setLongitude(47.524);

        Double distance = AnnonceService.distanceKm(-18.914, 47.524, annonce);
        assertNotNull(distance);
        assertEquals(0.0, distance, 0.1);
    }
}
