package mio.compito4;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class ScritturaFile {

    /** Intestazione di esperimenti.csv: una riga per istanza e variante. */
    public static final String INTESTAZIONE_CSV = String.join(",",
            "gridId", "variante", "tipoGriglia", "width", "height", "nOstacoli", "seed",
            "Ox", "Oy", "Dx", "Dy",
            "lunghezzaOD", "lunghezzaDO", "sequenzaLandmarkOD", "sequenzaLandmarkDO",
            "correttezza", "frontieraTot", "falsiRiga16", "falsiRiga17", "pruningTotale",
            "chiamateRicorsive", "cacheHit", "profonditaMassima", "chiusureAttivePicco",
            "celleChiusuraTotali", "memoriaMatriciPiccoBytesStimata",
            "warmupEseguiti", "ripetizioniTempo", "tempoMinNs", "tempoMedianoNs", "tempoMaxNs");

    public static void salvaCsv(List<StatisticheEsecuzione> stats, String path) throws IOException {
        Path output = Path.of(path);
        if (output.getParent() != null) Files.createDirectories(output.getParent());
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            pw.println(INTESTAZIONE_CSV);
            for (StatisticheEsecuzione s : stats) {
                pw.printf(Locale.US,
                        "%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%s,%s,%s,%s,%b,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d%n",
                        s.gridId, s.variante, s.tipoGriglia, s.width, s.height, s.nOstacoli, s.seed,
                        s.Ox, s.Oy, s.Dx, s.Dy,
                        formatDistance(s.lunghezzaOD), formatDistance(s.lunghezzaDO),
                        csvField(s.sequenzaLandmarkOD.toString()),
                        csvField(s.sequenzaLandmarkDO.toString()),
                        s.correttezza,
                        s.celleFrontieraTotali,
                        s.iterazioniPruneRiga16,
                        s.iterazioniPruneRiga17,
                        s.iterazioniPruningTotali,
                        s.chiamateRicorsive,
                        s.cacheHit,
                        s.profonditaMassima,
                        s.chiusureAttivePicco,
                        s.celleChiusuraTotali,
                        s.memoriaMatriciPiccoBytesStimata,
                        s.warmupEseguiti,
                        s.ripetizioniTempo,
                        s.tempoMinNs,
                        s.tempoMedianoNs,
                        s.tempoMaxNs);
            }
        }
    }

    /** Salva i riassunti testuali, una riga per istanza. */
    public static void salvaRiassunti(List<String> righe, String path) throws IOException {
        Path output = Path.of(path);
        if (output.getParent() != null) Files.createDirectories(output.getParent());
        Files.write(output, righe, StandardCharsets.UTF_8);
    }

    private static String formatDistance(double value) {
        if (Double.isInfinite(value)) {
            return "INF";
        }
        return String.format(Locale.US, "%.6f", value);
    }

    private static String csvField(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
