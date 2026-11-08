package mio.griglia;

import java.util.BitSet;

public interface IGriglia<C extends ICella> {

    /** Bit dello stato che marca una cella non attraversabile (coincide con StatoCella.OSTACOLO). */
    int BIT_OSTACOLO = 0b10000000;

    C getCellaAt(int x, int y);
    int width();
    int height();
    default int xmin() { return 0; }
    default int ymin() { return 0; }
    default int xmax() { return xmin() + width() - 1; }
    default int ymax() { return ymin() + height() - 1; }
    IGriglia<?> addObstacle(IObstacle o);

    /** true se la cella (x, y) e un ostacolo. */
    default boolean isOstacolo(int x, int y) {
        return (getCellaAt(x, y).stato() & BIT_OSTACOLO) != 0;
    }

    /**
     * Insieme delle celle ostacolo, con indice {@code (y - ymin) * width + (x - xmin)}.
     * Il BitSet restituito non deve essere modificato: le implementazioni possono
     * restituire la propria copia interna.
     */
    default BitSet ostacoli() {
        int w = width();
        BitSet b = new BitSet(w * height());
        for (int y = ymin(); y <= ymax(); y++)
            for (int x = xmin(); x <= xmax(); x++)
                if (isOstacolo(x, y)) b.set((y - ymin()) * w + (x - xmin()));
        return b;
    }
}
