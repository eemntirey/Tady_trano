package mg.emit.tadytrano.client.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeManagerTest {

    @Test
    void retirerEcouteurReduitLaListeEtLAbonneRetireNestPlusNotifie() {
        ThemeManager manager = new ThemeManager();
        List<Boolean> notificationsRetire = new ArrayList<>();
        List<Boolean> notificationsConserve = new ArrayList<>();

        Consumer<Boolean> ecouteurRetire = notificationsRetire::add;
        Consumer<Boolean> ecouteurConserve = notificationsConserve::add;
        manager.ajouterEcouteur(ecouteurRetire);
        manager.ajouterEcouteur(ecouteurConserve);

        assertEquals(2, manager.nombreEcouteurs());
        manager.retirerEcouteur(ecouteurRetire);
        assertEquals(1, manager.nombreEcouteurs());

        // L'abonnement notifie immédiatement à l'inscription; on isole les notifications suivantes.
        notificationsRetire.clear();
        notificationsConserve.clear();
        manager.notifierEcouteurs();

        assertTrue(notificationsRetire.isEmpty(), "L'écouteur retiré ne doit plus être notifié.");
        assertEquals(1, notificationsConserve.size(), "L'écouteur conservé doit toujours être notifié.");

        // Le retrait est idempotent du point de vue de la liste.
        manager.retirerEcouteur(ecouteurRetire);
        assertEquals(1, manager.nombreEcouteurs());
    }
}
