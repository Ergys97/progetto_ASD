package mio.compito2;

import mio.compito1.GeneratoreGriglia;
import mio.griglia.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalcoloChiusuraTest {

    /** Griglia 5×5 senza ostacoli: O al centro (2,2).
     *  La chiusura di (2,2) deve coprire tutte le 25 celle. */
    @Test void chiusuraCompletaSenzaOstacoli() {
        IGriglia<?> g = GeneratoreGriglia.genera(5, 5, 0, 0L);
        GrigliaConOrigine gco = CalcoloChiusura.calcola(g, 2, 2);
        assertEquals(25, gco.chiusura().size());
    }

    /** L'origine deve avere il bit ORIGINE settato. */
    @Test void origineMarcata() {
        IGriglia<?> g = GeneratoreGriglia.genera(5, 5, 0, 0L);
        GrigliaConOrigine gco = CalcoloChiusura.calcola(g, 2, 2);
        assertTrue(StatoCella.ORIGINE.is(gco.getCellaAt(2, 2).stato()));
    }

    /** La frontiera considera solo vicini reali interni alla griglia. */
    @Test void frontieraVuotaQuandoLaChiusuraCopreTuttaLaGriglia() {
        IGriglia<?> g = GeneratoreGriglia.genera(5, 5, 0, 0L);
        GrigliaConOrigine gco = CalcoloChiusura.calcola(g, 2, 2);
        assertEquals(0, gco.frontiera().length);
    }

    /** Una cella allineata cardinalmente è raggiunta da un cammino di tipo 1. */
    @Test void asseCardinaleAppartieneAlContesto() {
        GrigliaConOrigine gco = CalcoloChiusura.calcola(GrigliaMatrix.vuota(5, 5), 2, 2);
        int stato = gco.getCellaAt(4, 2).stato();
        assertTrue(StatoCella.CONTESTO.is(stato));
        assertFalse(StatoCella.COMPLEMENTO.is(stato));
    }

    /** Se entrambi i raggruppamenti sono liberi, la cella non è nel complemento esclusivo. */
    @Test void cellaRaggiungibileDaEntrambiRestaNelContesto() {
        GrigliaConOrigine gco = CalcoloChiusura.calcola(GrigliaMatrix.vuota(5, 5), 2, 2);
        int stato = gco.getCellaAt(4, 3).stato();
        assertTrue(StatoCella.CONTESTO.is(stato));
        assertFalse(StatoCella.COMPLEMENTO.is(stato));
    }

    /** Il complemento contiene le celle raggiungibili solo dal raggruppamento di tipo 2. */
    @Test void complementoContieneSoloLeCelleDiTipoDue() {
        IGriglia<?> g = GrigliaMatrix.vuota(4, 3).addObstacle(
                () -> java.util.List.of(new Cella2D(0, 1, 1)));
        GrigliaConOrigine gco = CalcoloChiusura.calcola(g, 0, 0);
        int stato = gco.getCellaAt(2, 1).stato();
        assertFalse(StatoCella.CONTESTO.is(stato));
        assertTrue(StatoCella.COMPLEMENTO.is(stato));
    }

    /** Con O all'angolo (0,0), la chiusura copre comunque tutte le celle raggiungibili. */
    @Test void origineAngolo() {
        IGriglia<?> g = GeneratoreGriglia.genera(5, 5, 0, 0L);
        GrigliaConOrigine gco = CalcoloChiusura.calcola(g, 0, 0);
        assertEquals(25, gco.chiusura().size());
    }

    /** convertiChiusuraInOstacolo() deve restituire la lista di chiusura. */
    @Test void convertiChiusuraInOstacolo() {
        IGriglia<?> g = GeneratoreGriglia.genera(5, 5, 0, 0L);
        GrigliaConOrigine gco = CalcoloChiusura.calcola(g, 2, 2);
        IObstacle obs = gco.convertiChiusuraInOstacolo();
        assertEquals(gco.chiusura(), obs.list());
    }
}
