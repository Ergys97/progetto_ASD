package mio.griglia;

import java.util.Arrays;
import java.util.Objects;

public record GrigliaMatrix(ICella[][] mat) implements IGriglia<ICella> {

    public GrigliaMatrix {
        mat = copiaValidata(mat);
    }

    public static GrigliaMatrix vuota(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Le dimensioni della griglia devono essere positive");
        }
        ICella[][] m = new ICella[height][width];
        for (ICella[] row : m) Arrays.fill(row, new Cella(0));
        return new GrigliaMatrix(m);
    }

    @Override
    public ICella getCellaAt(int x, int y) {
        if (x < xmin() || x > xmax() || y < ymin() || y > ymax()) {
            throw new IndexOutOfBoundsException("Coordinate fuori griglia: (" + x + "," + y + ")");
        }
        return mat[y][x];
    }

    @Override
    public ICella[][] mat() {
        return copia(mat);
    }

    @Override
    public int width() { return mat[0].length; }

    @Override
    public int height() { return mat.length; }

    @Override
    public IGriglia<?> addObstacle(IObstacle o) {
        Objects.requireNonNull(o, "L'ostacolo non puo essere null");
        return GrigliaOstacoliView.withAddedObstacle(this, o);
    }

    private static ICella[][] copiaValidata(ICella[][] source) {
        Objects.requireNonNull(source, "La matrice non puo essere null");
        if (source.length == 0) {
            throw new IllegalArgumentException("La matrice deve avere almeno una riga");
        }
        Objects.requireNonNull(source[0], "La riga 0 non puo essere null");
        int width = source[0].length;
        if (width == 0) {
            throw new IllegalArgumentException("La matrice deve avere almeno una colonna");
        }
        ICella[][] copy = new ICella[source.length][width];
        for (int y = 0; y < source.length; y++) {
            ICella[] row = Objects.requireNonNull(source[y], "La riga " + y + " non puo essere null");
            if (row.length != width) {
                throw new IllegalArgumentException("La matrice deve essere rettangolare");
            }
            for (int x = 0; x < width; x++) {
                copy[y][x] = Objects.requireNonNull(
                        row[x], "La cella (" + x + "," + y + ") non puo essere null");
            }
        }
        return copy;
    }

    private static ICella[][] copia(ICella[][] source) {
        ICella[][] copy = new ICella[source.length][];
        for (int y = 0; y < source.length; y++) {
            copy[y] = Arrays.copyOf(source[y], source[y].length);
        }
        return copy;
    }
}
