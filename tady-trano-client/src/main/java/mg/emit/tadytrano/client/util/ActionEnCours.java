package mg.emit.tadytrano.client.util;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Petit verrou atomique pour empecher deux soumissions du meme formulaire
 * pendant qu'une mutation reseau est en cours.
 */
public final class ActionEnCours {

    private final AtomicBoolean active = new AtomicBoolean(false);

    /** Demarre l'action uniquement si aucune action equivalente n'est en cours. */
    public boolean demarrer() {
        return active.compareAndSet(false, true);
    }

    public boolean estEnCours() {
        return active.get();
    }

    /** Libere le verrou apres succes, echec ou fin du rafraichissement. */
    public void terminer() {
        active.set(false);
    }
}
