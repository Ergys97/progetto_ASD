package mio.compito4;

import mio.compito1.GeneratoreGriglia;
import mio.compito2.StatoCella;
import mio.griglia.Cella2D;
import mio.griglia.ICella2D;
import mio.griglia.IGriglia;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SperimentazioneRipetutaTest {

    @Test
    void protocolloUnoCinqueProduceMedianaOrdinataERisultatiDeterministici() {
        IGriglia<?> griglia = GeneratoreGriglia.genera(8, 8, 10, 12345L);
        ICella2D origine = trovaCellaLibera(griglia, 0, 0);
        ICella2D destinazione = trovaCellaLibera(griglia, 7, 7);

        StatisticheEsecuzione prima = Sperimentazione.eseguiRipetuto(
                "ripetuto", griglia, origine, destinazione, 1, 5);
        StatisticheEsecuzione seconda = Sperimentazione.eseguiRipetuto(
                "ripetuto", griglia, origine, destinazione, 1, 5);

        assertAll(
                () -> assertEquals(1, prima.warmupEseguiti),
                () -> assertEquals(5, prima.ripetizioniTempo),
                () -> assertTrue(prima.tempoMinNs <= prima.tempoMedianoNs),
                () -> assertTrue(prima.tempoMedianoNs <= prima.tempoMaxNs),
                () -> assertEquals(prima.tempoMedianoNs, prima.tempoNs),
                () -> assertEquals(prima.lunghezzaOD, seconda.lunghezzaOD),
                () -> assertEquals(prima.lunghezzaDO, seconda.lunghezzaDO),
                () -> assertEquals(prima.sequenzaLandmarkOD, seconda.sequenzaLandmarkOD),
                () -> assertEquals(prima.sequenzaLandmarkDO, seconda.sequenzaLandmarkDO),
                () -> assertEquals(prima.correttezza, seconda.correttezza),
                () -> assertEquals(prima.celleFrontieraTotali, seconda.celleFrontieraTotali),
                () -> assertEquals(prima.iterazioniPruneRiga16, seconda.iterazioniPruneRiga16),
                () -> assertEquals(prima.iterazioniPruneRiga17, seconda.iterazioniPruneRiga17),
                () -> assertEquals(prima.chiamateRicorsive, seconda.chiamateRicorsive),
                () -> assertEquals(prima.cacheHit, seconda.cacheHit),
                () -> assertEquals(prima.profonditaMassima, seconda.profonditaMassima),
                () -> assertEquals(prima.chiusureAttivePicco, seconda.chiusureAttivePicco),
                () -> assertEquals(prima.memoriaMatriciPiccoBytesStimata,
                        seconda.memoriaMatriciPiccoBytesStimata)
        );
    }

    private static ICella2D trovaCellaLibera(IGriglia<?> griglia, int cx, int cy) {
        for (int r = 0; r <= Math.max(griglia.width(), griglia.height()); r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    int x = cx + dx;
                    int y = cy + dy;
                    if (x < griglia.xmin() || x > griglia.xmax()
                            || y < griglia.ymin() || y > griglia.ymax()) continue;
                    if (StatoCella.OSTACOLO.isNot(griglia.getCellaAt(x, y).stato())) {
                        return new Cella2D(0, x, y);
                    }
                }
            }
        }
        throw new IllegalStateException("Nessuna cella libera");
    }
}
