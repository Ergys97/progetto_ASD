package mio.compito3;

import mio.compito1.GeneratoreGriglia;
import mio.compito4.Sperimentazione;
import mio.compito4.StatisticheEsecuzione;
import mio.griglia.Cella2D;
import mio.griglia.ICella2D;
import mio.griglia.IGriglia;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VariantiCamminoMinTest {

    /** Istanza della fig. 1 della relazione: griglia 12x12, 15 ostacoli, seed 50. */
    private static final IGriglia<?> GRIGLIA_FIGURA = GeneratoreGriglia.genera(12, 12, 15, 50L);
    private static final ICella2D O_FIGURA = new Cella2D(0, 0, 0);
    private static final ICella2D D_FIGURA = new Cella2D(0, 10, 10);

    @Test
    void varianteBaseRiproduceIlRiassuntoDellaRelazione() {
        StatisticheEsecuzione s = Sperimentazione.esegui(
                "fig1", GRIGLIA_FIGURA, O_FIGURA, D_FIGURA, false, VarianteCamminoMin.BASE);

        double attesa = 9 * Math.sqrt(2.0) + 2;
        assertAll(
                () -> assertEquals("base", s.variante),
                () -> assertEquals(attesa, s.lunghezzaOD, 1e-9),
                () -> assertEquals(attesa, s.lunghezzaDO, 1e-9),
                () -> assertEquals(List.of(new Landmark(0, 0, 0), new Landmark(1, 8, 7),
                        new Landmark(1, 10, 10)), s.sequenzaLandmarkOD),
                () -> assertEquals(List.of(new Landmark(0, 10, 10), new Landmark(2, 0, 1),
                        new Landmark(1, 0, 0)), s.sequenzaLandmarkDO),
                () -> assertEquals(118, s.celleFrontieraTotali),
                () -> assertEquals(1, s.iterazioniPruneRiga16),
                () -> assertEquals(96, s.iterazioniPruneRiga17),
                () -> assertEquals(97, s.iterazioniPruningTotali),
                () -> assertEquals(18, s.chiamateRicorsive),
                () -> assertEquals(4, s.profonditaMassima)
        );
    }

    @Test
    void tutteLeVariantiRestituisconoLaStessaLunghezza() {
        for (VarianteCamminoMin v : VarianteCamminoMin.values()) {
            Cammino c = CamminoMin.calcola(GRIGLIA_FIGURA, O_FIGURA, D_FIGURA, null, v);
            assertEquals(9 * Math.sqrt(2.0) + 2, c.distanza().distanza(), 1e-9, "variante " + v);
        }
    }

    @Test
    void limitePropagatoRiduceLeChiamateSenzaCambiareLaLunghezza() {
        // Istanza G20x20_d20_s100_p1 del piano: la piu costosa per la variante BASE.
        IGriglia<?> g = GeneratoreGriglia.genera(20, 20, 80, 100L);
        ICella2D o = new Cella2D(0, 0, 0);
        ICella2D d = trovaLibera(g, 18, 18);

        StatisticheEsecuzione base = Sperimentazione.esegui(
                "base", g, o, d, false, VarianteCamminoMin.BASE);
        StatisticheEsecuzione limite = Sperimentazione.esegui(
                "limite", g, o, d, false, VarianteCamminoMin.LIMITE_PROPAGATO);

        assertEquals(base.lunghezzaOD, limite.lunghezzaOD, 1e-9);
        assertEquals(base.lunghezzaDO, limite.lunghezzaDO, 1e-9);
        assertTrue(limite.correttezza);
        assertTrue(limite.chiamateRicorsive < base.chiamateRicorsive,
                "chiamate: limite=" + limite.chiamateRicorsive + ", base=" + base.chiamateRicorsive);
    }

    @Test
    void senzaCacheNonServeChiamateDallaCache() {
        StatisticheEsecuzione s = Sperimentazione.esegui(
                "nocache", GRIGLIA_FIGURA, O_FIGURA, D_FIGURA, false, VarianteCamminoMin.SENZA_CACHE);
        assertEquals(0, s.cacheHit);
        assertEquals("senza-cache", s.variante);
    }

    @Test
    void nomiDelleVariantiSonoConvertibili() {
        for (VarianteCamminoMin v : VarianteCamminoMin.values()) {
            assertEquals(v, VarianteCamminoMin.daNome(v.nome()));
        }
        assertEquals(VarianteCamminoMin.LIMITE_PROPAGATO, VarianteCamminoMin.daNome(" Limite-Propagato "));
        assertThrows(IllegalArgumentException.class, () -> VarianteCamminoMin.daNome("veloce"));
    }

    private static ICella2D trovaLibera(IGriglia<?> g, int cx, int cy) {
        for (int r = 0; ; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    int x = cx + dx, y = cy + dy;
                    if (x < 0 || y < 0 || x >= g.width() || y >= g.height()) continue;
                    if (!g.isOstacolo(x, y)) return new Cella2D(0, x, y);
                }
            }
        }
    }
}
