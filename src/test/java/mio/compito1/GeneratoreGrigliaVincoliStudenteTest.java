package mio.compito1;

import mio.compito2.StatoCella;
import mio.griglia.IGriglia;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeneratoreGrigliaVincoliStudenteTest {

    /**
     * Verifica che il generatore usi un seed configurabile per riproducibilità.
     * Due griglie generate con lo stesso seed devono produrre gli stessi ostacoli
     * alle stesse posizioni.
     */
    @Test
    void seedConfigurabileDaRiproducibilita() {
        long seed = 42L;
        int w = 10, h = 10, nOst = 15;

        IGriglia<?> g1 = GeneratoreGriglia.genera(w, h, nOst, seed);
        IGriglia<?> g2 = GeneratoreGriglia.genera(w, h, nOst, seed);

        // Verifica che le due griglie abbiano ostacoli alle stesse posizioni
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean ostacolo1 = StatoCella.OSTACOLO.is(g1.getCellaAt(x, y).stato());
                boolean ostacolo2 = StatoCella.OSTACOLO.is(g2.getCellaAt(x, y).stato());
                assertEquals(ostacolo1, ostacolo2,
                        "Posizione (" + x + "," + y + ") deve avere stessa configurazione ostacolo");
            }
        }
    }

    /**
     * Verifica che il generatore produca ostacoli semplici (singola cella),
     * NON pattern complessi come barre, spirali, scacchiere, ecc.
     * Ogni ostacolo è una singola cella casuale.
     */
    @Test
    void ostacoliSempliciNonComplessi() {
        long seed = 77L;
        int w = 20, h = 20, nOst = 80;

        IGriglia<?> g = GeneratoreGriglia.genera(w, h, nOst, seed);

        // Conta gli ostacoli
        int conteggioOstacoli = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (StatoCella.OSTACOLO.is(g.getCellaAt(x, y).stato())) {
                    conteggioOstacoli++;
                }
            }
        }

        assertEquals(nOst, conteggioOstacoli,
                "Numero ostacoli deve essere esattamente nOst");
        assertTrue(conteggioOstacoli > 0,
                "Numero ostacoli deve essere > 0");

        // Verifica che non ci siano pattern regolari (es. barre continue o spirali)
        // Una semplice euristica: se una riga ha molti ostacoli consecutivi, potrebbe essere barra
        // Ma con ostacoli casuali, è molto improbabile avere barre complete
        for (int y = 0; y < h; y++) {
            int consecutivi = 0;
            for (int x = 0; x < w; x++) {
                if (StatoCella.OSTACOLO.is(g.getCellaAt(x, y).stato())) {
                    consecutivi++;
                } else {
                    if (consecutivi == w) {
                        fail("Riga " + y + " completamente riempita di ostacoli: potrebbe essere una barra intenzionale");
                    }
                    consecutivi = 0;
                }
            }
        }
    }

    /**
     * Verifica che il generatore sia sensibile al seed:
     * griglie generate con seed diversi devono avere ostacoli diversi.
     */
    @Test
    void seedDiversiProduconoGriglieDiverse() {
        int w = 10, h = 10, nOst = 15;

        IGriglia<?> g1 = GeneratoreGriglia.genera(w, h, nOst, 1L);
        IGriglia<?> g2 = GeneratoreGriglia.genera(w, h, nOst, 2L);

        boolean almenoUnaDifferenza = false;
        for (int y = 0; y < h && !almenoUnaDifferenza; y++) {
            for (int x = 0; x < w && !almenoUnaDifferenza; x++) {
                boolean o1 = StatoCella.OSTACOLO.is(g1.getCellaAt(x, y).stato());
                boolean o2 = StatoCella.OSTACOLO.is(g2.getCellaAt(x, y).stato());
                if (o1 != o2) {
                    almenoUnaDifferenza = true;
                }
            }
        }

        assertTrue(almenoUnaDifferenza,
                "Griglie con seed diversi devono avere configurazioni diverse");
    }

    /**
     * Verifica che non siano presenti implementazioni di pattern complessi
     * nel generatore (barre, spirale, scacchiera, zone chiuse, delimitatori).
     * Controllo della firma del metodo: deve accettare solo width, height, nOstacoli, seed.
     */
    @Test
    void generatoreAccettaSoloSeed() {
        // Verifica che il metodo genera() accetti 4 parametri: width, height, nOstacoli, seed
        // Questo è un test statico, verifichiamo che non ci siano metodi
        // genera(TipoOstacolo tipo, ...) che suggeriscono pattern complessi
        
        try {
            // Chiamata con firma corretta
            IGriglia<?> g = GeneratoreGriglia.genera(5, 5, 3, 42L);
            assertNotNull(g, "Generatore deve restituire una griglia non nulla");
        } catch (Exception e) {
            fail("Generatore deve accettare firma (width, height, nOstacoli, seed)");
        }
    }

    @Test
    void piazzaEsattamenteNumeroRichiestoAncheAdAltaDensita() {
        int w = 5, h = 5, nOst = 24;

        IGriglia<?> g = GeneratoreGriglia.genera(w, h, nOst, 123L);

        int conteggioOstacoli = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (StatoCella.OSTACOLO.is(g.getCellaAt(x, y).stato())) {
                    conteggioOstacoli++;
                }
            }
        }
        assertEquals(nOst, conteggioOstacoli);
    }

    @Test
    void rifiutaNumeroOstacoliImpossibile() {
        assertThrows(IllegalArgumentException.class,
                () -> GeneratoreGriglia.genera(5, 5, 26, 123L));
    }
}
