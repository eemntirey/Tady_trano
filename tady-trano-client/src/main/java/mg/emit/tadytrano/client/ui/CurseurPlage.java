package mg.emit.tadytrano.client.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * Curseur de fourchette a deux poignees (prix min / prix max).
 * Piste grise, remplissage bleu entre les deux valeurs, poignees deplacables
 * a la souris (clic ou glisser) ou au clavier (fleches = poignee basse,
 * Maj + fleches = poignee haute).
 */
public class CurseurPlage extends Region {

    private static final double LARGEUR_POIGNEE = 14;
    private static final double HAUTEUR_POIGNEE = 20;
    private static final double EPAISSEUR_PISTE = 4;

    private final double min;
    private final double max;
    private final double pas;

    private final DoubleProperty bas = new SimpleDoubleProperty(this, "bas");
    private final DoubleProperty haut = new SimpleDoubleProperty(this, "haut");

    private final Region piste = new Region();
    private final Region remplissage = new Region();
    private final Region poigneeBas = new Region();
    private final Region poigneeHaut = new Region();

    private Region poigneeActive;

    public CurseurPlage(double min, double max, double basInitial, double hautInitial, double pas) {
        this.min = min;
        this.max = max;
        this.pas = pas > 0 ? pas : 1;

        getStyleClass().add("range-slider");
        piste.getStyleClass().add("range-track");
        remplissage.getStyleClass().add("range-fill");
        poigneeBas.getStyleClass().add("range-thumb");
        poigneeHaut.getStyleClass().add("range-thumb");
        getChildren().addAll(piste, remplissage, poigneeBas, poigneeHaut);

        setFocusTraversable(true);
        setMinHeight(HAUTEUR_POIGNEE);
        setPrefHeight(HAUTEUR_POIGNEE);

        bas.set(ajuster(basInitial));
        haut.set(ajuster(hautInitial));

        bas.addListener((o, a, b) -> {
            if (b != null && b.doubleValue() > haut.get()) {
                bas.set(haut.get());
            } else {
                requestLayout();
            }
        });
        haut.addListener((o, a, b) -> {
            if (b != null && b.doubleValue() < bas.get()) {
                haut.set(bas.get());
            } else {
                requestLayout();
            }
        });

        setOnMousePressed(this::presser);
        setOnMouseDragged(this::glisser);
        setOnMouseReleased(e -> poigneeActive = null);
        setOnKeyPressed(this::touche);
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double bas() {
        return bas.get();
    }

    public double haut() {
        return haut.get();
    }

    public DoubleProperty basProperty() {
        return bas;
    }

    public DoubleProperty hautProperty() {
        return haut;
    }

    /** Ramene les deux poignees aux valeurs initiales. */
    public void reinitialiser(double basInitial, double hautInitial) {
        bas.set(ajuster(basInitial));
        haut.set(ajuster(hautInitial));
    }

    @Override
    protected void layoutChildren() {
        double centreY = (getHeight() - EPAISSEUR_PISTE) / 2;
        double utile = Math.max(0, getWidth() - LARGEUR_POIGNEE);
        double xBas = marge() + fraction(bas.get()) * utile;
        double xHaut = marge() + fraction(haut.get()) * utile;

        piste.resizeRelocate(marge(), centreY, utile, EPAISSEUR_PISTE);
        remplissage.resizeRelocate(xBas, centreY, Math.max(0, xHaut - xBas), EPAISSEUR_PISTE);
        poigneeBas.resizeRelocate(xBas - LARGEUR_POIGNEE / 2,
                (getHeight() - HAUTEUR_POIGNEE) / 2, LARGEUR_POIGNEE, HAUTEUR_POIGNEE);
        poigneeHaut.resizeRelocate(xHaut - LARGEUR_POIGNEE / 2,
                (getHeight() - HAUTEUR_POIGNEE) / 2, LARGEUR_POIGNEE, HAUTEUR_POIGNEE);
    }

    @Override
    protected double computeMinWidth(double height) {
        return LARGEUR_POIGNEE * 2;
    }

    @Override
    protected double computePrefWidth(double height) {
        return 280;
    }

    private void presser(MouseEvent e) {
        double x = e.getX();
        poigneeActive = Math.abs(x - centre(bas.get())) <= Math.abs(x - centre(haut.get()))
                ? poigneeBas : poigneeHaut;
        deplacerVers(x);
        requestFocus();
        e.consume();
    }

    private void glisser(MouseEvent e) {
        if (poigneeActive != null) {
            deplacerVers(e.getX());
            e.consume();
        }
    }

    private void touche(javafx.scene.input.KeyEvent e) {
        double delta = (e.isShiftDown() ? pas * 10 : pas) * (e.getCode() == KeyCode.RIGHT ? 1 : -1);
        if (e.getCode() != KeyCode.LEFT && e.getCode() != KeyCode.RIGHT) {
            return;
        }
        if (e.isShiftDown()) {
            haut.set(ajuster(haut.get() + delta));
        } else {
            bas.set(ajuster(bas.get() + delta));
        }
        e.consume();
    }

    private void deplacerVers(double x) {
        double utile = Math.max(1, getWidth() - LARGEUR_POIGNEE);
        double fraction = Math.max(0, Math.min(1, (x - marge()) / utile));
        double valeur = min + fraction * (max - min);
        if (poigneeActive == poigneeBas) {
            bas.set(Math.min(ajuster(valeur), haut.get()));
        } else {
            haut.set(Math.max(ajuster(valeur), bas.get()));
        }
    }

    private double centre(double valeur) {
        return marge() + fraction(valeur) * Math.max(0, getWidth() - LARGEUR_POIGNEE);
    }

    private double fraction(double valeur) {
        return max <= min ? 0 : (valeur - min) / (max - min);
    }

    private double marge() {
        return LARGEUR_POIGNEE / 2;
    }

    private double ajuster(double valeur) {
        double arrondi = min + Math.round((valeur - min) / pas) * pas;
        return Math.max(min, Math.min(max, arrondi));
    }
}
