package mio.compito4;

import mio.compito1.GeneratoreGriglia;
import mio.compito2.StatoCella;
import mio.griglia.Cella2D;
import mio.griglia.ICella2D;
import mio.griglia.IGriglia;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ValidazioneRiasuntoTest {

    private static ICella2D trovaCellaLibera(IGriglia<?> g, int cx, int cy) {
        int x0 = g.xmin();
        int y0 = g.ymin();
        int xmax = g.xmin() + g.width() - 1;
        int ymax = g.ymin() + g.height() - 1;

        for (int r = 0; r <= Math.max(g.width(), g.height()); r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) {
                        continue;
                    }
                    int x = cx + dx;
                    int y = cy + dy;
                    if (x < x0 || x > xmax || y < y0 || y > ymax) {
                        continue;
                    }
                    if (StatoCella.OSTACOLO.isNot(g.getCellaAt(x, y).stato())) {
                        return new Cella2D(0, x, y);
                    }
                }
            }
        }

        throw new IllegalStateException("Nessuna cella libera nella griglia");
    }

    @Test
    void riasuntoCompleto() {
        // Genera griglia complessa: 20x20 con 50 ostacoli casuali
        int width = 20;
        int height = 20;
        int ostacoli = 50;
        long seed = 77L;

        IGriglia<?> g = GeneratoreGriglia.genera(width, height, ostacoli, seed);
        ICella2D O = trovaCellaLibera(g, width / 2, height / 2);
        ICella2D D = trovaCellaLibera(g, width - 2, height - 2);

        // Esegui sperimentazione e raccogli statistiche
        StatisticheEsecuzione stats = Sperimentazione.esegui(g, O, D);

        // ===== VALIDAZIONI =====

        // 1. Dimensioni della griglia (casuale)
        assertEquals(width, stats.width);
        assertEquals(height, stats.height);
        // nOstacoli non è settato da Sperimentazione.esegui(), ma manualmente in Main.java

        // 2. Numero totale di celle di frontiera nell'intera esecuzione
        assertTrue(stats.celleFrontieraTotali > 0,
                "Celle frontiera totali deve essere > 0");

        // 3. Conteggio riga 16 (pruning su dlib(O,F) >= min)
        assertTrue(stats.iterazioniPruneRiga16 >= 0,
                "Iterazioni prune riga 16 deve essere >= 0");

        // 4. Conteggio riga 17 (pruning su bound >= min)
        assertTrue(stats.iterazioniPruneRiga17 >= 0,
                "Iterazioni prune riga 17 deve essere >= 0");

        // 5. Totale condizione falsa = riga 16 + riga 17
        assertEquals(stats.iterazioniPruneRiga16 + stats.iterazioniPruneRiga17,
                stats.iterazioniPruningTotali,
                "Somma pruning deve = totale condizione falsa");

        // 6. Tempo di esecuzione deve essere positivo
        assertTrue(stats.tempoNs > 0,
                "Tempo esecuzione deve essere > 0 ns");

        // 7. Prestazioni spaziali e struttura della ricorsione
        assertTrue(stats.chiamateRicorsive >= 2,
                "Devono essere conteggiate almeno le esecuzioni OD e DO");
        assertTrue(stats.profonditaMassima >= 1,
                "La profondità massima deve includere la chiamata radice");
        assertTrue(stats.celleChiusuraTotali > 0,
                "Le celle di chiusura elaborate devono essere conteggiate");
        assertTrue(stats.chiusureAttivePicco >= 1,
                "Il picco deve includere almeno la chiusura radice");
        assertTrue(stats.memoriaMatriciPiccoBytesStimata >= (long) width * height * Integer.BYTES,
                "La stima spaziale deve includere almeno una matrice di chiusura");
        assertEquals((long) stats.chiusureAttivePicco * width * height * Integer.BYTES,
                stats.memoriaMatriciPiccoBytesStimata,
                "La memoria stimata deve derivare dalle chiusure realmente attive al picco");

        // 8. Simmetria
        assertTrue(stats.correttezza,
                "Correttezza (simmetria O^D e D^O) deve essere vera");

        // ===== STAMPA RIASSUNTO STRUTTURATO =====
        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("RIASSUNTO ESECUZIONE COMPLESSA");
        System.out.println("=".repeat(80));
        System.out.println("Dimensioni griglia (casuale): " + stats.width + "x" + stats.height);
        System.out.println("Numero ostacoli (attesi): " + ostacoli);
        System.out.println("Seed: " + seed);
        System.out.println();
        System.out.println("Origine: (" + O.x() + ", " + O.y() + ")");
        System.out.println("Destinazione: (" + D.x() + ", " + D.y() + ")");
        System.out.println();
        System.out.println("--- METRICHE DI ESECUZIONE ---");
        System.out.println("Celle di frontiera totale individuate: " + stats.celleFrontieraTotali);
        System.out.println("Iterazioni pruning riga 16 (lF >= min): " + stats.iterazioniPruneRiga16);
        System.out.println("Iterazioni pruning riga 17 (bound >= min): " + stats.iterazioniPruneRiga17);
        System.out.println("Iterazioni condizione falsa (totale): " + stats.iterazioniPruningTotali);
        System.out.println("Chiamate ricorsive: " + stats.chiamateRicorsive);
        System.out.println("Profondita massima: " + stats.profonditaMassima);
        System.out.println("Memoria matrici di chiusura al picco (stima): "
                + stats.memoriaMatriciPiccoBytesStimata + " byte");
        System.out.println();
        System.out.println("Tempo esecuzione: " + stats.tempoNs + " ns (" +
                String.format(Locale.US, "%.6f", stats.tempoNs / 1_000_000.0) + " ms)");
        System.out.println();
        System.out.println("--- RISULTATI CAMMINO ---");
        System.out.println("Lunghezza O->D: " + String.format(Locale.US, "%.6f", stats.lunghezzaOD));
        System.out.println("Lunghezza D->O: " + String.format(Locale.US, "%.6f", stats.lunghezzaDO));
        System.out.println("Simmetria verificata: " + stats.correttezza);
        System.out.println("=".repeat(80));
        System.out.println();
    }
}
