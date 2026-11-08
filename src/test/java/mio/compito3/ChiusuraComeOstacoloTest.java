package mio.compito3;

import mio.compito2.CalcoloChiusura;
import mio.compito2.GrigliaConOrigine;
import mio.compito2.StatoCella;
import mio.griglia.Cella2D;
import mio.griglia.GrigliaMatrix;
import mio.griglia.ICella2D;
import mio.griglia.IGriglia;
import mio.griglia.IObstacle;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChiusuraComeOstacoloTest {

    @Test
    void nuovaChiusuraNonRiusaCelleDellaChiusuraPrecedente() {
        IGriglia<?> base = GrigliaMatrix.vuota(7, 7);
        ICella2D O = new Cella2D(0, 3, 3);

        IObstacle bloccoAttornoAOrigine = () -> List.of(
                new Cella2D(0, 2, 2), new Cella2D(0, 3, 2), new Cella2D(0, 4, 2),
                new Cella2D(0, 2, 3),                         new Cella2D(0, 4, 3),
                new Cella2D(0, 2, 4), new Cella2D(0, 3, 4), new Cella2D(0, 4, 4)
        );
        IGriglia<?> conMuro = base.addObstacle(bloccoAttornoAOrigine);

        GrigliaConOrigine chiusuraO = CalcoloChiusura.calcola(conMuro, O.x(), O.y());
        IGriglia<?> g2 = conMuro.addObstacle(chiusuraO.convertiChiusuraInOstacolo());

        // Tutta la chiusura precedente deve essere marcata come ostacolo in g2.
        for (ICella2D c : chiusuraO.chiusura()) {
            assertTrue(StatoCella.OSTACOLO.is(g2.getCellaAt(c.x(), c.y()).stato()));
        }

        // Simula livello successivo: nuova origine fuori dalla chiusura precedente.
        ICella2D F = new Cella2D(0, 0, 0);
        GrigliaConOrigine chiusuraF = CalcoloChiusura.calcola(g2, F.x(), F.y());

        Set<String> prev = new HashSet<>();
        for (ICella2D c : chiusuraO.chiusura()) {
            prev.add(c.x() + "," + c.y());
        }

        for (ICella2D c : chiusuraF.chiusura()) {
            assertTrue(!prev.contains(c.x() + "," + c.y()));
        }
    }
}
