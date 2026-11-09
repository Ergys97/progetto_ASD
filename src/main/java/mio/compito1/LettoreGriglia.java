package mio.compito1;

import mio.griglia.*;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.*;
import java.nio.file.*;
import java.util.Objects;
import java.util.StringJoiner;

public class LettoreGriglia {

    /**
     * Carica una griglia da file JSON.
     * Formato: array di array di interi (bitmask stato celle)
     * Es: [[0, 128, 0], [0, 0, 0], ...]
     *
     * @param path percorso del file JSON
     * @return IGriglia con celle caricate da file
     * @throws IOException se il file non esiste o non è leggibile
     */
    public static IGriglia<?> carica(String path) throws IOException {
        Path inputPath = validaPath(path);
        try {
            String content = Files.readString(inputPath);
            JSONArray rows = new JSONArray(content);
            int height = rows.length();
            if (height == 0) {
                throw new IOException("La griglia JSON non puo essere vuota");
            }
            int width = rows.getJSONArray(0).length();
            if (width == 0) {
                throw new IOException("Le righe della griglia JSON non possono essere vuote");
            }

            ICella[][] mat = new ICella[height][width];
            for (int y = 0; y < height; y++) {
                JSONArray row = rows.getJSONArray(y);
                if (row.length() != width) {
                    throw new IOException("La griglia JSON deve essere rettangolare: riga " + y);
                }
                for (int x = 0; x < width; x++) {
                    int stato = row.getInt(x);
                    if (stato < 0 || stato > 0xFF) {
                        throw new IOException(
                                "Stato cella fuori dalla bitmask a 8 bit in (" + x + "," + y + ")");
                    }
                    mat[y][x] = new Cella(stato);
                }
            }
            return new GrigliaMatrix(mat);
        } catch (JSONException e) {
            throw new IOException("JSON della griglia non valido: " + inputPath, e);
        }
    }

    /**
     * Salva una griglia in formato JSON.
     * Formato: array di array di interi (bitmask stato celle)
     * Es: [[0, 128, 0], [0, 0, 0], ...]
     *
     * @param g griglia da salvare
     * @param path percorso di destinazione del file JSON
     * @throws IOException se non è possibile scrivere il file
     */
    public static void salva(IGriglia<?> g, String path) throws IOException {
        Objects.requireNonNull(g, "La griglia non puo essere null");
        if (g.width() <= 0 || g.height() <= 0) {
            throw new IllegalArgumentException("La griglia deve avere dimensioni positive");
        }
        int larghezzaCampo = 1;
        for (int y = g.ymin(); y <= g.ymax(); y++) {
            for (int x = g.xmin(); x <= g.xmax(); x++) {
                larghezzaCampo = Math.max(larghezzaCampo,
                        Integer.toString(g.getCellaAt(x, y).stato()).length());
            }
        }
        String formato = "%" + larghezzaCampo + "d";
        StringBuilder rows = new StringBuilder();
        rows.append("[\n");
        for (int y = g.ymin(); y <= g.ymax(); y++) {
            StringJoiner row = new StringJoiner(", ", "  [", "]");
            for (int x = g.xmin(); x <= g.xmax(); x++) {
                row.add(String.format(formato, g.getCellaAt(x, y).stato()));
            }
            rows.append(row);
            if (y < g.ymax()) {
                rows.append(',');
            }
            rows.append("\n");
        }
        rows.append("]\n");

        Path outputPath = validaPath(path);
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputPath, rows.toString());
    }

    private static Path validaPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Il percorso del file non puo essere vuoto");
        }
        return Path.of(path);
    }
}
