package mio.compito3;

import mio.compito1.GeneratoreGriglia;
import mio.compito2.StatoCella;
import mio.griglia.Cella2D;
import mio.griglia.ICella2D;
import mio.griglia.IGriglia;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImmutabilitaGrigliaRicorsioneTest {

    @Test
    void camminoMinNonModificaLaGrigliaOriginale() {
        IGriglia<?> g = GeneratoreGriglia.genera(12, 12, 20, 123L);
        ICella2D O = new Cella2D(0, 1, 1);
        ICella2D D = new Cella2D(0, 10, 10);

        int ostacoliPrima = contaOstacoli(g);
        int statoODPrima = g.getCellaAt(O.x(), D.y()).stato();

        Cammino risultato = CamminoMin.calcola(g, O, D);

        int ostacoliDopo = contaOstacoli(g);
        int statoODDopo = g.getCellaAt(O.x(), D.y()).stato();

        assertEquals(ostacoliPrima, ostacoliDopo);
        assertEquals(statoODPrima, statoODDopo);
        assertTrue(risultato != null);
    }

    private static int contaOstacoli(IGriglia<?> g) {
        int count = 0;
        for (int y = g.ymin(); y <= g.ymax(); y++) {
            for (int x = g.xmin(); x <= g.xmax(); x++) {
                if (StatoCella.OSTACOLO.is(g.getCellaAt(x, y).stato())) {
                    count++;
                }
            }
        }
        return count;
    }
}
