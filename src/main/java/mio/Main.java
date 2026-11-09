package mio;

import mio.compito1.GeneratoreGriglia;
import mio.compito1.LettoreGriglia;
import mio.compito2.CalcoloChiusura;
import mio.compito2.DistanzaLibera;
import mio.compito2.GrigliaConOrigine;
import mio.compito2.StatoCella;
import mio.compito3.VarianteCamminoMin;
import mio.compito4.LimitiScala;
import mio.compito4.ScritturaFile;
import mio.compito4.Sperimentazione;
import mio.compito4.StatisticheEsecuzione;
import mio.griglia.Cella2D;
import mio.griglia.ICella2D;
import mio.griglia.IGriglia;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Main {

    /** Coppia O/D espressa come frazioni in [0,1] delle dimensioni della griglia. */
    private record PairFrac(double ox, double oy, double dx, double dy) {}
    private record Punto(int x, int y) {}

    /** Scenario = una configurazione di griglia, le varianti da eseguire e le coppie O/D. */
    private record Scenario(int w, int h, int nOst, long seed, List<VarianteCamminoMin> varianti,
                            PairFrac... pairs) {}

    /** Istanza pronta per le misure: griglia ricaricata dal JSON e coppia effettiva. */
    private record Istanza(String gridId, long seed, IGriglia<?> griglia, ICella2D O, ICella2D D,
                           List<VarianteCamminoMin> varianti) {}

    /** Configurazione della singola istanza richiesta da riga di comando. */
    private record Configurazione(
            int width, int height, int nOstacoli, long seed, boolean generatoreEsplicito,
            String inputGriglia, Punto origine, Punto destinazione, boolean riepilogo,
            String outputCsv, String outputGriglia, boolean outputGrigliaEsplicito,
            VarianteCamminoMin variante, boolean soloChiusura, String outputChiusura,
            boolean help) {}

    private static final int WARMUP = 1;
    private static final int RIPETIZIONI = 5;
    private static final Path DIRECTORY_SNAPSHOT = Path.of("risultati", "griglie_json")
            .toAbsolutePath().normalize();
    private static final String CSV_PIANO = "risultati/esperimenti.csv";
    private static final String RIASSUNTI_PIANO = "risultati/riassunti.txt";
    /** Griglie senza ostacoli e caso irraggiungibile: basta una chiamata per verso. */
    private static final List<VarianteCamminoMin> PRINCIPALI =
            List.of(VarianteCamminoMin.BASE, VarianteCamminoMin.LIMITE_PROPAGATO);
    /**
     * Senza ordinamento della frontiera le istanze 20x20 al 20%, 30x30 e 50x50
     * richiedono da secondi a minuti per esecuzione: la variante e esclusa da quei
     * gruppi per mantenere il piano entro pochi minuti.
     */
    private static final List<VarianteCamminoMin> SENZA_ORDINAMENTO_ESCLUSA = List.of(
            VarianteCamminoMin.BASE, VarianteCamminoMin.SENZA_CACHE, VarianteCamminoMin.LIMITE_PROPAGATO);
    private static final List<VarianteCamminoMin> TUTTE = List.of(VarianteCamminoMin.values());
    private static final PairFrac[] P3 = {
        new PairFrac(0.05, 0.05, 0.95, 0.95),
        new PairFrac(0.95, 0.05, 0.05, 0.95),
        new PairFrac(0.50, 0.50, 0.90, 0.10),
    };
    private static final PairFrac[] EDGE_INF = {
        new PairFrac(0.0, 0.0, 1.0, 1.0),
    };

    /**
     * Piano sperimentale. Una griglia senza ostacoli per dimensione isola il costo
     * del caso base; il peso del piano e sulle griglie 20x20 con densita fra il 5%
     * e il 20%, dove la ricorsione e significativa. Le istanze che la variante BASE
     * non completa in tempi ragionevoli (30x30 al 10%, 50x50 al 5%) sono coperte
     * dalla serie sui limiti di scala, che usa processi separati con timeout.
     */
    private static final List<Scenario> SCENARI = List.of(
        new Scenario(12, 12,   0,  50L, PRINCIPALI, P3),
        new Scenario(12, 12,  15,  50L, TUTTE, P3),
        new Scenario(12, 12,  15,  51L, TUTTE, P3),
        new Scenario(12, 12,  15,  52L, TUTTE, P3),
        new Scenario(20, 20,   0, 100L, PRINCIPALI, P3),
        new Scenario(20, 20,  20, 100L, TUTTE, P3),
        new Scenario(20, 20,  20, 101L, TUTTE, P3),
        new Scenario(20, 20,  20, 102L, TUTTE, P3),
        new Scenario(20, 20,  40, 100L, TUTTE, P3),
        new Scenario(20, 20,  40, 101L, TUTTE, P3),
        new Scenario(20, 20,  40, 102L, TUTTE, P3),
        new Scenario(20, 20,  80, 100L, SENZA_ORDINAMENTO_ESCLUSA, P3),
        new Scenario(20, 20,  80, 101L, SENZA_ORDINAMENTO_ESCLUSA, P3),
        new Scenario(20, 20,  80, 102L, SENZA_ORDINAMENTO_ESCLUSA, P3),
        new Scenario(30, 30,   0, 200L, PRINCIPALI, P3),
        new Scenario(30, 30,  45, 200L, SENZA_ORDINAMENTO_ESCLUSA, P3),
        new Scenario(30, 30,  45, 201L, SENZA_ORDINAMENTO_ESCLUSA, P3),
        new Scenario(30, 30,  45, 202L, SENZA_ORDINAMENTO_ESCLUSA, P3),
        new Scenario(50, 50,   0, 300L, PRINCIPALI, P3),
        new Scenario(50, 50,  50, 300L, SENZA_ORDINAMENTO_ESCLUSA, P3),
        new Scenario(50, 50,  50, 301L, SENZA_ORDINAMENTO_ESCLUSA, P3),
        new Scenario(50, 50,  50, 302L, SENZA_ORDINAMENTO_ESCLUSA, P3),
        new Scenario( 5,  5,  23,   1L, PRINCIPALI, EDGE_INF)
    );

    public static void main(String[] args) {
        int exitCode = esegui(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * Esegue la CLI restituendo un exit code senza stampare stack trace.
     * Codici: 0 successo, 2 argomenti non validi, 1 errore operativo.
     */
    static int esegui(String[] args) {
        try {
            if (args == null) {
                throw new IllegalArgumentException("Gli argomenti non possono essere null");
            }
            if (args.length == 0) {
                eseguiPianoSperimentale();
                return 0;
            }
            if (args[0].equals("--limiti-scala")) {
                if (args.length > 2) {
                    throw new IllegalArgumentException("--limiti-scala accetta al piu una variante");
                }
                List<VarianteCamminoMin> varianti = args.length == 2
                        ? List.of(VarianteCamminoMin.daNome(args[1])) : LimitiScala.VARIANTI;
                LimitiScala.eseguiSerie("risultati/limiti_scala.csv", varianti);
                return 0;
            }

            Configurazione config = parseArgs(args);
            if (config.help()) {
                stampaUso();
                return 0;
            }
            if (config.soloChiusura()) {
                eseguiChiusura(config);
            } else {
                eseguiIstanzaPersonalizzata(config);
            }
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Errore: " + e.getMessage());
            stampaUso();
            return 2;
        } catch (Exception e) {
            String dettaglio = e.getMessage() == null
                    ? e.getClass().getSimpleName() : e.getMessage();
            System.err.println("Errore di esecuzione: " + dettaglio);
            return 1;
        }
    }

    // ------------------------------------------------------------------ piano

    private static void eseguiPianoSperimentale() throws Exception {
        pulisciSnapshotScenario(DIRECTORY_SNAPSHOT);
        List<Istanza> istanze = preparaIstanze();

        // Riscaldamento della JVM: un passaggio completo del piano non misurato,
        // cosi le prime istanze non pagano la compilazione JIT.
        System.out.println("Riscaldamento: un passaggio non misurato su " + istanze.size() + " istanze");
        for (Istanza ist : istanze) {
            for (VarianteCamminoMin v : ist.varianti()) {
                Sperimentazione.esegui(ist.gridId(), ist.griglia(), ist.O(), ist.D(), false, v);
            }
        }

        List<StatisticheEsecuzione> risultati = new ArrayList<>();
        List<String> riassunti = new ArrayList<>();
        for (Istanza ist : istanze) {
            for (VarianteCamminoMin v : ist.varianti()) {
                StatisticheEsecuzione stats = Sperimentazione.eseguiRipetuto(
                        ist.gridId(), ist.griglia(), ist.O(), ist.D(), WARMUP, RIPETIZIONI, v);
                stats.seed = ist.seed();
                String riassunto = Sperimentazione.formattaRiassunto(stats);
                System.out.println(riassunto);
                riassunti.add(riassunto);
                risultati.add(stats);
            }
        }

        ScritturaFile.salvaCsv(risultati, CSV_PIANO);
        ScritturaFile.salvaRiassunti(riassunti, RIASSUNTI_PIANO);
        System.out.println("\nRisultati salvati in " + CSV_PIANO + " (" + risultati.size()
                + " righe) e " + RIASSUNTI_PIANO);
    }

    /** Genera, salva e ricarica le griglie del piano; calcola le coppie O/D effettive. */
    private static List<Istanza> preparaIstanze() throws IOException {
        List<Istanza> istanze = new ArrayList<>();
        int scenarioNum = 0;
        for (Scenario sc : SCENARI) {
            scenarioNum++;
            IGriglia<?> generata = GeneratoreGriglia.genera(sc.w(), sc.h(), sc.nOst(), sc.seed());
            String nomeSnapshot = String.format("scenario_%02d_%dx%d_%dost_seed%d.json",
                    scenarioNum, sc.w(), sc.h(), sc.nOst(), sc.seed());
            Path scenarioPath = DIRECTORY_SNAPSHOT.resolve(nomeSnapshot).normalize();
            if (!DIRECTORY_SNAPSHOT.equals(scenarioPath.getParent())) {
                throw new IOException("Percorso snapshot fuori dalla directory prevista: " + scenarioPath);
            }
            LettoreGriglia.salva(generata, scenarioPath.toString());
            IGriglia<?> g = LettoreGriglia.carica(scenarioPath.toString());
            int densPerc = (int) Math.round(100.0 * sc.nOst() / (sc.w() * sc.h()));

            int pairNum = 0;
            for (PairFrac pf : sc.pairs()) {
                pairNum++;
                ICella2D O = trovaCellaLibera(g,
                        (int) (pf.ox() * (sc.w() - 1)), (int) (pf.oy() * (sc.h() - 1)));
                ICella2D D = trovaCellaLibera(g,
                        (int) (pf.dx() * (sc.w() - 1)), (int) (pf.dy() * (sc.h() - 1)));
                String gridId = String.format("G%dx%d_d%02d_s%d_p%d",
                        sc.w(), sc.h(), densPerc, sc.seed(), pairNum);
                istanze.add(new Istanza(gridId, sc.seed(), g, O, D, sc.varianti()));
            }
        }
        return istanze;
    }

    /** Elimina soltanto gli snapshot regolari creati da precedenti piani sperimentali. */
    static void pulisciSnapshotScenario(Path directory) throws IOException {
        if (directory == null) {
            throw new IllegalArgumentException("La directory snapshot non puo essere null");
        }
        Path directoryNormalizzata = directory.toAbsolutePath().normalize();
        Files.createDirectories(directoryNormalizzata);

        try (DirectoryStream<Path> snapshot = Files.newDirectoryStream(
                directoryNormalizzata, "scenario_*.json")) {
            for (Path candidato : snapshot) {
                Path candidatoNormalizzato = candidato.toAbsolutePath().normalize();
                if (!directoryNormalizzata.equals(candidatoNormalizzato.getParent())) {
                    throw new IOException("Snapshot fuori dalla directory prevista: " + candidato);
                }
                if (Files.isRegularFile(candidatoNormalizzato, LinkOption.NOFOLLOW_LINKS)) {
                    Files.delete(candidatoNormalizzato);
                }
            }
        }
    }

    // ------------------------------------------------------- istanza singola

    /** Compito Tre: CamminoMin nei due versi su una griglia generata o letta da file. */
    private static void eseguiIstanzaPersonalizzata(Configurazione c) throws Exception {
        IGriglia<?> g = grigliaIngresso(c);
        ICella2D[] od = coppiaEffettiva(c, g);
        ICella2D O = od[0], D = od[1];

        StatisticheEsecuzione stats = Sperimentazione.esegui("CUSTOM", g, O, D, false, c.variante());
        stats.seed = c.inputGriglia() == null ? c.seed() : -1;
        if (c.riepilogo()) Sperimentazione.stampaRiassunto(stats);
        ScritturaFile.salvaCsv(List.of(stats), c.outputCsv());
        System.out.printf("Output: %s; griglia: %s%n", c.outputCsv(),
                c.inputGriglia() == null ? c.outputGriglia() : c.inputGriglia());
    }

    /** Compito Due: contesto, complemento e frontiera di O e distanza libera fra O e D. */
    private static void eseguiChiusura(Configurazione c) throws Exception {
        IGriglia<?> g = grigliaIngresso(c);
        ICella2D[] od = coppiaEffettiva(c, g);
        ICella2D O = od[0], D = od[1];

        GrigliaConOrigine chiusura = CalcoloChiusura.calcola(g, O.x(), O.y());
        System.out.print(descriviChiusura(chiusura, D));
        if (c.outputChiusura() != null) {
            LettoreGriglia.salva(chiusura, c.outputChiusura());
            System.out.println("Stati delle celle salvati in " + c.outputChiusura());
        }
    }

    /**
     * Mappa testuale della chiusura di O con i conteggi e la distanza libera da D.
     * Le celle di frontiera sono in maiuscolo.
     */
    static String descriviChiusura(GrigliaConOrigine c, ICella2D D) {
        StringBuilder sb = new StringBuilder();
        int contesto = 0, complemento = 0, frontiera1 = 0, frontiera2 = 0;
        for (int y = c.ymin(); y <= c.ymax(); y++) {
            for (int x = c.xmin(); x <= c.xmax(); x++) {
                int s = c.getCellaAt(x, y).stato();
                boolean front = StatoCella.FRONTIERA.is(s);
                char ch;
                if (StatoCella.OSTACOLO.is(s)) {
                    ch = '#';
                } else if (StatoCella.CONTESTO.is(s)) {
                    contesto++;
                    if (front) frontiera1++;
                    ch = front ? 'C' : 'c';
                } else if (StatoCella.COMPLEMENTO.is(s)) {
                    complemento++;
                    if (front) frontiera2++;
                    ch = front ? 'K' : 'k';
                } else {
                    ch = '.';
                }
                if (StatoCella.ORIGINE.is(s)) ch = 'O';
                else if (x == D.x() && y == D.y()) ch = 'D';
                if (x > c.xmin()) sb.append("   ");
                sb.append(ch);
            }
            sb.append('\n');
        }
        sb.append("Legenda: O origine, D destinazione, # ostacolo, c contesto (tipo 1), "
                + "k complemento (tipo 2), maiuscole = frontiera, . fuori dalla chiusura\n");
        sb.append(String.format(Locale.US,
                "Contesto: %d celle | complemento: %d celle | frontiera: %d celle (%d di tipo 1, %d di tipo 2)%n",
                contesto, complemento, frontiera1 + frontiera2, frontiera1, frontiera2));

        int statoD = c.getCellaAt(D.x(), D.y()).stato();
        String appartenenza;
        if (StatoCella.CONTESTO.is(statoD)) appartenenza = "nel contesto di O (cammino libero di tipo 1)";
        else if (StatoCella.COMPLEMENTO.is(statoD)) appartenenza = "nel complemento di O (cammino libero di tipo 2)";
        else appartenenza = "fuori dalla chiusura di O";
        DistanzaLibera d = DistanzaLibera.from(c.Ox(), c.Oy(), D.x(), D.y());
        sb.append(String.format(Locale.US, "O=(%d,%d), D=(%d,%d): D si trova %s%n",
                c.Ox(), c.Oy(), D.x(), D.y(), appartenenza));
        sb.append(String.format(Locale.US,
                "dlib(O,D) = %d + %d*sqrt(2) = %.6f (%d mosse rettilinee, %d diagonali)%n",
                d.distanzaTorre(), d.distanzaAlfiere(), d.distanza(),
                d.distanzaTorre(), d.distanzaAlfiere()));
        return sb.toString();
    }

    /** Griglia letta da --grid-input oppure generata, salvata e ricaricata dal JSON. */
    private static IGriglia<?> grigliaIngresso(Configurazione c) throws IOException {
        if (c.inputGriglia() != null) {
            return LettoreGriglia.carica(c.inputGriglia());
        }
        IGriglia<?> generata = GeneratoreGriglia.genera(c.width(), c.height(), c.nOstacoli(), c.seed());
        LettoreGriglia.salva(generata, c.outputGriglia());
        return LettoreGriglia.carica(c.outputGriglia());
    }

    /** Origine e destinazione effettive: la cella libera piu vicina a quelle richieste. */
    private static ICella2D[] coppiaEffettiva(Configurazione c, IGriglia<?> g) {
        validaPunto(c.origine(), g.width(), g.height(), "origin");
        validaPunto(c.destinazione(), g.width(), g.height(), "destination");
        Punto origineRichiesta = c.origine() != null ? c.origine() : new Punto(0, 0);
        Punto destinazioneRichiesta = c.destinazione() != null
                ? c.destinazione() : new Punto(g.width() - 1, g.height() - 1);
        ICella2D O = trovaCellaLibera(g, origineRichiesta.x(), origineRichiesta.y());
        ICella2D D = trovaCellaLibera(g, destinazioneRichiesta.x(), destinazioneRichiesta.y());
        segnalaSpostamento("origine", origineRichiesta, O);
        segnalaSpostamento("destinazione", destinazioneRichiesta, D);
        return new ICella2D[]{O, D};
    }

    // ------------------------------------------------------- riga di comando

    private static Configurazione parseArgs(String[] args) {
        int width = 20, height = 20, nOstacoli = 20;
        long seed = 42L;
        boolean generatoreEsplicito = false;
        String inputGriglia = null;
        Punto origine = null, destinazione = null;
        boolean riepilogo = true, help = false, soloChiusura = false;
        String outputCsv = "risultati/esperimento_personalizzato.csv";
        String outputGriglia = "risultati/griglie_json/personalizzata.json";
        boolean outputGrigliaEsplicito = false;
        String outputChiusura = null;
        VarianteCamminoMin variante = VarianteCamminoMin.BASE;

        for (int i = 0; i < args.length; i++) {
            String opzione = args[i];
            if (opzione.equals("--help") || opzione.equals("-h")) {
                help = true;
                continue;
            }
            if (opzione.equals("--chiusura")) {
                soloChiusura = true;
                continue;
            }
            if (!List.of("--width", "--height", "--obstacles", "--seed", "--grid-input",
                    "--origin", "--destination", "--summary", "--output", "--grid-output",
                    "--variante", "--chiusura-output").contains(opzione)) {
                throw new IllegalArgumentException("Opzione sconosciuta: " + opzione);
            }
            if (i + 1 >= args.length) throw new IllegalArgumentException("Valore mancante per " + opzione);
            String valore = args[++i];
            try {
                switch (opzione) {
                    case "--width" -> { width = Integer.parseInt(valore); generatoreEsplicito = true; }
                    case "--height" -> { height = Integer.parseInt(valore); generatoreEsplicito = true; }
                    case "--obstacles" -> { nOstacoli = Integer.parseInt(valore); generatoreEsplicito = true; }
                    case "--seed" -> { seed = Long.parseLong(valore); generatoreEsplicito = true; }
                    case "--grid-input" -> inputGriglia = valore;
                    case "--origin" -> origine = parsePunto(valore, opzione);
                    case "--destination" -> destinazione = parsePunto(valore, opzione);
                    case "--summary" -> riepilogo = parseBoolean(valore, opzione);
                    case "--output" -> outputCsv = valore;
                    case "--grid-output" -> { outputGriglia = valore; outputGrigliaEsplicito = true; }
                    case "--variante" -> variante = VarianteCamminoMin.daNome(valore);
                    case "--chiusura-output" -> outputChiusura = valore;
                    default -> throw new IllegalArgumentException("Opzione sconosciuta: " + opzione);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Valore numerico non valido per " + opzione + ": " + valore);
            }
        }
        Configurazione c = new Configurazione(width, height, nOstacoli, seed, generatoreEsplicito,
                inputGriglia, origine, destinazione, riepilogo, outputCsv, outputGriglia,
                outputGrigliaEsplicito, variante, soloChiusura, outputChiusura, help);
        if (!help) valida(c);
        return c;
    }

    private static Punto parsePunto(String value, String option) {
        String[] parti = value.split(",", -1);
        if (parti.length != 2) throw new IllegalArgumentException(option + " richiede x,y");
        return new Punto(Integer.parseInt(parti[0].trim()), Integer.parseInt(parti[1].trim()));
    }

    private static boolean parseBoolean(String value, String option) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException(option + " accetta solo true o false");
    }

    private static void valida(Configurazione c) {
        if (c.inputGriglia() != null) {
            if (c.generatoreEsplicito())
                throw new IllegalArgumentException(
                        "--grid-input non si combina con --width, --height, --obstacles e --seed");
            if (c.outputGrigliaEsplicito())
                throw new IllegalArgumentException("--grid-input non si combina con --grid-output");
            return;
        }
        if (c.width() <= 0 || c.height() <= 0)
            throw new IllegalArgumentException("width e height devono essere positivi");
        long celleTotali = (long) c.width() * c.height();
        if (c.nOstacoli() < 0 || c.nOstacoli() >= celleTotali)
            throw new IllegalArgumentException("obstacles deve essere tra 0 e width*height-1");
        validaPunto(c.origine(), c.width(), c.height(), "origin");
        validaPunto(c.destinazione(), c.width(), c.height(), "destination");
    }

    private static void validaPunto(Punto p, int width, int height, String nome) {
        if (p != null && (p.x() < 0 || p.x() >= width || p.y() < 0 || p.y() >= height))
            throw new IllegalArgumentException(nome + " deve essere interna alla griglia");
    }

    private static void segnalaSpostamento(String nome, Punto richiesta, ICella2D effettiva) {
        if (richiesta.x() != effettiva.x() || richiesta.y() != effettiva.y())
            System.err.printf("La %s richiesta (%d,%d) era un ostacolo: uso (%d,%d).%n",
                    nome, richiesta.x(), richiesta.y(), effettiva.x(), effettiva.y());
    }

    /** Trova la cella libera più vicina (distanza di Chebyshev) a (cx, cy). */
    private static ICella2D trovaCellaLibera(IGriglia<?> g, int cx, int cy) {
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

    private static void stampaUso() {
        System.out.println("""
                Uso:
                  nessun argomento: esegue il piano sperimentale (risultati/esperimenti.csv, risultati/riassunti.txt)
                  --limiti-scala [variante]: serie sui limiti di scala (risultati/limiti_scala.csv)

                  Griglia: generata con --width N --height N --obstacles N --seed N
                           oppure letta da file con --grid-input griglia.json
                  [--origin x,y] [--destination x,y]

                  Compito Due:  --chiusura [--chiusura-output stati.json]
                                stampa contesto, complemento e frontiera di O e dlib(O,D)
                  Compito Tre:  [--variante base|senza-cache|senza-ordinamento|limite-propagato]
                                [--summary true|false] [--output file.csv] [--grid-output griglia.json]

                Esempi:
                  --width 20 --height 20 --obstacles 40 --seed 77 --origin 0,0 --destination 19,19
                  --grid-input risultati/griglie_json/scenario_02_12x12_15ost_seed50.json --origin 0,0 --destination 10,10 --chiusura
                """);
    }
}
