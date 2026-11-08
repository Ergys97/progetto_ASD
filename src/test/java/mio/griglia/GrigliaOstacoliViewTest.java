package mio.griglia;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GrigliaOstacoliViewTest {

    @Test
    void ostacoliEIsOstacoloConcordanoConGetCellaAt() {
        ICella[][] m = new ICella[3][4];
        for (ICella[] riga : m) java.util.Arrays.fill(riga, new Cella(0));
        m[1][2] = new Cella(IGriglia.BIT_OSTACOLO);
        IGriglia<?> base = new GrigliaMatrix(m);

        IGriglia<?> v1 = base.addObstacle(() -> List.of(new Cella2D(0, 0, 0)));
        IGriglia<?> v2 = v1.addObstacle(() -> List.of(new Cella2D(0, 3, 2), new Cella2D(0, 2, 1)));

        for (IGriglia<?> g : List.of(base, v1, v2)) {
            BitSet atteso = new BitSet();
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 4; x++) {
                    boolean ost = (g.getCellaAt(x, y).stato() & IGriglia.BIT_OSTACOLO) != 0;
                    assertEquals(ost, g.isOstacolo(x, y), "(" + x + "," + y + ")");
                    if (ost) atteso.set(y * 4 + x);
                }
            }
            assertEquals(atteso, g.ostacoli());
        }
        assertEquals(1, base.ostacoli().cardinality());
        assertEquals(2, v1.ostacoli().cardinality());
        assertEquals(3, v2.ostacoli().cardinality());
    }

    @Test
    void aggiungereOstacoliNonModificaLaVistaDiPartenza() {
        IGriglia<?> v1 = GrigliaMatrix.vuota(3, 3).addObstacle(() -> List.of(new Cella2D(0, 1, 1)));
        BitSet prima = (BitSet) v1.ostacoli().clone();
        v1.addObstacle(() -> List.of(new Cella2D(0, 0, 0), new Cella2D(0, 2, 2)));
        assertEquals(prima, v1.ostacoli());
        assertFalse(v1.isOstacolo(0, 0));
    }
}
