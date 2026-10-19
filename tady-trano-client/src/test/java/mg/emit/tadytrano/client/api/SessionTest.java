package mg.emit.tadytrano.client.api;

import mg.emit.tadytrano.client.dto.ProfilResponse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les rôles côté client doivent refléter exactement les contraintes serveur :
 * les routes /api/proprietaires exigent le rôle strict PROPRIETAIRE,
 * les ADMIN en sont exclus (même si la vue « Modération » leur est ouverte).
 */
class SessionTest {

    private static Session connecter(String role) {
        Session session = new Session();
        session.connecter("jeton-test", new ProfilResponse(1L, "u@t.mg", "Rakoto", "Jean", role));
        return session;
    }

    @Test
    void proprietaireStrictEstProprietaireEtPasAdmin() {
        Session session = connecter("PROPRIETAIRE");

        assertTrue(session.estConnecte());
        assertTrue(session.estProprietaire());
        assertFalse(session.estAdmin());
        assertTrue(session.proprietaireProperty().get());
        assertFalse(session.adminProperty().get());
        assertEquals("jeton-test", session.jeton());
        assertEquals("u@t.mg", session.email());
    }

    @Test
    void adminNestPasProprietaire() {
        Session session = connecter("ADMIN");

        assertTrue(session.estAdmin());
        assertFalse(session.estProprietaire());
        assertFalse(session.proprietaireProperty().get());
        assertTrue(session.adminProperty().get());
    }

    @Test
    void locataireNaAucunRoleEspace() {
        Session session = connecter("LOCATAIRE");

        assertTrue(session.estConnecte());
        assertFalse(session.estProprietaire());
        assertFalse(session.estAdmin());
        assertFalse(session.proprietaireProperty().get());
        assertFalse(session.adminProperty().get());
    }

    @Test
    void deconnecterEffaceJetonEtRoles() {
        Session session = connecter("PROPRIETAIRE");
        session.deconnecter();

        assertFalse(session.estConnecte());
        assertFalse(session.estProprietaire());
        assertFalse(session.estAdmin());
        assertNull(session.jeton());
        assertNull(session.profil());
        assertNull(session.email());
        assertFalse(session.proprietaireProperty().get());
        assertFalse(session.adminProperty().get());
    }

    @Test
    void lesProprietesFxNotifientLesEcouteurs() {
        Session session = new Session();
        AtomicInteger declenchements = new AtomicInteger();
        session.proprietaireProperty().addListener((obs, old, val) -> declenchements.incrementAndGet());

        session.connecter("t", new ProfilResponse(1L, "a@b.mg", "N", "P", "PROPRIETAIRE"));
        assertEquals(1, declenchements.get());
        assertTrue(session.proprietaireProperty().get());

        session.deconnecter();
        assertEquals(2, declenchements.get());
        assertFalse(session.proprietaireProperty().get());
    }
}
