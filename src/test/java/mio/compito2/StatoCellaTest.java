package mio.compito2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatoCellaTest {

    @Test
    void bitmaskPureRispettanoLaSpecifica() {
        assertEquals(0b00000001, StatoCella.CONTESTO.value);
        assertEquals(0b00000010, StatoCella.REGINA.value);
        assertEquals(0b00000100, StatoCella.ORIGINE.value);
        assertEquals(0b00001000, StatoCella.COMPLEMENTO.value);
        assertEquals(0b00010000, StatoCella.FRONTIERA.value);
        assertEquals(0b10000000, StatoCella.OSTACOLO.value);
    }

    @Test
    void bitOstacoloDellaGrigliaCoincideConStatoCella() {
        assertEquals(StatoCella.OSTACOLO.value, mio.griglia.IGriglia.BIT_OSTACOLO);
    }

    @Test
    void reginaDaSolaNonFaParteDellaChiusura() {
        int statoSoloRegina = StatoCella.REGINA.addTo(0);
        assertFalse(StatoCella.CHIUSURA.is(statoSoloRegina));
    }

    @Test
    void origineDaSolaNonFaParteDellaChiusura() {
        int statoOrigine = StatoCella.ORIGINE.addTo(0);
        assertFalse(StatoCella.CHIUSURA.is(statoOrigine));
    }

    @Test
    void chiusuraUsaSoloContestoOComplemento() {
        assertTrue(StatoCella.CHIUSURA.is(StatoCella.CONTESTO.addTo(0)));
        assertTrue(StatoCella.CHIUSURA.is(StatoCella.COMPLEMENTO.addTo(0)));
    }
}
