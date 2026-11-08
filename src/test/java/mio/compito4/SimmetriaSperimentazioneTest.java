package mio.compito4;

import mio.compito1.GeneratoreGriglia;
import mio.compito2.DistanzaLibera;
import mio.compito2.StatoCella;
import mio.compito3.Cammino;
import mio.compito3.CamminoMin;
import mio.griglia.Cella2D;
import mio.griglia.ICella2D;
import mio.griglia.IGriglia;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SimmetriaSperimentazioneTest {

    private static ICella2D trovaCellaLibera(IGriglia<?> g, int cx, int cy) {
        int x0 = g.xmin();
        int y0 = g.ymin();
        int xmax = g.xmin() + g.width() - 1;
        int ymax = g.ymin() + g.height() - 1;

        for (int r = 0; r <= Math.max(g.width(), g.height()); r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) {
                        continue;
                    }
                    int x = cx + dx;
                    int y = cy + dy;
                    if (x < x0 || x > xmax || y < y0 || y > ymax) {
                        continue;
                    }
                    if (StatoCella.OSTACOLO.isNot(g.getCellaAt(x, y).stato())) {
                        return new Cella2D(0, x, y);
                    }
                }
            }
        }

        throw new IllegalStateException("Nessuna cella libera nella griglia");
    }

    @Test
    void simmetriaSuPiuSeed() {
        int width = 20;
        int height = 20;
        int ostacoli = 60;

        for (long seed = 1; seed <= 30; seed++) {
            IGriglia<?> g = GeneratoreGriglia.genera(width, height, ostacoli, seed);
            ICella2D O = trovaCellaLibera(g, width / 2, height / 2);
            ICella2D D = trovaCellaLibera(g, width - 2, height - 2);

            Cammino od = CamminoMin.calcola(g, O, D);
            Cammino dO = CamminoMin.calcola(g, D, O);

            double lOD = od.distanza().distanza();
            double lDO = dO.distanza().distanza();
            boolean simmetrico =
                    (Double.isInfinite(lOD) && Double.isInfinite(lDO))
                            || Math.abs(lOD - lDO) <= DistanzaLibera.EPSILON;

            assertTrue(
                    simmetrico,
                    "Simmetria violata con seed=" + seed + " (OD=" + lOD + ", DO=" + lDO + ")"
            );
        }
    }
}
