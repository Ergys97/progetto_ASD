package mio.compito1;

import mio.griglia.Cella2D;
import mio.griglia.GrigliaMatrix;
import mio.griglia.IGriglia;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GeneratoreGrigliaRiproducibilitaTest {

    /**
     * Versione precedente del generatore (una vista per ogni ostacolo): le griglie
     * pubblicate in risultati/griglie_json devono restare identiche.
     */
    private static IGriglia<?> generaUnoAllaVolta(int w, int h, int n, long seed) {
        Random rng = new Random(seed);
        boolean[][] occupato = new boolean[h][w];
        IGriglia<?> g = GrigliaMatrix.vuota(w, h);
        int piazzati = 0;
        while (piazzati < n) {
            int x = rng.nextInt(w);
            int y = rng.nextInt(h);
            if (!occupato[y][x]) {
                occupato[y][x] = true;
                final int fx = x, fy = y;
                g = g.addObstacle(() -> List.of(new Cella2D(0, fx, fy)));
                piazzati++;
            }
        }
        return g;
    }

    @Test
    void generaProduceGliStessiOstacoliDellaVersionePrecedente() {
        int[][] casi = {{12, 12, 15, 50}, {20, 20, 80, 100}, {30, 30, 45, 202}, {50, 50, 50, 301},
                {5, 5, 23, 1}, {7, 3, 0, 9}, {4, 4, 16, 3}};
        for (int[] c : casi) {
            IGriglia<?> nuova = GeneratoreGriglia.genera(c[0], c[1], c[2], c[3]);
            IGriglia<?> vecchia = generaUnoAllaVolta(c[0], c[1], c[2], c[3]);
            assertEquals(vecchia.ostacoli(), nuova.ostacoli(), "caso " + c[0] + "x" + c[1]);
            assertEquals(c[2], nuova.ostacoli().cardinality());
            for (int y = 0; y < c[1]; y++)
                for (int x = 0; x < c[0]; x++)
                    assertEquals(vecchia.getCellaAt(x, y).stato(), nuova.getCellaAt(x, y).stato());
        }
    }
}
