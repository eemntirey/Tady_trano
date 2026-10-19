package mg.emit.tadytrano.client.util;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Execute les appels API hors du thread JavaFX, puis revient sur le thread
 * FX pour les callbacks. Une seule thread : les requetes sont en sequence.
 * Expose aussi une propriete FX « occupe » (au moins une tache en vol) pour
 * afficher un indicateur de chargement et eviter les doubles clics.
 */
public final class Travailleur {

    private static final ExecutorService FILE = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "api-client");
        t.setDaemon(true);
        return t;
    });

    private static final AtomicInteger EN_VOL = new AtomicInteger();
    private static final BooleanProperty occupe = new SimpleBooleanProperty(false);

    private Travailleur() {
    }

    /** Propriete FX : vraie tant qu'au moins un appel API est en cours. */
    public static BooleanProperty occupeProperty() {
        return occupe;
    }

    public static boolean estOccupe() {
        return occupe.get();
    }

    public static <T> void faire(Callable<T> tache, Consumer<T> succes, Consumer<Exception> echec) {
        demarrer();
        FILE.execute(() -> {
            try {
                T resultat = tache.call();
                Platform.runLater(() -> {
                    try {
                        succes.accept(resultat);
                    } finally {
                        terminer();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    try {
                        echec.accept(e);
                    } finally {
                        terminer();
                    }
                });
            }
        });
    }

    /** La propriete FX ne doit etre modifiee que depuis le thread JavaFX. */
    private static void majOccupe(boolean valeur) {
        if (Platform.isFxApplicationThread()) {
            occupe.set(valeur);
        } else {
            Platform.runLater(() -> occupe.set(valeur));
        }
    }

    private static void demarrer() {
        if (EN_VOL.incrementAndGet() > 0) {
            majOccupe(true);
        }
    }

    private static void terminer() {
        if (EN_VOL.decrementAndGet() <= 0) {
            majOccupe(false);
        }
    }
}
