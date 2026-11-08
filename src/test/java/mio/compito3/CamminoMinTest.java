package mio.compito3;

import mio.compito1.GeneratoreGriglia;
import mio.compito2.DistanzaLibera;
import mio.griglia.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CamminoMinTest {

    /** D è nella chiusura di O: il cammino deve avere distanza dlib(O,D). */
    @Test void casoBaseDestinazionInChiusura() {
        // Griglia 5×5 senza ostacoli; O=(0,0), D=(2,0) — stessa riga, sicuramente in chiusura
        IGriglia<?> g = GeneratoreGriglia.genera(5, 5, 0, 0L);
        ICella2D O = new Cella2D(0, 0, 0);
        ICella2D D = new Cella2D(0, 2, 0);
        Cammino c = CamminoMin.calcola(g, O, D);
        assertNotEquals(Cammino.INFINITY, c);
        assertEquals(DistanzaLibera.from(0, 0, 2, 0).distanza(), c.distanza().distanza(), DistanzaLibera.EPSILON);
        assertEquals(2, c.landmarks().size());
        assertEquals(0, c.landmarks().get(0).stato());
        assertEquals(1, c.landmarks().get(1).stato());
    }

    /** CamminoMin(O,D) == CamminoMin(D,O) su griglia semplice. */
    @Test void simmetria() {
        IGriglia<?> g = GeneratoreGriglia.genera(10, 10, 5, 42L);
        ICella2D O = new Cella2D(0, 1, 1);
        ICella2D D = new Cella2D(0, 8, 8);
        Cammino od = CamminoMin.calcola(g, O, D);
        Cammino dO = CamminoMin.calcola(g, D, O);
        assertEquals(od.distanza().distanza(), dO.distanza().distanza(), DistanzaLibera.EPSILON);
    }

    /** D irraggiungibile (circondato da ostacoli) → restituisce INFINITY. */
    @Test void destinazioneIrraggiungibile() {
        // Costruiamo una griglia 5×5, poi aggiungiamo ostacoli attorno a D=(4,4)
        IGriglia<?> g = GeneratoreGriglia.genera(5, 5, 0, 0L);
        // Aggiungiamo ostacoli su (3,3),(3,4),(4,3) — D=(4,4) diventa angolo irraggiungibile
        IObstacle muro = () -> java.util.List.of(
                new Cella2D(0, 3, 3), new Cella2D(0, 3, 4), new Cella2D(0, 4, 3));
        IGriglia<?> gConMuro = g.addObstacle(muro);
        ICella2D O = new Cella2D(0, 0, 0);
        ICella2D D = new Cella2D(0, 4, 4);
        Cammino c = CamminoMin.calcola(gConMuro, O, D);
        assertEquals(Cammino.INFINITY, c);
    }

    /** Il tipo del landmark destinazione distingue il solo complemento. */
    @Test void destinazioneNelSoloComplementoHaTipoDue() {
        IGriglia<?> g = GrigliaMatrix.vuota(4, 3).addObstacle(
                () -> java.util.List.of(new Cella2D(0, 1, 1)));

        Cammino c = CamminoMin.calcola(g, new Cella2D(0, 0, 0), new Cella2D(0, 2, 1));

        assertNotEquals(Cammino.INFINITY, c);
        assertEquals(2, c.landmarks().size());
        assertEquals(0, c.landmarks().get(0).stato());
        assertEquals(2, c.landmarks().get(1).stato());
    }

    /** Ogni landmark dopo l'origine espone il tipo logico 1 o 2, non la bitmask interna. */
    @Test void tuttiILandmarkDopoLOrigineHannoTipoUnoODue() {
        IGriglia<?> g = GrigliaMatrix.vuota(5, 5).addObstacle(
                () -> java.util.List.of(new Cella2D(0, 1, 1)));

        Cammino c = CamminoMin.calcola(g, new Cella2D(0, 0, 0), new Cella2D(0, 4, 4));

        assertNotEquals(Cammino.INFINITY, c);
        assertTrue(c.landmarks().size() > 2);
        assertEquals(0, c.landmarks().get(0).stato());
        assertTrue(c.landmarks().stream().skip(1)
                .allMatch(l -> l.stato() == 1 || l.stato() == 2));
    }
}
