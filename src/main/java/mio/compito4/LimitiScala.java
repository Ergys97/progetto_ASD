package mio.compito4;

import mio.compito1.GeneratoreGriglia;
import mio.compito2.DistanzaLibera;
import mio.compito3.Cammino;
import mio.compito3.CamminoMin;
import mio.compito3.VarianteCamminoMin;
import mio.griglia.Cella2D;
import mio.griglia.ICella2D;
import mio.griglia.IGriglia;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Serie sperimentale sui limiti di scala di CAMMINOMIN.
 *
 * Ogni direzione di ogni istanza viene eseguita in un processo JVM separato, con
 * heap massimo fisso e tempo massimo fisso. In questo modo un timeout o un
 * OutOfMemoryError non compromettono le istanze successive e l'algoritmo non
 * richiede alcuna modifica (in particolare nessun meccanismo di interruzione).
 *
 * Per ogni coppia viene eseguita prima la direzione O-&gt;D; se termina, viene
 * eseguita anche D-&gt;O e si verifica la simmetria delle lunghezze.
 *
 * La serie viene ripetuta per ciascuna variante di {@link #VARIANTI}, sulle stesse
 * griglie e coppie, per confrontare la versione della traccia con il limite propagato.
 */
public final class LimitiScala {

    public static final int[] DIMENSIONI = {20, 30, 40, 50, 70, 100};
    public static final int[] DENSITA_PERC = {0, 5, 10, 15, 20, 30};
    public static final long[] SEED = {1L, 2L, 3L};
    public static final double FRAZIONE_ORIGINE = 0.10;
    public static final double FRAZIONE_DESTINAZIONE = 0.90;
    public static final long TIMEOUT_MS = 10_000L;
    public static final String HEAP_MAX = "1g";
    public static final List<VarianteCamminoMin> VARIANTI =
            List.of(VarianteCamminoMin.BASE, VarianteCamminoMin.LIMITE_PROPAGATO);

    private static final String PREFISSO_RISULTATO = "RISULTATO;";

    /** Esito della singola direzione eseguita nel processo figlio. */
    record Esito(String stato, double lunghezza, long tempoNs, long chiamate,
                 int profondita, long frontiera, long cacheHit, long piccoHeapBytes) {

        static Esito fallito(String stato) {
            return new Esito(stato, Double.NaN, -1, -1, -1, -1, -1, -1);
        }

        boolean completato() {
            return stato.equals("OK");
        }
    }

    private LimitiScala() {}

    /**
     * Esegue l'intera serie e salva il CSV.
     *
     * @param outputCsv percorso del CSV di uscita
     * @return numero di coppie elaborate
     */
    public static int eseguiSerie(String outputCsv) throws IOException, InterruptedException {
        return eseguiSerie(outputCsv, VARIANTI);
    }

    /**
     * Esegue la serie per le varianti indicate e salva il CSV.
     *
     * @param outputCsv percorso del CSV di uscita
     * @param varianti  varianti di CamminoMin da eseguire, nell'ordine dato
     * @return numero di righe scritte (coppie per variante)
     */
    public static int eseguiSerie(String outputCsv, List<VarianteCamminoMin> varianti)
            throws IOException, InterruptedException {
        if (varianti == null || varianti.isEmpty()) {
            throw new IllegalArgumentException("Serve almeno una variante");
        }
        List<String> righe = new ArrayList<>();
        righe.add(String.join(",",
                "variante", "width", "height", "densitaPerc", "nOstacoli", "seed",
                "Ox", "Oy", "Dx", "Dy",
                "esitoOD", "esitoDO", "lunghezzaOD", "lunghezzaDO", "simmetria",
                "tempoODms", "tempoDOms", "chiamateOD", "profonditaOD", "frontieraOD",
                "cacheHitOD", "piccoHeapMBOD", "timeoutMs", "heapMax"));

        int totale = varianti.size() * DIMENSIONI.length * DENSITA_PERC.length * SEED.length;
        int indice = 0;
        for (VarianteCamminoMin variante : varianti) {
            for (int n : DIMENSIONI) {
                for (int densita : DENSITA_PERC) {
                    int nOstacoli = (int) Math.round(n * n * densita / 100.0);
                    for (long seed : SEED) {
                        indice++;
                        IGriglia<?> g = GeneratoreGriglia.genera(n, n, nOstacoli, seed);
                        int c0 = (int) (FRAZIONE_ORIGINE * (n - 1));
                        int c1 = (int) (FRAZIONE_DESTINAZIONE * (n - 1));
                        ICella2D o = trovaCellaLibera(g, c0, c0);
                        ICella2D d = trovaCellaLibera(g, c1, c1);

                        Esito od = eseguiDirezione(n, nOstacoli, seed, o, d, variante);
                        Esito dO = od.completato()
                                ? eseguiDirezione(n, nOstacoli, seed, d, o, variante)
                                : Esito.fallito("NON_ESEGUITO");

                        String simmetria;
                        if (!od.completato() || !dO.completato()) {
                            simmetria = "ND";
                        } else if (Double.isInfinite(od.lunghezza()) && Double.isInfinite(dO.lunghezza())) {
                            simmetria = "true";
                        } else {
                            simmetria = Boolean.toString(
                                    Math.abs(od.lunghezza() - dO.lunghezza()) < DistanzaLibera.EPSILON);
                        }

                        righe.add(String.join(",",
                                variante.nome(), Integer.toString(n), Integer.toString(n), Integer.toString(densita),
                                Integer.toString(nOstacoli), Long.toString(seed),
                                Integer.toString(o.x()), Integer.toString(o.y()),
                                Integer.toString(d.x()), Integer.toString(d.y()),
                                od.stato(), dO.stato(),
                                formattaLunghezza(od), formattaLunghezza(dO), simmetria,
                                formattaMs(od), formattaMs(dO),
                                formattaIntero(od.chiamate()), formattaIntero(od.profondita()),
                                formattaIntero(od.frontiera()), formattaIntero(od.cacheHit()),
                                od.piccoHeapBytes() < 0 ? "" : String.format(Locale.US, "%.1f",
                                        od.piccoHeapBytes() / (1024.0 * 1024.0)),
                                Long.toString(TIMEOUT_MS), HEAP_MAX));

                        System.out.printf(Locale.US,
                                "[%3d/%d] %s %dx%d dens=%d%% seed=%d -> OD=%s %s | DO=%s %s | simm=%s%n",
                                indice, totale, variante.nome(), n, n, densita, seed,
                                od.stato(), formattaMs(od), dO.stato(), formattaMs(dO), simmetria);
                    }
                }
            }
        }

        Path path = Path.of(outputCsv);
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        Files.write(path, righe, StandardCharsets.UTF_8);
        System.out.println("\nSerie sui limiti di scala salvata in " + outputCsv
                + " (" + (righe.size() - 1) + " righe)");
        return righe.size() - 1;
    }

    /** Avvia un processo figlio per una sola direzione e ne interpreta l'esito. */
    private static Esito eseguiDirezione(int n, int nOstacoli, long seed, ICella2D o, ICella2D d,
                                         VarianteCamminoMin variante)
            throws IOException, InterruptedException {
        File log = File.createTempFile("limiti_scala_", ".log");
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    javaEseguibile(), "-Xmx" + HEAP_MAX,
                    "-cp", classpath(), LimitiScala.class.getName(),
                    Integer.toString(n), Integer.toString(nOstacoli), Long.toString(seed),
                    Integer.toString(o.x()), Integer.toString(o.y()),
                    Integer.toString(d.x()), Integer.toString(d.y()), variante.nome());
            pb.redirectErrorStream(true);
            pb.redirectOutput(log);
            Process p = pb.start();
            if (!p.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                p.waitFor();
                return Esito.fallito("TIMEOUT");
            }
            String output = Files.readString(log.toPath(), StandardCharsets.UTF_8);
            for (String riga : output.split("\\R")) {
                if (riga.startsWith(PREFISSO_RISULTATO)) {
                    return parseRisultato(riga.substring(PREFISSO_RISULTATO.length()));
                }
            }
            if (output.contains("OutOfMemoryError")) return Esito.fallito("OOM");
            if (output.contains("StackOverflowError")) return Esito.fallito("STACK");
            return Esito.fallito("ERRORE");
        } finally {
            Files.deleteIfExists(log.toPath());
        }
    }

    private static Esito parseRisultato(String s) {
        String[] c = s.split(";", -1);
        if (!c[0].equals("OK")) return Esito.fallito(c[0]);
        double lunghezza = c[1].equals("INF") ? Double.POSITIVE_INFINITY : Double.parseDouble(c[1]);
        return new Esito("OK", lunghezza, Long.parseLong(c[2]), Long.parseLong(c[3]),
                Integer.parseInt(c[4]), Long.parseLong(c[5]), Long.parseLong(c[6]),
                Long.parseLong(c[7]));
    }

    /**
     * Ingresso del processo figlio:
     * {@code width nOstacoli seed Ox Oy Dx Dy [variante]} (griglia quadrata width x width,
     * variante BASE se omessa).
     */
    public static void main(String[] args) {
        try {
            int n = Integer.parseInt(args[0]);
            int nOstacoli = Integer.parseInt(args[1]);
            long seed = Long.parseLong(args[2]);
            ICella2D o = new Cella2D(0, Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            ICella2D d = new Cella2D(0, Integer.parseInt(args[5]), Integer.parseInt(args[6]));
            VarianteCamminoMin variante = args.length > 7
                    ? VarianteCamminoMin.daNome(args[7]) : VarianteCamminoMin.BASE;
            IGriglia<?> g = GeneratoreGriglia.genera(n, n, nOstacoli, seed);

            StatisticheEsecuzione stats = new StatisticheEsecuzione();
            long t0 = System.nanoTime();
            Cammino c = CamminoMin.calcola(g, o, d, stats, variante);
            long tempo = System.nanoTime() - t0;

            double l = c.distanza().distanza();
            System.out.println(PREFISSO_RISULTATO + String.join(";", "OK",
                    Double.isInfinite(l) ? "INF" : String.format(Locale.US, "%.6f", l),
                    Long.toString(tempo), Long.toString(stats.chiamateRicorsive),
                    Integer.toString(stats.profonditaMassima),
                    Long.toString(stats.celleFrontieraTotali), Long.toString(stats.cacheHit),
                    Long.toString(piccoHeapBytes())));
        } catch (OutOfMemoryError e) {
            System.out.println(PREFISSO_RISULTATO + "OOM");
            System.exit(3);
        } catch (StackOverflowError e) {
            System.out.println(PREFISSO_RISULTATO + "STACK");
            System.exit(4);
        }
    }

    /** Somma dei picchi dei pool di heap: limite superiore del picco effettivo. */
    private static long piccoHeapBytes() {
        long totale = 0;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP && pool.getPeakUsage() != null) {
                totale += pool.getPeakUsage().getUsed();
            }
        }
        return totale;
    }

    private static String javaEseguibile() {
        String nome = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")
                ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", nome).toString();
    }

    /** Percorso delle classi del progetto: funziona sia con il JAR sia con exec:java. */
    private static String classpath() {
        try {
            return Path.of(LimitiScala.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI()).toString();
        } catch (Exception e) {
            return System.getProperty("java.class.path");
        }
    }

    /** Cella libera più vicina in distanza di Chebyshev, come nel piano principale. */
    static ICella2D trovaCellaLibera(IGriglia<?> g, int cx, int cy) {
        for (int r = 0; r <= Math.max(g.width(), g.height()); r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    int x = cx + dx, y = cy + dy;
                    if (x < g.xmin() || x > g.xmax() || y < g.ymin() || y > g.ymax()) continue;
                    if (!g.isOstacolo(x, y))
                        return new Cella2D(0, x, y);
                }
            }
        }
        throw new IllegalStateException("Nessuna cella libera nella griglia");
    }

    private static String formattaLunghezza(Esito e) {
        if (!e.completato()) return "";
        return Double.isInfinite(e.lunghezza()) ? "INF"
                : String.format(Locale.US, "%.6f", e.lunghezza());
    }

    private static String formattaMs(Esito e) {
        return e.completato() ? String.format(Locale.US, "%.3f", e.tempoNs() / 1e6) : "";
    }

    private static String formattaIntero(long v) {
        return v < 0 ? "" : Long.toString(v);
    }
}
