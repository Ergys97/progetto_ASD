package mio.griglia;

import java.util.BitSet;

/**
 * Vista immutabile di una griglia base con ostacoli aggiuntivi.
 *
 * La vista conserva l'insieme completo degli ostacoli (quelli della base e quelli
 * aggiunti) in un BitSet: {@link #isOstacolo} costa O(1) senza allocazioni e
 * {@link #ostacoli()} restituisce lo stesso BitSet, usato come chiave dalla cache
 * di CamminoMin. Il BitSet non viene mai modificato dopo la costruzione.
 */
public final class GrigliaOstacoliView implements IGriglia<ICella> {

    private final IGriglia<?> base;
    private final BitSet ostacoli;
    private final int xmin;
    private final int ymin;
    private final int width;
    private final int height;

    private GrigliaOstacoliView(IGriglia<?> base, BitSet ostacoli) {
        this.base = base;
        this.ostacoli = ostacoli;
        this.xmin = base.xmin();
        this.ymin = base.ymin();
        this.width = base.width();
        this.height = base.height();
    }

    public static IGriglia<?> withAddedObstacle(IGriglia<?> griglia, IObstacle obstacle) {
        IGriglia<?> rootBase;
        BitSet merged;

        if (griglia instanceof GrigliaOstacoliView view) {
            rootBase = view.base;
            merged = (BitSet) view.ostacoli.clone();
        } else {
            rootBase = griglia;
            merged = (BitSet) griglia.ostacoli().clone();
        }

        int x0 = griglia.xmin();
        int y0 = griglia.ymin();
        int w = griglia.width();
        int h = griglia.height();

        for (ICella2D c : obstacle.list()) {
            int rx = c.x() - x0;
            int ry = c.y() - y0;
            if (rx < 0 || rx >= w || ry < 0 || ry >= h) {
                continue;
            }
            merged.set(index(rx, ry, w));
        }

        return new GrigliaOstacoliView(rootBase, merged);
    }

    @Override
    public ICella getCellaAt(int x, int y) {
        ICella baseCell = base.getCellaAt(x, y);
        if (ostacoli.get(index(x - xmin, y - ymin, width))
                && (baseCell.stato() & BIT_OSTACOLO) == 0) {
            return new Cella(baseCell.stato() | BIT_OSTACOLO);
        }
        return baseCell;
    }

    @Override
    public boolean isOstacolo(int x, int y) {
        if (x < xmin || x > xmax() || y < ymin || y > ymax()) {
            throw new IndexOutOfBoundsException("Coordinate fuori griglia: (" + x + "," + y + ")");
        }
        return ostacoli.get(index(x - xmin, y - ymin, width));
    }

    /** Insieme completo degli ostacoli; non va modificato. */
    @Override
    public BitSet ostacoli() {
        return ostacoli;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public int xmin() {
        return xmin;
    }

    @Override
    public int ymin() {
        return ymin;
    }

    @Override
    public IGriglia<?> addObstacle(IObstacle o) {
        return withAddedObstacle(this, o);
    }

    private static int index(int rx, int ry, int width) {
        return ry * width + rx;
    }
}
