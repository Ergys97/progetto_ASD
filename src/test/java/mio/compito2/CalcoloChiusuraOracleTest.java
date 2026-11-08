package mio.compito2;

import mio.griglia.Cella2D;
import mio.griglia.GrigliaMatrix;
import mio.griglia.ICella2D;
import mio.griglia.IGriglia;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalcoloChiusuraOracleTest {

    private static final int[][] DIREZIONI_8 = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    @Test
    void contestoComplementoEFrontieraCoincidonoConEnumerazioneEsaustivaTrePerTre() {
        int width = 3;
        int height = 3;

        for (int maskOstacoli = 0; maskOstacoli < (1 << (width * height)); maskOstacoli++) {
            IGriglia<?> griglia = grigliaDaMask(width, height, maskOstacoli);
            for (int oy = 0; oy < height; oy++) {
                for (int ox = 0; ox < width; ox++) {
                    if (ostacolo(griglia, ox, oy)) continue;
                    verificaOrigine(griglia, ox, oy, maskOstacoli);
                }
            }
        }
    }

    private static void verificaOrigine(IGriglia<?> griglia, int ox, int oy, int maskOstacoli) {
        GrigliaConOrigine ottenuta = CalcoloChiusura.calcola(griglia, ox, oy);
        boolean[][] chiusuraAttesa = new boolean[griglia.height()][griglia.width()];
        int numeroChiusuraAtteso = 0;

        for (int y = griglia.ymin(); y <= griglia.ymax(); y++) {
            for (int x = griglia.xmin(); x <= griglia.xmax(); x++) {
                boolean tipo1 = camminoOrdinatoLibero(griglia, ox, oy, x, y, true);
                boolean tipo2 = camminoOrdinatoLibero(griglia, ox, oy, x, y, false);
                boolean soloTipo2 = !tipo1 && tipo2;
                int stato = ottenuta.getCellaAt(x, y).stato();
                String caso = caso(maskOstacoli, ox, oy, x, y);

                assertEquals(tipo1, StatoCella.CONTESTO.is(stato), "CONTESTO: " + caso);
                assertEquals(soloTipo2, StatoCella.COMPLEMENTO.is(stato), "COMPLEMENTO: " + caso);
                assertEquals(tipo1 || tipo2, StatoCella.CHIUSURA.is(stato), "CHIUSURA: " + caso);

                chiusuraAttesa[y - griglia.ymin()][x - griglia.xmin()] = tipo1 || tipo2;
                if (tipo1 || tipo2) numeroChiusuraAtteso++;
            }
        }

        int numeroFrontieraAtteso = 0;
        for (int y = griglia.ymin(); y <= griglia.ymax(); y++) {
            for (int x = griglia.xmin(); x <= griglia.xmax(); x++) {
                boolean frontieraAttesa = isFrontiera(griglia, chiusuraAttesa, x, y);
                int stato = ottenuta.getCellaAt(x, y).stato();
                assertEquals(frontieraAttesa, StatoCella.FRONTIERA.is(stato),
                        "FRONTIERA: " + caso(maskOstacoli, ox, oy, x, y));
                if (frontieraAttesa) numeroFrontieraAtteso++;
            }
        }

        assertEquals(numeroChiusuraAtteso, ottenuta.chiusura().size(),
                "Numero celle chiusura, mask=" + maskOstacoli + ", O=(" + ox + ',' + oy + ')');
        assertEquals(numeroFrontieraAtteso, ottenuta.frontiera().length,
                "Numero celle frontiera, mask=" + maskOstacoli + ", O=(" + ox + ',' + oy + ')');
    }

    private static boolean isFrontiera(IGriglia<?> griglia, boolean[][] chiusura, int x, int y) {
        if (!chiusura[y - griglia.ymin()][x - griglia.xmin()]) return false;
        for (int[] direzione : DIREZIONI_8) {
            int nx = x + direzione[0];
            int ny = y + direzione[1];
            if (nx < griglia.xmin() || nx > griglia.xmax()
                    || ny < griglia.ymin() || ny > griglia.ymax()) continue;
            if (!chiusura[ny - griglia.ymin()][nx - griglia.xmin()]
                    && !ostacolo(griglia, nx, ny)) return true;
        }
        return false;
    }

    private static boolean camminoOrdinatoLibero(IGriglia<?> griglia,
                                                  int ox, int oy, int dx, int dy,
                                                  boolean diagonaliPrima) {
        if (ostacolo(griglia, dx, dy)) return false;

        int passoX = Integer.signum(dx - ox);
        int passoY = Integer.signum(dy - oy);
        int diagonali = Math.min(Math.abs(dx - ox), Math.abs(dy - oy));
        int cardinaliX = Math.abs(dx - ox) - diagonali;
        int cardinaliY = Math.abs(dy - oy) - diagonali;
        int x = ox;
        int y = oy;

        if (diagonaliPrima) {
            for (int i = 0; i < diagonali; i++) {
                x += passoX;
                y += passoY;
                if (ostacolo(griglia, x, y)) return false;
            }
        }
        for (int i = 0; i < cardinaliX; i++) {
            x += passoX;
            if (ostacolo(griglia, x, y)) return false;
        }
        for (int i = 0; i < cardinaliY; i++) {
            y += passoY;
            if (ostacolo(griglia, x, y)) return false;
        }
        if (!diagonaliPrima) {
            for (int i = 0; i < diagonali; i++) {
                x += passoX;
                y += passoY;
                if (ostacolo(griglia, x, y)) return false;
            }
        }
        return x == dx && y == dy;
    }

    private static IGriglia<?> grigliaDaMask(int width, int height, int maskOstacoli) {
        List<ICella2D> ostacoli = new ArrayList<>();
        for (int indice = 0; indice < width * height; indice++) {
            if ((maskOstacoli & (1 << indice)) != 0) {
                ostacoli.add(new Cella2D(0, indice % width, indice / width));
            }
        }
        return GrigliaMatrix.vuota(width, height).addObstacle(() -> ostacoli);
    }

    private static boolean ostacolo(IGriglia<?> griglia, int x, int y) {
        return StatoCella.OSTACOLO.is(griglia.getCellaAt(x, y).stato());
    }

    private static String caso(int mask, int ox, int oy, int x, int y) {
        return "mask=" + mask + ", O=(" + ox + ',' + oy + "), cella=(" + x + ',' + y + ')';
    }
}
