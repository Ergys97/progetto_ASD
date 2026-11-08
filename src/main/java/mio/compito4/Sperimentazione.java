package mio.compito4;

import mio.compito2.DistanzaLibera;
import mio.compito3.Cammino;
import mio.compito3.CamminoMin;
import mio.compito3.VarianteCamminoMin;
import mio.griglia.ICella2D;
import mio.griglia.IGriglia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Sperimentazione {

    /** Overload senza gridId — usato nei test. */
    public static StatisticheEsecuzione esegui(IGriglia<?> g, ICella2D O, ICella2D D) {
        return esegui("test", g, O, D);
    }

    public static StatisticheEsecuzione esegui(String gridId, IGriglia<?> g, ICella2D O, ICella2D D) {
        return esegui(gridId, g, O, D, true);
    }

    /** Esegue entrambe le direzioni con la variante BASE. */
    public static StatisticheEsecuzione esegui(String gridId, IGriglia<?> g, ICella2D O, ICella2D D,
                                                boolean stampaRiassunto) {
        return esegui(gridId, g, O, D, stampaRiassunto, VarianteCamminoMin.BASE);
    }

    /** Esegue entrambe le direzioni; il riassunto a console può essere disabilitato. */
    public static StatisticheEsecuzione esegui(String gridId, IGriglia<?> g, ICella2D O, ICella2D D,
                                                boolean stampaRiassunto, VarianteCamminoMin variante) {
        StatisticheEsecuzione stats = new StatisticheEsecuzione();
        stats.gridId = gridId;
        stats.variante = variante.nome();
        stats.tipoGriglia = "Casuale";
        stats.width  = g.width();
        stats.height = g.height();
        stats.nOstacoli = contaOstacoli(g);
        stats.Ox = O.x();  stats.Oy = O.y();
        stats.Dx = D.x();  stats.Dy = D.y();

        long t0 = System.nanoTime();
        Cammino od = CamminoMin.calcola(g, O, D, stats, variante);
        Cammino dO = CamminoMin.calcola(g, D, O, stats, variante);
        stats.tempoNs = System.nanoTime() - t0;
        stats.warmupEseguiti = 0;
        stats.ripetizioniTempo = 1;
        stats.tempoMinNs = stats.tempoNs;
        stats.tempoMedianoNs = stats.tempoNs;
        stats.tempoMaxNs = stats.tempoNs;

        stats.lunghezzaOD = od.distanza().distanza();
        stats.lunghezzaDO = dO.distanza().distanza();
        stats.sequenzaLandmarkOD = Double.isInfinite(stats.lunghezzaOD)
                ? List.of() : List.copyOf(od.landmarks());
        stats.sequenzaLandmarkDO = Double.isInfinite(stats.lunghezzaDO)
                ? List.of() : List.copyOf(dO.landmarks());
        stats.correttezza = (Double.isInfinite(stats.lunghezzaOD) && Double.isInfinite(stats.lunghezzaDO))
            || Math.abs(stats.lunghezzaOD - stats.lunghezzaDO) < DistanzaLibera.EPSILON;

        if (stampaRiassunto) stampaRiassunto(stats);
        return stats;
    }

    /**
     * Esegue un protocollo temporale ripetuto. I warmup vengono scartati e le
     * metriche deterministiche appartengono al singolo run di tempo mediano.
     */
    public static StatisticheEsecuzione eseguiRipetuto(
            String gridId, IGriglia<?> g, ICella2D O, ICella2D D,
            int warmup, int ripetizioni) {
        return eseguiRipetuto(gridId, g, O, D, warmup, ripetizioni, VarianteCamminoMin.BASE);
    }

    /** Protocollo ripetuto per una variante specifica di CamminoMin. */
    public static StatisticheEsecuzione eseguiRipetuto(
            String gridId, IGriglia<?> g, ICella2D O, ICella2D D,
            int warmup, int ripetizioni, VarianteCamminoMin variante) {
        if (warmup < 0) {
            throw new IllegalArgumentException("warmup deve essere maggiore o uguale a zero");
        }
        if (ripetizioni <= 0 || ripetizioni % 2 == 0) {
            throw new IllegalArgumentException("ripetizioni deve essere un intero positivo dispari");
        }

        for (int i = 0; i < warmup; i++) {
            esegui(gridId, g, O, D, false, variante);
        }

        List<StatisticheEsecuzione> misure = new ArrayList<>(ripetizioni);
        StatisticheEsecuzione riferimento = null;
        for (int i = 0; i < ripetizioni; i++) {
            StatisticheEsecuzione misura = esegui(gridId, g, O, D, false, variante);
            if (riferimento == null) {
                riferimento = misura;
            } else {
                verificaMetricheDeterministiche(riferimento, misura, i + 1);
            }
            misure.add(misura);
        }

        misure.sort(Comparator.comparingLong(s -> s.tempoNs));
        StatisticheEsecuzione mediana = misure.get(ripetizioni / 2);
        mediana.warmupEseguiti = warmup;
        mediana.ripetizioniTempo = ripetizioni;
        mediana.tempoMinNs = misure.get(0).tempoNs;
        mediana.tempoMedianoNs = mediana.tempoNs;
        mediana.tempoMaxNs = misure.get(misure.size() - 1).tempoNs;
        mediana.tempoNs = mediana.tempoMedianoNs;
        return mediana;
    }

    public static void stampaRiassunto(StatisticheEsecuzione stats) {
        System.out.println(formattaRiassunto(stats));
    }

    /** Riassunto di un'istanza su una riga, come richiesto dalla traccia (pag. 71). */
    public static String formattaRiassunto(StatisticheEsecuzione stats) {
        String odStr = Double.isInfinite(stats.lunghezzaOD)
                ? "INF" : String.format(Locale.US, "%.4f", stats.lunghezzaOD);
        String doStr = Double.isInfinite(stats.lunghezzaDO)
                ? "INF" : String.format(Locale.US, "%.4f", stats.lunghezzaDO);
        return String.format(
            Locale.US,
            "[%s] variante=%s | tipo=%s | %dx%d | ost=%d | O=(%d,%d)->D=(%d,%d) | OD=%s | DO=%s | corr=%b | front=%d | r16False=%d | r17Prune=%d | pruneTot=%d | chiamate=%d | cacheHit=%d | prof=%d | chiusurePicco=%d | memEst=%dB | tempoNs[min/med/max]=%d/%d/%d | warmup=%d | rip=%d | seqOD=%s | seqDO=%s",
            stats.gridId, stats.variante, stats.tipoGriglia, stats.width, stats.height,
            stats.nOstacoli,
            stats.Ox, stats.Oy, stats.Dx, stats.Dy,
            odStr, doStr, stats.correttezza,
            stats.celleFrontieraTotali,
            stats.iterazioniPruneRiga16,
            stats.iterazioniPruneRiga17,
            stats.iterazioniPruningTotali,
            stats.chiamateRicorsive,
            stats.cacheHit,
            stats.profonditaMassima,
            stats.chiusureAttivePicco,
            stats.memoriaMatriciPiccoBytesStimata,
            stats.tempoMinNs,
            stats.tempoMedianoNs,
            stats.tempoMaxNs,
            stats.warmupEseguiti,
            stats.ripetizioniTempo,
            stats.sequenzaLandmarkOD,
            stats.sequenzaLandmarkDO
        );
    }

    private static void verificaMetricheDeterministiche(
            StatisticheEsecuzione attesa, StatisticheEsecuzione ottenuta, int numeroRun) {
        verifica(Objects.equals(attesa.gridId, ottenuta.gridId), "gridId", numeroRun);
        verifica(Objects.equals(attesa.variante, ottenuta.variante), "variante", numeroRun);
        verifica(Objects.equals(attesa.tipoGriglia, ottenuta.tipoGriglia), "tipoGriglia", numeroRun);
        verifica(attesa.width == ottenuta.width && attesa.height == ottenuta.height,
                "dimensioni", numeroRun);
        verifica(attesa.nOstacoli == ottenuta.nOstacoli, "nOstacoli", numeroRun);
        verifica(attesa.seed == ottenuta.seed, "seed", numeroRun);
        verifica(attesa.Ox == ottenuta.Ox && attesa.Oy == ottenuta.Oy
                        && attesa.Dx == ottenuta.Dx && attesa.Dy == ottenuta.Dy,
                "coordinate O/D", numeroRun);
        verifica(Double.doubleToLongBits(attesa.lunghezzaOD)
                        == Double.doubleToLongBits(ottenuta.lunghezzaOD),
                "lunghezzaOD", numeroRun);
        verifica(Double.doubleToLongBits(attesa.lunghezzaDO)
                        == Double.doubleToLongBits(ottenuta.lunghezzaDO),
                "lunghezzaDO", numeroRun);
        verifica(attesa.sequenzaLandmarkOD.equals(ottenuta.sequenzaLandmarkOD),
                "sequenzaLandmarkOD", numeroRun);
        verifica(attesa.sequenzaLandmarkDO.equals(ottenuta.sequenzaLandmarkDO),
                "sequenzaLandmarkDO", numeroRun);
        verifica(attesa.correttezza == ottenuta.correttezza, "correttezza", numeroRun);
        verifica(attesa.celleFrontieraTotali == ottenuta.celleFrontieraTotali,
                "celleFrontieraTotali", numeroRun);
        verifica(attesa.iterazioniPruneRiga16 == ottenuta.iterazioniPruneRiga16,
                "iterazioniPruneRiga16", numeroRun);
        verifica(attesa.iterazioniPruneRiga17 == ottenuta.iterazioniPruneRiga17,
                "iterazioniPruneRiga17", numeroRun);
        verifica(attesa.iterazioniPruningTotali == ottenuta.iterazioniPruningTotali,
                "iterazioniPruningTotali", numeroRun);
        verifica(attesa.chiamateRicorsive == ottenuta.chiamateRicorsive,
                "chiamateRicorsive", numeroRun);
        verifica(attesa.cacheHit == ottenuta.cacheHit, "cacheHit", numeroRun);
        verifica(attesa.profonditaMassima == ottenuta.profonditaMassima,
                "profonditaMassima", numeroRun);
        verifica(attesa.chiusureAttivePicco == ottenuta.chiusureAttivePicco,
                "chiusureAttivePicco", numeroRun);
        verifica(attesa.celleChiusuraTotali == ottenuta.celleChiusuraTotali,
                "celleChiusuraTotali", numeroRun);
        verifica(attesa.memoriaMatriciPiccoBytesStimata
                        == ottenuta.memoriaMatriciPiccoBytesStimata,
                "memoriaMatriciPiccoBytesStimata", numeroRun);
    }

    private static void verifica(boolean condizione, String metrica, int numeroRun) {
        if (!condizione) {
            throw new IllegalStateException(
                    "Metrica non deterministica al run " + numeroRun + ": " + metrica);
        }
    }

    private static int contaOstacoli(IGriglia<?> g) {
        int totale = 0;
        for (int y = g.ymin(); y <= g.ymax(); y++)
            for (int x = g.xmin(); x <= g.xmax(); x++)
                if (g.isOstacolo(x, y)) totale++;
        return totale;
    }
}
