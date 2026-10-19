package mg.emit.tadytrano.client.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AffichageImageUrlTest {

    @Test
    void imagesUnsplashSontForceesEnJpegPourJavaFx() {
        String resultat = Affichage.urlImageCompatible(
                "https://images.unsplash.com/photo-test?auto=format&fit=crop&w=800&q=80");

        assertEquals(
                "https://images.unsplash.com/photo-test?fit=crop&w=800&q=80&fm=jpg",
                resultat);
        assertFalse(resultat.contains("auto=format"));
    }

    @Test
    void formatUnsplashExistantEstRemplaceParJpegSansPerdreLesAutresParametres() {
        String resultat = Affichage.urlImageCompatible(
                "https://images.unsplash.com/photo-test?fit=crop&fm=webp&w=400");

        assertEquals("https://images.unsplash.com/photo-test?fit=crop&w=400&fm=jpg", resultat);
    }

    @Test
    void urlHttpExterneEstConservee() {
        String url = "https://cdn.example.test/logement.jpg?largeur=640";
        assertEquals(url, Affichage.urlImageCompatible(url));
    }

    @Test
    void urlNonHttpOuInvalideEstRefusee() {
        assertNull(Affichage.urlImageCompatible("javascript:alert(1)"));
        assertNull(Affichage.urlImageCompatible("/images/logement.jpg"));
        assertNull(Affichage.urlImageCompatible("https://"));
        assertTrue(Affichage.urlImageCompatible("  ") == null);
    }
}
