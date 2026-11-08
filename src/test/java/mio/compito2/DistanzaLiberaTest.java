package mio.compito2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DistanzaLiberaTest {

    @Test void fromOrigin() {
        var d = DistanzaLibera.from(0, 0, 0, 0);
        assertEquals(DistanzaLibera.ZERO, d);
    }

    @Test void fromCardinale() {
        var d = DistanzaLibera.from(0, 0, 3, 0);
        assertEquals(3, d.distanzaTorre());
        assertEquals(0, d.distanzaAlfiere());
        assertEquals(3.0, d.distanza(), DistanzaLibera.EPSILON);
    }

    @Test void fromDiagonale() {
        var d = DistanzaLibera.from(0, 0, 2, 2);
        assertEquals(0, d.distanzaTorre());
        assertEquals(2, d.distanzaAlfiere());
        assertEquals(2 * Math.sqrt(2), d.distanza(), DistanzaLibera.EPSILON);
    }

    @Test void fromMisto() {
        var d = DistanzaLibera.from(0, 0, 3, 1);
        assertEquals(2, d.distanzaTorre());
        assertEquals(1, d.distanzaAlfiere());
        assertEquals(2 + Math.sqrt(2), d.distanza(), DistanzaLibera.EPSILON);
    }

    @Test void simmetria() {
        var od = DistanzaLibera.from(1, 2, 5, 4);
        var do_ = DistanzaLibera.from(5, 4, 1, 2);
        assertEquals(od.distanza(), do_.distanza(), DistanzaLibera.EPSILON);
    }

    @Test void add() {
        var a = DistanzaLibera.from(0, 0, 2, 0);
        var b = DistanzaLibera.from(0, 0, 3, 0);
        var sum = a.add(b);
        assertEquals(5, sum.distanzaTorre());
        assertEquals(0, sum.distanzaAlfiere());
    }

    @Test void infinity() {
        assertTrue(DistanzaLibera.INFINITY.distanza() > 1e15);
        assertTrue(DistanzaLibera.ZERO.compareTo(DistanzaLibera.INFINITY) < 0);
    }
}
