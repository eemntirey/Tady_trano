package mg.emit.tadytrano.client.api;

/**
 * Erreur renvoyee par l'API (message du serveur + code HTTP).
 * statut vaut 0 quand le serveur est injoignable.
 */
public class ErreurApi extends RuntimeException {

    private final int statut;

    public ErreurApi(int statut, String message) {
        super(message);
        this.statut = statut;
    }

    public int statut() {
        return statut;
    }

    public boolean estNonConnecte() {
        return statut == 401;
    }
}
