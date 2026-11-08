package mio.compito2;

import mio.griglia.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CalcoloChiusura {

    public static GrigliaConOrigine calcola(IGriglia<?> griglia, int Ox, int Oy) {
        Objects.requireNonNull(griglia, "La griglia non puo essere null");
        int w = griglia.width();
        int h = griglia.height();
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("La griglia deve avere dimensioni positive");
        }
        int x0 = griglia.xmin();
        int y0 = griglia.ymin();
        if (Ox < x0 || Ox > griglia.xmax() || Oy < y0 || Oy > griglia.ymax()) {
            throw new IllegalArgumentException(
                    "Origine fuori griglia: (" + Ox + "," + Oy + ")");
        }
        int[][] res = new int[h][w];

        // Pre-inizializza res con gli ostacoli della griglia originale
        // (necessario per il corretto calcolo della frontiera)
        for (int y = y0; y <= griglia.ymax(); y++)
            for (int x = x0; x <= griglia.xmax(); x++) {
                if (griglia.isOstacolo(x, y)) {
                    // Una nuova elaborazione non deve ereditare bit transienti
                    // (contesto, complemento o frontiera) da elaborazioni precedenti.
                    res[y - y0][x - x0] = StatoCella.OSTACOLO.value;
                }
            }

        // Se O è un ostacolo nessun cammino può partire da lì: chiusura e frontiera vuote.
        if (griglia.isOstacolo(Ox, Oy)) {
            return new GrigliaConOrigine(res, Ox, Oy, x0, y0, List.of(), new ICella2D[0]);
        }

        // Paint nelle 8 direzioni con le corrette direzioni laterali:
        // Cardinali (N/S/E/W): laterali = le due diagonali nella stessa direzione primaria → COMPLEMENTO
        // Diagonali (NE/NW/SE/SW): laterali = le due cardinali componenti → CONTESTO
        paint(griglia, res, Ox, Oy,  0, -1, x0, y0, StatoCella.COMPLEMENTO, true, -1,-1,  1,-1); // N → NW,NE
        paint(griglia, res, Ox, Oy,  0,  1, x0, y0, StatoCella.COMPLEMENTO, true, -1, 1,  1, 1); // S → SW,SE
        paint(griglia, res, Ox, Oy,  1,  0, x0, y0, StatoCella.COMPLEMENTO, true,  1,-1,  1, 1); // E → NE,SE
        paint(griglia, res, Ox, Oy, -1,  0, x0, y0, StatoCella.COMPLEMENTO, true, -1,-1, -1, 1); // W → NW,SW
        paint(griglia, res, Ox, Oy,  1,  1, x0, y0, StatoCella.CONTESTO,    true,  1, 0,  0, 1); // SE → E,S
        paint(griglia, res, Ox, Oy,  1, -1, x0, y0, StatoCella.CONTESTO,    true,  1, 0,  0,-1); // NE → E,N
        paint(griglia, res, Ox, Oy, -1, -1, x0, y0, StatoCella.CONTESTO,    true, -1, 0,  0,-1); // NW → W,N
        paint(griglia, res, Ox, Oy, -1,  1, x0, y0, StatoCella.CONTESTO,    true, -1, 0,  0, 1); // SW → W,S

        // Il complemento è, per definizione, esclusivo: se una cella è raggiunta
        // anche da un cammino di tipo 1 resta soltanto nel contesto.
        for (int y = y0; y <= griglia.ymax(); y++)
            for (int x = x0; x <= griglia.xmax(); x++) {
                int stato = res[y - y0][x - x0];
                if (StatoCella.CONTESTO.is(stato))
                    res[y - y0][x - x0] = StatoCella.COMPLEMENTO.removeTo(stato);
            }

        // Segna origine e la include esplicitamente nella chiusura.
        res[Oy - y0][Ox - x0] = StatoCella.CONTESTO.addTo(
                StatoCella.ORIGINE.addTo(res[Oy - y0][Ox - x0]));

        // Raccogli chiusura (celle con bit CONTESTO o COMPLEMENTO)
        List<ICella2D> chiusuraProvvisoria = new ArrayList<>();
        for (int y = y0; y <= griglia.ymax(); y++)
            for (int x = x0; x <= griglia.xmax(); x++)
                if (StatoCella.CHIUSURA.is(res[y - y0][x - x0])
                        && StatoCella.OSTACOLO.isNot(res[y - y0][x - x0]))
                    chiusuraProvvisoria.add(new Cella2D(res[y - y0][x - x0], x, y));

        // Post-processing frontiera
        int[][] dirs8 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1},
                         {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        List<ICella2D> frontieraList = new ArrayList<>();
        for (ICella2D c : chiusuraProvvisoria) {
            boolean isFrontiera = false;
            for (int[] d : dirs8) {
                int nx = c.x() + d[0], ny = c.y() + d[1];
                // La frontiera è definita rispetto a celle reali della griglia:
                // l'esterno non è una cella attraversabile.
                if (nx < x0 || nx > griglia.xmax() || ny < y0 || ny > griglia.ymax()) {
                    continue;
                }
                int nStato = res[ny - y0][nx - x0];
                if (StatoCella.CHIUSURA.isNot(nStato) && StatoCella.OSTACOLO.isNot(nStato)) {
                    isFrontiera = true;
                    break;
                }
            }
            if (isFrontiera) {
                int s = StatoCella.FRONTIERA.addTo(res[c.y() - y0][c.x() - x0]);
                res[c.y() - y0][c.x() - x0] = s;
                frontieraList.add(new Cella2D(s, c.x(), c.y()));
            }
        }

        // Ricostruisce gli snapshot dopo il post-processing, cosi le celle
        // presenti sia in chiusura sia in frontiera espongono la stessa bitmask.
        List<ICella2D> chiusura = new ArrayList<>(chiusuraProvvisoria.size());
        for (ICella2D c : chiusuraProvvisoria) {
            chiusura.add(new Cella2D(res[c.y() - y0][c.x() - x0], c.x(), c.y()));
        }

        return new GrigliaConOrigine(res, Ox, Oy, x0, y0, chiusura,
                frontieraList.toArray(new ICella2D[0]));
    }

    /**
     * Percorre in linea retta da (x+dx, y+dy) nella direzione (dx, dy).
     * Con recursion=true marca la linea come REGINA e lancia due paint laterali non ricorsivi.
     * La linea primaria è sempre un cammino di tipo 1; le linee laterali
     * ricevono lineClosureMask (CONTESTO o COMPLEMENTO in base al raggruppamento).
     */
    private static void paint(IGriglia<?> griglia, int[][] res,
                               int x, int y, int dx, int dy,
                               int xmin, int ymin,
                               StatoCella lineClosureMask,
                               boolean recursion,
                               int ldx1, int ldy1, int ldx2, int ldy2) {
        int cx = x + dx, cy = y + dy;
        while (cx >= griglia.xmin() && cx <= griglia.xmax()
                && cy >= griglia.ymin() && cy <= griglia.ymax()) {
            if (griglia.isOstacolo(cx, cy)) break;

            int stato = (recursion ? StatoCella.CONTESTO : lineClosureMask)
                    .addTo(res[cy - ymin][cx - xmin]);
            if (recursion) {
                stato = StatoCella.REGINA.addTo(stato);
            }
            res[cy - ymin][cx - xmin] = stato;

            if (recursion) {
                paint(griglia, res, cx, cy, ldx1, ldy1, xmin, ymin,
                        lineClosureMask, false, 0, 0, 0, 0);
                paint(griglia, res, cx, cy, ldx2, ldy2, xmin, ymin,
                        lineClosureMask, false, 0, 0, 0, 0);
            }

            cx += dx;
            cy += dy;
        }
    }
}
