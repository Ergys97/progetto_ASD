package mio.compito1;

import mio.griglia.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeneratoreGriglia {

    /**
     * Genera una griglia width x height con esattamente nOstacoli ostacoli in
     * posizioni casuali, riproducibili tramite il seed.
     *
     * Le posizioni vengono estratte con rigetto delle celle gia occupate e
     * aggiunte alla griglia con un'unica vista: il costo e O(W*H + estrazioni).
     */
    public static IGriglia<?> genera(int width, int height, int nOstacoli, long seed) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Dimensioni griglia non valide");
        }
        long celleTotali = (long) width * height;
        if (nOstacoli < 0 || nOstacoli > celleTotali) {
            throw new IllegalArgumentException("Numero ostacoli non valido");
        }

        Random rng = new Random(seed);
        boolean[][] occupato = new boolean[height][width];
        List<ICella2D> ostacoli = new ArrayList<>(nOstacoli);

        while (ostacoli.size() < nOstacoli) {
            int x = rng.nextInt(width);
            int y = rng.nextInt(height);
            if (!occupato[y][x]) {
                occupato[y][x] = true;
                ostacoli.add(new Cella2D(0, x, y));
            }
        }

        IGriglia<?> griglia = GrigliaMatrix.vuota(width, height);
        if (ostacoli.isEmpty()) {
            return griglia;
        }
        List<ICella2D> elenco = List.copyOf(ostacoli);
        return griglia.addObstacle(() -> elenco);
    }
}
