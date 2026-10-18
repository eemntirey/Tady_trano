package mg.emit.tadytrano.client.ui;

import mg.emit.tadytrano.client.dto.AnnonceResumeResponse;

/**
 * Interface de routage et de navigation dans l'IHM JavaFX.
 */
public interface NavigationManager {

    void naviguerVersRecherche();

    void naviguerVersDetail(Long annonceId);

    void naviguerVersFavoris();

    void naviguerVersMesAnnonces();

    void naviguerVersAdmin();

    void ouvrirDialogueConnexion();

    void ouvrirDialoguePublication(Long annonceIdAEditer);
}
