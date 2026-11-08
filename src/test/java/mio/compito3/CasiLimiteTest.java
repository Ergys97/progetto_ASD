package mio.compito3;

import mio.compito1.GeneratoreGriglia;
import mio.compito2.DistanzaLibera;
import mio.compito2.StatoCella;
import mio.griglia.Cella2D;
import mio.griglia.GrigliaMatrix;
import mio.griglia.ICella2D;
import mio.griglia.IGriglia;
import mio.griglia.IObstacle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CasiLimiteTest {

    /**
     * Caso 1: Identità O = D
     * Atteso: lunghezza = 0, sequenza ha 2 landmark con stessa posizione
     */
    @Test
    void casoIdentita() {
        IGriglia<?> g = GrigliaMatrix.vuota(5, 5);
        ICella2D O = new Cella2D(0, 2, 2);
        ICella2D D = new Cella2D(0, 2, 2); // O = D

        Cammino c = CamminoMin.calcola(g, O, D);

        assertEquals(0.0, c.distanza().distanza(), DistanzaLibera.EPSILON);
        assertEquals(2, c.landmarks().size());
        assertEquals(O.x(), c.landmarks().get(0).x());
        assertEquals(O.y(), c.landmarks().get(0).y());
        assertEquals(D.x(), c.landmarks().get(1).x());
        assertEquals(D.y(), c.landmarks().get(1).y());
        assertEquals(0, c.landmarks().get(0).stato());
        assertEquals(1, c.landmarks().get(1).stato());
    }

    /**
     * Caso 2a: Irraggiungibilità per D circondato da ostacoli
     * Atteso: lunghezza = ∞, sequenza vuota
     */
    @Test
    void casoIrraggiungibilitaCircondato() {
        IGriglia<?> g = GrigliaMatrix.vuota(5, 5);
        ICella2D O = new Cella2D(0, 0, 0);
        ICella2D D = new Cella2D(0, 4, 4);

        // Circonda D con un muro completo su 8 vicini
        IObstacle muro = () -> List.of(
                new Cella2D(0, 3, 3), new Cella2D(0, 4, 3), new Cella2D(0, 5, 3),
                new Cella2D(0, 3, 4),                         new Cella2D(0, 5, 4),
                new Cella2D(0, 3, 5), new Cella2D(0, 4, 5), new Cella2D(0, 5, 5)
        );
        IGriglia<?> conMuro = g.addObstacle(muro);

        Cammino c = CamminoMin.calcola(conMuro, O, D);

        assertTrue(Double.isInfinite(c.distanza().distanza()));
        assertEquals(0, c.landmarks().size());
    }

    /**
     * Caso 2b: Irraggiungibilità per D irraggiungibile completamente
     * (destinazione stessa è un ostacolo)
     * Atteso: lunghezza = ∞, sequenza vuota
     */
    @Test
    void casoIrraggiungibilitaDestinazioneOstacolo() {
        IGriglia<?> g = GrigliaMatrix.vuota(5, 5);
        ICella2D O = new Cella2D(0, 0, 0);
        ICella2D D = new Cella2D(0, 2, 2);

        // D è un ostacolo diretto
        IObstacle blocco = () -> List.of(new Cella2D(0, 2, 2));
        IGriglia<?> conOstacolo = g.addObstacle(blocco);

        Cammino c = CamminoMin.calcola(conOstacolo, O, D);

        assertTrue(Double.isInfinite(c.distanza().distanza()));
        assertEquals(0, c.landmarks().size());
    }

    /**
     * Caso 3: Caso Base - D è già raggiungibile direttamente dalla chiusura di O
     * (nessuna ricorsione necessaria)
     * Atteso: lunghezza = dlib(O,D), sequenza = [O, D], nessuna ricorsione
     */
    @Test
    void casoCasoBaseDiretto() {
        // Griglia senza ostacoli: tutto è raggiungibile direttamente
        IGriglia<?> g = GeneratoreGriglia.genera(10, 10, 0, 0L);
        ICella2D O = new Cella2D(0, 2, 2);
        ICella2D D = new Cella2D(0, 5, 5);

        Cammino c = CamminoMin.calcola(g, O, D);

        double attesaDlib = DistanzaLibera.from(O.x(), O.y(), D.x(), D.y()).distanza();
        assertEquals(attesaDlib, c.distanza().distanza(), DistanzaLibera.EPSILON);
        // In griglia senza ostacoli, il cammino diretto è il minimo quindi sequence è [O, D]
        assertEquals(2, c.landmarks().size());
        assertEquals(O.x(), c.landmarks().get(0).x());
        assertEquals(D.x(), c.landmarks().get(1).x());
    }

    /**
     * Caso 3b: Caso Base - D è nel contesto ma oltre la frontiera non serve ricorsione profonda
     * Se la griglia è semplice e O,D sono entrambi nella stessa chiusura, nessuna ricorsione.
     */
    @Test
    void casoCasoBaseNoRecursione() {
        // Due punti nella stessa griglia piccola senza ostacoli
        IGriglia<?> g = GrigliaMatrix.vuota(7, 7);
        ICella2D O = new Cella2D(0, 1, 1);
        ICella2D D = new Cella2D(0, 5, 5);

        Cammino c = CamminoMin.calcola(g, O, D);

        assertNotEquals(Cammino.INFINITY, c);
        double attesa = DistanzaLibera.from(O.x(), O.y(), D.x(), D.y()).distanza();
        assertEquals(attesa, c.distanza().distanza(), DistanzaLibera.EPSILON);
    }
}
