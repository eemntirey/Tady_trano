package mg.emit.tadytrano.client.util;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Execute les appels API hors du thread JavaFX, puis revient sur le thread
 * FX pour les callbacks. Une seule thread : les requetes sont en sequence.
 */
public final class Travailleur {

    private static final ExecutorService FILE = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "api-client");
        t.setDaemon(true);
        return t;
    });

    private Travailleur() {
    }

    public static <T> void faire(Callable<T> tache, Consumer<T> succes, Consumer<Exception> echec) {
        FILE.execute(() -> {
            try {
                T resultat = tache.call();
                Platform.runLater(() -> succes.accept(resultat));
            } catch (Exception e) {
                Platform.runLater(() -> echec.accept(e));
            }
        });
    }
}
