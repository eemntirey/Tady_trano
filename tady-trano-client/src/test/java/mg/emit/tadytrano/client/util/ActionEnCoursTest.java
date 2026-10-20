package mg.emit.tadytrano.client.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionEnCoursTest {

    @Test
    void refuseUneDeuxiemeSoumissionPendantUneAction() {
        ActionEnCours action = new ActionEnCours();

        assertTrue(action.demarrer());
        assertTrue(action.estEnCours());
        assertFalse(action.demarrer());
    }

    @Test
    void permetUneNouvelleSoumissionApresLiberation() {
        ActionEnCours action = new ActionEnCours();

        assertTrue(action.demarrer());
        action.terminer();

        assertFalse(action.estEnCours());
        assertTrue(action.demarrer());
    }
}
