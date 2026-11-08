package mio.compito3;

import mio.compito2.CalcoloChiusura;
import mio.compito2.DistanzaLibera;
import mio.compito2.GrigliaConOrigine;
import mio.compito2.StatoCella;
import mio.compito4.StatisticheEsecuzione;
import mio.griglia.ICella2D;
import mio.griglia.IGriglia;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CamminoMin {

    /** API pubblica senza raccolta metriche, variante BASE. */
    public static Cammino calcola(IGriglia<?> griglia, ICella2D O, ICella2D D) {
        return calcola(griglia, O, D, null, VarianteCamminoMin.BASE);
    }

    /** Variante BASE con raccolta delle metriche. */
    public static Cammino calcola(IGriglia<?> griglia, ICella2D O, ICella2D D,
                                  StatisticheEsecuzione stats) {
        return calcola(griglia, O, D, stats, VarianteCamminoMin.BASE);
    }

    /**
     * Implementazione ricorsiva di CAMMINOMIN.
     *
     * Caso base (righe 4-8 dello pseudocodice): D appartiene alla chiusura di O,
     * il cammino minimo e il cammino libero e la sequenza e [(O,0), (D,t)].
     *
     * Passo ricorsivo (righe 14-22): per ogni cella F della frontiera di O che supera
     * le condizioni delle righe 16 e 17 si ricorre su F, con la chiusura di O
     * (tranne F) aggiunta agli ostacoli.
     *
     * @param stats    se non null, raccoglie le metriche del riassunto
     * @param variante ottimizzazioni attive (cache, ordinamento, limite propagato)
     */
    public static Cammino calcola(IGriglia<?> griglia, ICella2D O, ICella2D D,
                                  StatisticheEsecuzione stats, VarianteCamminoMin variante) {
        validaInput(griglia, O, D);
        Objects.requireNonNull(variante, "La variante non puo essere null");
        Esecuzione e = new Esecuzione(stats, variante);
        return e.ricorsiva(griglia, O, D, 1, 0, DistanzaLibera.INFINITY);
    }

    /** Chiave della cache: insieme completo degli ostacoli, origine e destinazione. */
    private record Chiave(BitSet ostacoli, int ox, int oy, int dx, int dy) {}

    /**
     * Valore della cache. Se {@code esatto} non e null e il cammino minimo del
     * sottoproblema; altrimenti si sa soltanto che non esistono cammini piu corti
     * di {@code almeno} (risultato di una chiamata potata dal limite).
     */
    private record Voce(Cammino esatto, DistanzaLibera almeno) {
        boolean copre(DistanzaLibera limite) {
            return esatto != null || limite.compareTo(almeno) <= 0;
        }

        Cammino risultato() {
            return esatto != null ? esatto : Cammino.INFINITY;
        }
    }

    /** Cella di frontiera con le due distanze libere usate da ordinamento e potatura. */
    private record Candidato(ICella2D F, DistanzaLibera lF, DistanzaLibera limiteInferiore) {}

    /** Stato di una singola invocazione principale: metriche, variante e cache. */
    private static final class Esecuzione {
        private final StatisticheEsecuzione stats;
        private final VarianteCamminoMin variante;
        private final Map<Chiave, Voce> cache = new HashMap<>();

        Esecuzione(StatisticheEsecuzione stats, VarianteCamminoMin variante) {
            this.stats = stats;
            this.variante = variante;
        }

        /**
         * @param limite si cercano solo cammini di lunghezza minore di limite; con
         *               limite infinito la chiamata coincide con quella della traccia
         */
        Cammino ricorsiva(IGriglia<?> griglia, ICella2D O, ICella2D D,
                          int profondita, int chiusureAttive, DistanzaLibera limite) {
            if (stats != null) {
                stats.chiamateRicorsive++;
                stats.profonditaMassima = Math.max(stats.profonditaMassima, profondita);
            }

            Chiave chiave = null;
            if (variante.cache()) {
                chiave = new Chiave(griglia.ostacoli(), O.x(), O.y(), D.x(), D.y());
                Voce voce = cache.get(chiave);
                if (voce != null && voce.copre(limite)) {
                    if (stats != null) stats.cacheHit++;
                    return voce.risultato();
                }
            }

            // Righe 3, 6 e 9: contesto, complemento e frontiera in un'unica passata.
            GrigliaConOrigine g = CalcoloChiusura.calcola(griglia, O.x(), O.y());
            int chiusureConQuesta = chiusureAttive + 1;
            if (stats != null) {
                stats.celleChiusuraTotali += g.chiusura().size();
                stats.celleFrontieraTotali += g.frontiera().length;
                stats.chiusureAttivePicco = Math.max(stats.chiusureAttivePicco, chiusureConQuesta);
                stats.memoriaMatriciPiccoBytesStimata =
                        (long) stats.chiusureAttivePicco
                                * griglia.width() * griglia.height() * Integer.BYTES;
            }

            // Caso base (righe 4-8): D appartiene alla chiusura di O.
            int statoD = g.getCellaAt(D.x(), D.y()).stato();
            if (StatoCella.CHIUSURA.is(statoD)) {
                Cammino risultato = new Cammino(
                        DistanzaLibera.from(O.x(), O.y(), D.x(), D.y()),
                        List.of(new Landmark(0, O.x(), O.y()),
                                new Landmark(tipoLandmark(statoD), D.x(), D.y())));
                memorizza(chiave, new Voce(risultato, null));
                return risultato;
            }

            // Righe 12-13. Con il limite propagato si parte dal limite ricevuto.
            DistanzaLibera lunghezzaMin = limite;
            List<Landmark> seqMin = List.of();
            boolean trovato = false;

            List<Candidato> candidati = new ArrayList<>();
            for (ICella2D F : g.frontiera()) {
                DistanzaLibera lF = DistanzaLibera.from(O.x(), O.y(), F.x(), F.y());
                candidati.add(new Candidato(F, lF,
                        lF.add(DistanzaLibera.from(F.x(), F.y(), D.x(), D.y()))));
            }
            if (variante.ordinaFrontiera()) {
                candidati.sort(Comparator.comparing(Candidato::limiteInferiore));
            }

            // Righe 14-22. La frontiera vuota (righe 10-11) non esegue iterazioni.
            for (Candidato c : candidati) {
                ICella2D F = c.F();
                DistanzaLibera lF = c.lF();

                // Riga 16: dlib(O,F) >= lunghezzaMin, F non puo migliorare il minimo.
                if (lF.compareTo(lunghezzaMin) >= 0) {
                    if (stats != null) {
                        stats.iterazioniPruneRiga16++;
                        stats.iterazioniPruningTotali++;
                    }
                    continue;
                }

                // Riga 17: dlib(O,F) + dlib(F,D) e un limite inferiore di ogni cammino per F.
                if (c.limiteInferiore().compareTo(lunghezzaMin) >= 0) {
                    if (stats != null) {
                        stats.iterazioniPruneRiga17++;
                        stats.iterazioniPruningTotali++;
                    }
                    continue;
                }

                // Riga 18: la chiusura di O diventa ostacolo, F resta libera perche
                // e l'origine della chiamata annidata.
                IGriglia<?> g2 = griglia.addObstacle(() -> g.chiusura().stream()
                        .filter(cella -> cella.x() != F.x() || cella.y() != F.y())
                        .toList());
                DistanzaLibera limiteFiglio = variante.limitePropagato()
                        ? residuo(lunghezzaMin, lF) : DistanzaLibera.INFINITY;
                Cammino camminoFD = ricorsiva(g2, F, D, profondita + 1, chiusureConQuesta,
                        limiteFiglio);

                // Righe 19-22.
                DistanzaLibera lTot = lF.add(camminoFD.distanza());
                if (lTot.compareTo(lunghezzaMin) < 0) {
                    lunghezzaMin = lTot;
                    trovato = true;
                    List<Landmark> seq = new ArrayList<>();
                    seq.add(new Landmark(0, O.x(), O.y()));
                    seq.add(new Landmark(tipoLandmark(F.stato()), F.x(), F.y()));
                    // COMPATTA: il primo landmark del sottocammino e (F,0) e viene
                    // sostituito da (F,t).
                    seq.addAll(camminoFD.landmarks().subList(1, camminoFD.landmarks().size()));
                    seqMin = seq;
                }
            }

            if (!trovato) {
                // Nessun cammino piu corto del limite (infinito senza limite propagato).
                memorizza(chiave, new Voce(null, limite));
                return Cammino.INFINITY;
            }
            // Un cammino trovato sotto il limite e il minimo del sottoproblema.
            Cammino risultato = new Cammino(lunghezzaMin, seqMin);
            memorizza(chiave, new Voce(risultato, null));
            return risultato;
        }

        private void memorizza(Chiave chiave, Voce voce) {
            if (chiave != null) cache.put(chiave, voce);
        }
    }

    /** Limite per la chiamata annidata: lunghezzaMin - dlib(O,F). */
    private static DistanzaLibera residuo(DistanzaLibera lunghezzaMin, DistanzaLibera lF) {
        if (Double.isInfinite(lunghezzaMin.distanza())) return DistanzaLibera.INFINITY;
        return new DistanzaLibera(
                lunghezzaMin.distanzaTorre() - lF.distanzaTorre(),
                lunghezzaMin.distanzaAlfiere() - lF.distanzaAlfiere(),
                lunghezzaMin.distanza() - lF.distanza());
    }

    /** Converte la bitmask interna nel tipo logico richiesto dalla consegna. */
    private static int tipoLandmark(int stato) {
        if (StatoCella.CONTESTO.is(stato)) return 1;
        if (StatoCella.COMPLEMENTO.is(stato)) return 2;
        throw new IllegalArgumentException("Il landmark non appartiene alla chiusura");
    }

    private static void validaInput(IGriglia<?> griglia, ICella2D O, ICella2D D) {
        Objects.requireNonNull(griglia, "La griglia non puo essere null");
        Objects.requireNonNull(O, "L'origine non puo essere null");
        Objects.requireNonNull(D, "La destinazione non puo essere null");
        if (griglia.width() <= 0 || griglia.height() <= 0) {
            throw new IllegalArgumentException("La griglia deve avere dimensioni positive");
        }
        validaCoordinata(griglia, O, "origine");
        validaCoordinata(griglia, D, "destinazione");
    }

    private static void validaCoordinata(IGriglia<?> griglia, ICella2D cella, String nome) {
        if (cella.x() < griglia.xmin() || cella.x() > griglia.xmax()
                || cella.y() < griglia.ymin() || cella.y() > griglia.ymax()) {
            throw new IllegalArgumentException(
                    "Coordinate " + nome + " fuori griglia: (" + cella.x() + "," + cella.y() + ")");
        }
    }
}
