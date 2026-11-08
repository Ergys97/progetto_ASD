package mio.compito3;

import mio.compito1.GeneratoreGriglia;
import mio.compito2.DistanzaLibera;
import mio.compito2.StatoCella;
import mio.griglia.Cella2D;
import mio.griglia.GrigliaMatrix;
import mio.griglia.ICella2D;
import mio.griglia.IGriglia;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CamminoMinDijkstraOracleTest {

    private static final int NUMERO_ISTANZE = 500;
    private static final long SEED_SCENARI = 0x5A17_2024_25L;

    private static final int[][] DIRS8 = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    @Test
    void camminoMinCoincideConDijkstraSuCinquecentoIstanzeCasualiCinquePerCinque() {
        Random scenari = new Random(SEED_SCENARI);

        for (int istanza = 0; istanza < NUMERO_ISTANZE; istanza++) {
            int nOstacoli = scenari.nextInt(21); // 0..20: resta sempre almeno una coppia libera
            long seedGriglia = scenari.nextLong();
            IGriglia<?> g = GeneratoreGriglia.genera(5, 5, nOstacoli, seedGriglia);
            List<ICella2D> libere = celleLibere(g);

            int indiceOrigine = scenari.nextInt(libere.size());
            int indiceDestinazione = scenari.nextInt(libere.size());
            if (indiceDestinazione == indiceOrigine) {
                indiceDestinazione = (indiceDestinazione + 1) % libere.size();
            }
            ICella2D origine = libere.get(indiceOrigine);
            ICella2D destinazione = libere.get(indiceDestinazione);

            double attesa = dijkstra(g, origine, destinazione);
            for (VarianteCamminoMin variante : VarianteCamminoMin.values()) {
                Cammino cammino = CamminoMin.calcola(g, origine, destinazione, null, variante);
                double ottenuta = cammino.distanza().distanza();
                String contesto = "istanza=" + istanza + ", seed=" + seedGriglia
                        + ", ostacoli=" + nOstacoli + ", O=" + origine + ", D=" + destinazione
                        + ", variante=" + variante;

                if (Double.isInfinite(attesa)) {
                    assertEquals(Double.POSITIVE_INFINITY, ottenuta,
                            "Dijkstra trova INF ma CamminoMin no: " + contesto);
                    assertTrue(cammino.landmarks().isEmpty(),
                            "Un cammino infinito deve avere landmark vuoti: " + contesto);
                } else {
                    assertEquals(attesa, ottenuta, DistanzaLibera.EPSILON,
                            "Distanza diversa da Dijkstra: " + contesto);
                    verificaLandmark(g, origine, destinazione, cammino, contesto);
                }
            }
        }
    }

    @Test
    void attraversamentoDiagonaleTraDueOstacoliAdiacentiRestaConsentito() {
        IGriglia<?> g = GrigliaMatrix.vuota(2, 2).addObstacle(() -> List.of(
                new Cella2D(0, 1, 0),
                new Cella2D(0, 0, 1)
        ));
        ICella2D origine = new Cella2D(0, 0, 0);
        ICella2D destinazione = new Cella2D(0, 1, 1);

        Cammino cammino = CamminoMin.calcola(g, origine, destinazione);

        assertEquals(Math.sqrt(2.0), dijkstra(g, origine, destinazione), DistanzaLibera.EPSILON);
        assertEquals(Math.sqrt(2.0), cammino.distanza().distanza(), DistanzaLibera.EPSILON);
        verificaLandmark(g, origine, destinazione, cammino, "corner crossing 2x2");
    }

    @Test
    void camminoMinCoincideConDijkstraSuSerpentina() {
        // Muri orizzontali a tutta larghezza con un varco alternato a destra e a sinistra:
        // il cammino deve percorrere ogni corridoio e attraversare molti landmark.
        int w = 15;
        int h = 15;
        List<ICella2D> muri = new ArrayList<>();
        boolean varcoADestra = true;
        for (int y = 2; y < h - 1; y += 3) {
            int varco = varcoADestra ? w - 1 : 0;
            for (int x = 0; x < w; x++) {
                if (x != varco) {
                    muri.add(new Cella2D(0, x, y));
                }
            }
            varcoADestra = !varcoADestra;
        }
        IGriglia<?> g = GrigliaMatrix.vuota(w, h).addObstacle(() -> muri);

        confrontaConDijkstraNeiDueVersi(g, new Cella2D(0, 0, 0), new Cella2D(0, 0, h - 1), "serpentina 15x15");
    }

    @Test
    void camminoMinCoincideConDijkstraSuSpirale() {
        // Corridoio largo una cella che si avvolge verso il centro, separato da muri larghi una cella.
        int n = 13;
        boolean[][] corridoio = new boolean[n][n];
        int sinistra = 0;
        int alto = 0;
        int destra = n - 1;
        int basso = n - 1;
        int ultimaX = 0;
        int ultimaY = 0;
        while (sinistra <= destra && alto <= basso) {
            for (int x = sinistra; x <= destra; x++) {
                corridoio[alto][x] = true; ultimaX = x; ultimaY = alto;
            }
            for (int y = alto; y <= basso; y++) {
                corridoio[y][destra] = true; ultimaX = destra; ultimaY = y;
            }
            if (alto == basso || sinistra == destra) {
                break;
            }
            for (int x = destra; x >= sinistra; x--) {
                corridoio[basso][x] = true; ultimaX = x; ultimaY = basso;
            }
            for (int y = basso; y >= alto + 2; y--) {
                corridoio[y][sinistra] = true; ultimaX = sinistra; ultimaY = y;
            }
            if (alto + 2 > basso - 2) {
                break;
            }
            corridoio[alto + 2][sinistra + 1] = true; ultimaX = sinistra + 1; ultimaY = alto + 2;
            sinistra += 2;
            alto += 2;
            destra -= 2;
            basso -= 2;
        }
        List<ICella2D> muri = new ArrayList<>();
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (!corridoio[y][x]) {
                    muri.add(new Cella2D(0, x, y));
                }
            }
        }
        IGriglia<?> g = GrigliaMatrix.vuota(n, n).addObstacle(() -> muri);

        confrontaConDijkstraNeiDueVersi(g, new Cella2D(0, 0, 0), new Cella2D(0, ultimaX, ultimaY), "spirale 13x13");
    }

    @Test
    void camminoMinCoincideConDijkstraSuCinquantaIstanzeCasualiDieciPerDieci() {
        Random scenari = new Random(SEED_SCENARI + 1);
        for (int istanza = 0; istanza < 50; istanza++) {
            long seedGriglia = scenari.nextLong();
            IGriglia<?> g = GeneratoreGriglia.genera(10, 10, 20, seedGriglia);
            List<ICella2D> libere = celleLibere(g);
            ICella2D origine = libere.get(scenari.nextInt(libere.size()));
            ICella2D destinazione = libere.get(scenari.nextInt(libere.size()));
            if (origine.x() == destinazione.x() && origine.y() == destinazione.y()) {
                continue;
            }
            confrontaConDijkstraNeiDueVersi(g, origine, destinazione,
                    "10x10 istanza=" + istanza + ", seed=" + seedGriglia);
        }
    }

    /**
     * Confronta CamminoMin con Dijkstra in entrambi i versi, per tutte le varianti,
     * e verifica i landmark.
     */
    private static void confrontaConDijkstraNeiDueVersi(IGriglia<?> g, ICella2D origine,
                                                        ICella2D destinazione, String contesto) {
        double attesa = dijkstra(g, origine, destinazione);
        for (VarianteCamminoMin variante : VarianteCamminoMin.values()) {
            for (boolean diretto : new boolean[] {true, false}) {
                ICella2D da = diretto ? origine : destinazione;
                ICella2D a = diretto ? destinazione : origine;
                String verso = contesto + (diretto ? " (O->D)" : " (D->O)") + " variante=" + variante;
                Cammino cammino = CamminoMin.calcola(g, da, a, null, variante);
                if (Double.isInfinite(attesa)) {
                    assertEquals(Double.POSITIVE_INFINITY, cammino.distanza().distanza(),
                            "Dijkstra trova INF ma CamminoMin no: " + verso);
                    assertTrue(cammino.landmarks().isEmpty(),
                            "Un cammino infinito deve avere landmark vuoti: " + verso);
                } else {
                    assertEquals(attesa, cammino.distanza().distanza(), DistanzaLibera.EPSILON,
                            "Distanza diversa da Dijkstra: " + verso);
                    verificaLandmark(g, da, a, cammino, verso);
                }
            }
        }
    }

    private static List<ICella2D> celleLibere(IGriglia<?> g) {
        List<ICella2D> libere = new ArrayList<>();
        for (int y = g.ymin(); y <= g.ymax(); y++) {
            for (int x = g.xmin(); x <= g.xmax(); x++) {
                if (StatoCella.OSTACOLO.isNot(g.getCellaAt(x, y).stato())) {
                    libere.add(new Cella2D(0, x, y));
                }
            }
        }
        return libere;
    }

    private static void verificaLandmark(IGriglia<?> g, ICella2D origine, ICella2D destinazione,
                                          Cammino cammino, String contesto) {
        List<Landmark> landmarks = cammino.landmarks();
        assertTrue(landmarks.size() >= 2, "Cammino finito senza estremi: " + contesto);
        Landmark primo = landmarks.get(0);
        Landmark ultimo = landmarks.get(landmarks.size() - 1);
        assertAll(
                () -> assertEquals(0, primo.stato(), "Tipo origine: " + contesto),
                () -> assertEquals(origine.x(), primo.x(), "x origine: " + contesto),
                () -> assertEquals(origine.y(), primo.y(), "y origine: " + contesto),
                () -> assertEquals(destinazione.x(), ultimo.x(), "x destinazione: " + contesto),
                () -> assertEquals(destinazione.y(), ultimo.y(), "y destinazione: " + contesto)
        );

        double somma = 0.0;
        for (int i = 1; i < landmarks.size(); i++) {
            Landmark precedente = landmarks.get(i - 1);
            Landmark corrente = landmarks.get(i);
            assertTrue(corrente.stato() == 1 || corrente.stato() == 2,
                    "Tipo landmark non logico: " + contesto + ", landmark=" + corrente);
            assertTrue(StatoCella.OSTACOLO.isNot(g.getCellaAt(corrente.x(), corrente.y()).stato()),
                    "Landmark su ostacolo: " + contesto + ", landmark=" + corrente);

            double dlib = DistanzaLibera.from(precedente, corrente).distanza();
            assertEquals(dlib, dijkstra(g, precedente, corrente), DistanzaLibera.EPSILON,
                    "Segmento landmark non libero alla distanza dichiarata: " + contesto);
            somma += dlib;
        }
        assertEquals(cammino.distanza().distanza(), somma, DistanzaLibera.EPSILON,
                "La somma dei segmenti landmark non coincide: " + contesto);
    }

    private static double dijkstra(IGriglia<?> g, ICella2D origine, ICella2D destinazione) {
        int w = g.width();
        int h = g.height();
        double[][] dist = new double[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                dist[y][x] = Double.POSITIVE_INFINITY;
            }
        }

        PriorityQueue<Nodo> pq = new PriorityQueue<>(Comparator.comparingDouble(Nodo::distanza));
        dist[origine.y() - g.ymin()][origine.x() - g.xmin()] = 0.0;
        pq.add(new Nodo(origine.x(), origine.y(), 0.0));

        while (!pq.isEmpty()) {
            Nodo cur = pq.poll();
            int cx = cur.x();
            int cy = cur.y();
            if (cur.distanza() > dist[cy - g.ymin()][cx - g.xmin()] + DistanzaLibera.EPSILON) {
                continue;
            }
            if (cx == destinazione.x() && cy == destinazione.y()) {
                return cur.distanza();
            }

            for (int[] dir : DIRS8) {
                int nx = cx + dir[0];
                int ny = cy + dir[1];
                if (nx < g.xmin() || nx > g.xmax() || ny < g.ymin() || ny > g.ymax()) {
                    continue;
                }
                if (StatoCella.OSTACOLO.is(g.getCellaAt(nx, ny).stato())) {
                    continue;
                }
                double step = dir[0] != 0 && dir[1] != 0 ? Math.sqrt(2.0) : 1.0;
                double nd = cur.distanza() + step;
                int rx = nx - g.xmin();
                int ry = ny - g.ymin();
                if (nd + DistanzaLibera.EPSILON < dist[ry][rx]) {
                    dist[ry][rx] = nd;
                    pq.add(new Nodo(nx, ny, nd));
                }
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    private record Nodo(int x, int y, double distanza) {}
}
