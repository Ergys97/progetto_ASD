package mio.compito2;

import mio.griglia.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record GrigliaConOrigine(
        int[][] mat,
        int Ox, int Oy,
        int xmin, int ymin,
        List<ICella2D> chiusura,
        ICella2D[] frontiera
) implements IGriglia<ICella2D> {

    public GrigliaConOrigine {
        mat = copiaValidata(mat);
        long xmax = (long) xmin + mat[0].length - 1L;
        long ymax = (long) ymin + mat.length - 1L;
        if (xmax > Integer.MAX_VALUE || ymax > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Le coordinate della griglia superano il range int");
        }
        if (Ox < xmin || Ox > xmax || Oy < ymin || Oy > ymax) {
            throw new IllegalArgumentException("Origine fuori griglia: (" + Ox + "," + Oy + ")");
        }
        chiusura = List.copyOf(Objects.requireNonNull(chiusura, "La chiusura non puo essere null"));
        frontiera = Objects.requireNonNull(frontiera, "La frontiera non puo essere null").clone();
    }

    @Override public int xmin() { return xmin; }
    @Override public int ymin() { return ymin; }
    @Override public int width()  { return mat[0].length; }
    @Override public int height() { return mat.length; }

    @Override
    public ICella2D getCellaAt(int x, int y) {
        if (x < xmin() || x > xmax() || y < ymin() || y > ymax()) {
            throw new IndexOutOfBoundsException("Coordinate fuori griglia: (" + x + "," + y + ")");
        }
        return new Cella2D(mat[y - ymin][x - xmin], x, y);
    }

    @Override
    public int[][] mat() {
        return copia(mat);
    }

    @Override
    public ICella2D[] frontiera() {
        return frontiera.clone();
    }

    @Override
    public IGriglia<?> addObstacle(IObstacle o) {
        Objects.requireNonNull(o, "L'ostacolo non puo essere null");
        int[][] newMat = new int[height()][width()];
        for (int r = 0; r < height(); r++)
            newMat[r] = Arrays.copyOf(mat[r], width());
        for (ICella2D cell : o.list()) {
            Objects.requireNonNull(cell, "Una cella ostacolo non puo essere null");
            int row = cell.y() - ymin;
            int col = cell.x() - xmin;
            if (row < 0 || row >= height() || col < 0 || col >= width()) {
                throw new IllegalArgumentException(
                        "Ostacolo fuori griglia: (" + cell.x() + "," + cell.y() + ")");
            }
            newMat[row][col] |= StatoCella.OSTACOLO.value;
        }
        return new GrigliaConOrigine(newMat, Ox, Oy, xmin, ymin, List.of(), new ICella2D[0]);
    }

    public IObstacle convertiChiusuraInOstacolo() {
        return () -> chiusura;
    }

    private static int[][] copiaValidata(int[][] source) {
        Objects.requireNonNull(source, "La matrice non puo essere null");
        if (source.length == 0) {
            throw new IllegalArgumentException("La matrice deve avere almeno una riga");
        }
        Objects.requireNonNull(source[0], "La riga 0 non puo essere null");
        int width = source[0].length;
        if (width == 0) {
            throw new IllegalArgumentException("La matrice deve avere almeno una colonna");
        }
        int[][] copy = new int[source.length][width];
        for (int y = 0; y < source.length; y++) {
            int[] row = Objects.requireNonNull(source[y], "La riga " + y + " non puo essere null");
            if (row.length != width) {
                throw new IllegalArgumentException("La matrice deve essere rettangolare");
            }
            copy[y] = Arrays.copyOf(row, width);
        }
        return copy;
    }

    private static int[][] copia(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int y = 0; y < source.length; y++) {
            copy[y] = Arrays.copyOf(source[y], source[y].length);
        }
        return copy;
    }
}
