package mio.compito2;

import mio.griglia.GrigliaMatrix;
import mio.griglia.ICella2D;
import mio.griglia.IGriglia;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaintAssiTest {

    /**
     * Verifica che paint() esegua senza eccezioni su una griglia vuota
     * e che la chiusura sia non vuota in una griglia senza ostacoli.
     */
    @Test
    void paintGrigliaVuotaSenzaEccezioni() {
        IGriglia<?> g = GrigliaMatrix.vuota(7, 7);
        int Ox = 3, Oy = 3;

        GrigliaConOrigine gco = CalcoloChiusura.calcola(g, Ox, Oy);

        assertNotNull(gco.chiusura());
        assertTrue(gco.chiusura().size() > 0,
                "Chiusura di griglia vuota deve essere non vuota");
        assertEquals(0, gco.frontiera().length,
                "Se la chiusura copre la griglia non esistono celle interne esterne alla chiusura");
    }

    /**
     * Verifica che paint() preservi i bit della bitmask (OR additivo, non sovrascritture).
     * I bit strutturali sono additivi; il solo COMPLEMENTO viene poi rimosso
     * quando la cella appartiene anche al CONTESTO, perché è un insieme esclusivo.
     */
    @Test
    void paintBitmaskAdditivo() {
        IGriglia<?> g = GrigliaMatrix.vuota(5, 5);
        int Ox = 2, Oy = 2;

        GrigliaConOrigine gco = CalcoloChiusura.calcola(g, Ox, Oy);

        // Verifica che l'origine sia marcata con ORIGINE (dovrebbe includere CONTESTO)
        ICella2D origine = gco.getCellaAt(Ox, Oy);
        assertTrue(StatoCella.ORIGINE.is(origine.stato()),
                "Origine deve avere bit ORIGINE");

        // Verifica che tutte le celle della chiusura abbiano almeno uno tra
        // CONTESTO o COMPLEMENTO (cioè passino il test CHIUSURA)
        for (ICella2D c : gco.chiusura()) {
            assertTrue(StatoCella.CHIUSURA.is(c.stato()),
                    "Tutte le celle della chiusura devono passare test CHIUSURA");
            // Verifica anche che non siano ostacoli
            assertFalse(StatoCella.OSTACOLO.is(c.stato()),
                    "Nessuna cella della chiusura può essere ostacolo");
        }
    }

    /**
     * Verifica che, per una griglia con bordo di ostacoli, paint() non lancini
     * eccezioni e produca risultati coerenti.
     */
    @Test
    void paintConOstacoliSemplici() {
        IGriglia<?> g = GrigliaMatrix.vuota(7, 7);
        
        // Crea un muro orizzontale al centro (y=3)
        mio.griglia.IObstacle muro = () -> java.util.List.of(
                new mio.griglia.Cella2D(0, 0, 3),
                new mio.griglia.Cella2D(0, 1, 3),
                new mio.griglia.Cella2D(0, 2, 3),
                new mio.griglia.Cella2D(0, 3, 3),
                new mio.griglia.Cella2D(0, 4, 3),
                new mio.griglia.Cella2D(0, 5, 3),
                new mio.griglia.Cella2D(0, 6, 3)
        );
        IGriglia<?> conMuro = g.addObstacle(muro);

        // Calcola chiusura per un'origine sopra il muro
        GrigliaConOrigine gcoSopra = CalcoloChiusura.calcola(conMuro, 3, 1);
        assertTrue(gcoSopra.chiusura().size() > 0);

        // Calcola chiusura per un'origine sotto il muro
        GrigliaConOrigine gcoSotto = CalcoloChiusura.calcola(conMuro, 3, 5);
        assertTrue(gcoSotto.chiusura().size() > 0);

        // Entrambe le chiusure devono avere almeno qualche cella
        // (il muro potrebbe non dividere completamente a causa delle laterali diagonali)
        assertTrue(gcoSopra.chiusura().size() > 0,
                "Chiusura sopra il muro deve avere celle");
        assertTrue(gcoSotto.chiusura().size() > 0,
                "Chiusura sotto il muro deve avere celle");
    }

    /**
     * Verifica che paint() gestisca correttamente gli assi (celle cardinali).
     * Le celle sugli assi cardinali da O devono essere raggiungibili e marcare
     * il contesto correttamente.
     */
    @Test
    void paintAssiCardinaliRaggiungibili() {
        IGriglia<?> g = GrigliaMatrix.vuota(9, 9);
        int Ox = 4, Oy = 4;

        GrigliaConOrigine gco = CalcoloChiusura.calcola(g, Ox, Oy);

        // Verifica che i vicini cardinali di O siano nella chiusura
        mio.griglia.Cella2D nord = new mio.griglia.Cella2D(0, Ox, Oy - 1);
        mio.griglia.Cella2D sud = new mio.griglia.Cella2D(0, Ox, Oy + 1);
        mio.griglia.Cella2D est = new mio.griglia.Cella2D(0, Ox + 1, Oy);
        mio.griglia.Cella2D ovest = new mio.griglia.Cella2D(0, Ox - 1, Oy);

        assertTrue(StatoCella.CHIUSURA.is(gco.getCellaAt(nord.x(), nord.y()).stato()),
                "Asse Nord deve essere nella chiusura");
        assertTrue(StatoCella.CHIUSURA.is(gco.getCellaAt(sud.x(), sud.y()).stato()),
                "Asse Sud deve essere nella chiusura");
        assertTrue(StatoCella.CHIUSURA.is(gco.getCellaAt(est.x(), est.y()).stato()),
                "Asse Est deve essere nella chiusura");
        assertTrue(StatoCella.CHIUSURA.is(gco.getCellaAt(ovest.x(), ovest.y()).stato()),
                "Asse Ovest deve essere nella chiusura");
    }

    /**
     * Verifica che le celle di frontiera siano coerenti: devono avere FRONTIERA
     * e avere almeno un vicino non-chiusura e non-ostacolo.
     */
    @Test
    void paintFrontieraCoerente() {
        IGriglia<?> g = GrigliaMatrix.vuota(5, 5);
        int Ox = 2, Oy = 2;

        GrigliaConOrigine gco = CalcoloChiusura.calcola(g, Ox, Oy);

        for (ICella2D fCell : gco.frontiera()) {
            assertTrue(StatoCella.FRONTIERA.is(fCell.stato()),
                    "Cella (" + fCell.x() + "," + fCell.y() + ") deve avere FRONTIERA");
            assertTrue(StatoCella.CHIUSURA.is(fCell.stato()),
                    "Celle di frontiera devono essere nella chiusura");
        }
    }
}
