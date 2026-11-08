package mio.compito4;

import mio.compito3.Landmark;

import java.util.List;

public class StatisticheEsecuzione {
    public String  gridId;
    /** Nome della variante di CamminoMin (vedi VarianteCamminoMin). */
    public String  variante = "base";
    public String  tipoGriglia;
    public int     width;
    public int     height;
    public int     nOstacoli;
    public long    seed;
    public int     Ox;
    public int     Oy;
    public int     Dx;
    public int     Dy;
    public double  lunghezzaOD;
    public double  lunghezzaDO;
    /** Sequenze restituite da CamminoMin; vuote quando la relativa distanza e infinita. */
    public List<Landmark> sequenzaLandmarkOD = List.of();
    public List<Landmark> sequenzaLandmarkDO = List.of();
    public boolean correttezza;
    /** Celle di frontiera di tutte le chiusure calcolate (CSV: frontieraTot). */
    public int     celleFrontieraTotali;
    /** Volte in cui la condizione di riga 16 e risultata falsa (CSV: falsiRiga16). */
    public int     iterazioniPruneRiga16;
    /** Volte in cui la riga 16 era vera ma la condizione di riga 17 falsa (CSV: falsiRiga17). */
    public int     iterazioniPruneRiga17;
    /** Rami potati in totale, riga 16 piu riga 17 (CSV: pruningTotale). */
    public int     iterazioniPruningTotali;
    public long    chiamateRicorsive;
    public long    cacheHit;
    public int     profonditaMassima;
    /** Numero massimo di chiusure int[][] realmente allocate e attive nello stesso momento. */
    public int     chiusureAttivePicco;
    public long    celleChiusuraTotali;
    /** Stima del solo payload int[][] delle chiusure contemporaneamente attive. */
    public long    memoriaMatriciPiccoBytesStimata;
    public int     warmupEseguiti;
    public int     ripetizioniTempo;
    public long    tempoMinNs;
    public long    tempoMedianoNs;
    public long    tempoMaxNs;
    /** Tempo della singola esecuzione; dopo il protocollo ripetuto coincide con la mediana. */
    public long    tempoNs;
}
