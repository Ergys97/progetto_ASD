package mio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainCliTest {

    @TempDir
    Path tempDir;

    @Test
    void parametriUtenteGeneranoGrigliaECsvConMetricheSpaziali() throws Exception {
        Path csv = tempDir.resolve("custom.csv");
        Path json = tempDir.resolve("custom.json");

        int exitCode = Main.esegui(new String[]{
                "--width", "6", "--height", "5", "--obstacles", "4", "--seed", "7",
                "--origin", "0,0", "--destination", "5,4", "--summary", "false",
                "--output", csv.toString(), "--grid-output", json.toString()
        });

        assertEquals(0, exitCode);
        assertTrue(Files.exists(json));
        assertTrue(Files.exists(csv));
        var righe = Files.readAllLines(csv);
        assertEquals(2, righe.size());
        assertTrue(righe.get(0).contains("seed"));
        assertTrue(righe.get(0).contains("profonditaMassima"));
        assertTrue(righe.get(0).contains("chiusureAttivePicco"));
        assertTrue(righe.get(0).contains("memoriaMatriciPiccoBytesStimata"));
        assertTrue(righe.get(0).contains("sequenzaLandmarkOD"));
        assertTrue(righe.get(0).contains("sequenzaLandmarkDO"));
        assertTrue(righe.get(0).contains("warmupEseguiti"));
        assertTrue(righe.get(0).contains("ripetizioniTempo"));
        assertTrue(righe.get(0).contains("tempoMinNs"));
        assertTrue(righe.get(0).contains("tempoMedianoNs"));
        assertTrue(righe.get(0).contains("tempoMaxNs"));
        assertTrue(righe.get(1).contains("tipo="), "Il CSV custom deve esporre i landmark");
        assertTrue(righe.get(1).startsWith("CUSTOM,base,Casuale,6,5,4,7,"));
    }

    @Test
    void eseguiRestituisceDuePerArgomentiSconosciutiOInvalidiSenzaTerminareLaJvm() {
        assertEquals(2, Main.esegui(new String[]{"--unknown", "1"}));
        assertEquals(2, Main.esegui(new String[]{"--width", "0"}));
    }

    @Test
    void scenarioEdgeProduceCoordinateEffettiveDistinteECamminoInfinito() throws Exception {
        Path csv = tempDir.resolve("edge.csv");
        Path json = tempDir.resolve("edge.json");

        int exitCode = Main.esegui(new String[]{
                "--width", "5", "--height", "5", "--obstacles", "23", "--seed", "1",
                "--origin", "0,0", "--destination", "4,4", "--summary", "false",
                "--output", csv.toString(), "--grid-output", json.toString()
        });

        assertEquals(0, exitCode);
        var righe = Files.readAllLines(csv);
        assertEquals(2, righe.size());
        Map<String, String> valori = csvSemplice(righe.get(0), righe.get(1));
        assertEquals("2", valori.get("Ox"));
        assertEquals("0", valori.get("Oy"));
        assertEquals("4", valori.get("Dx"));
        assertEquals("2", valori.get("Dy"));
        assertEquals("INF", valori.get("lunghezzaOD"));
        assertEquals("INF", valori.get("lunghezzaDO"));
        assertEquals("[]", valori.get("sequenzaLandmarkOD"));
        assertEquals("[]", valori.get("sequenzaLandmarkDO"));
        assertEquals("true", valori.get("correttezza"));
    }

    @Test
    void puliziaSnapshotRimuoveSoloFileRegolariScenarioJsonDiretti() throws Exception {
        Path directory = tempDir.resolve("griglie_json");
        Files.createDirectories(directory);
        Path obsoleto1 = Files.writeString(directory.resolve("scenario_01.json"), "[]");
        Path obsoleto2 = Files.writeString(directory.resolve("scenario_vecchio.json"), "[]");
        Path personalizzata = Files.writeString(directory.resolve("personalizzata.json"), "[]");
        Path altroFormato = Files.writeString(directory.resolve("scenario_note.txt"), "note");
        Path directoryConNomeCompatibile = Files.createDirectory(directory.resolve("scenario_directory.json"));
        Path sottodirectory = Files.createDirectory(directory.resolve("nested"));
        Path annidato = Files.writeString(sottodirectory.resolve("scenario_annidato.json"), "[]");

        Main.pulisciSnapshotScenario(directory.resolve("."));

        assertFalse(Files.exists(obsoleto1));
        assertFalse(Files.exists(obsoleto2));
        assertTrue(Files.exists(personalizzata));
        assertTrue(Files.exists(altroFormato));
        assertTrue(Files.isDirectory(directoryConNomeCompatibile));
        assertTrue(Files.exists(annidato));
    }

    @Test
    void grigliaDaFileEVarianteSonoUsateDalCompitoTre() throws Exception {
        Path json = tempDir.resolve("input.json");
        Files.writeString(json, "[[0,0,0,0],[0,128,128,0],[0,0,0,0]]");
        Path csv = tempDir.resolve("input.csv");

        int exitCode = Main.esegui(new String[]{
                "--grid-input", json.toString(), "--origin", "0,0", "--destination", "3,2",
                "--variante", "limite-propagato", "--summary", "false", "--output", csv.toString()
        });

        assertEquals(0, exitCode);
        var righe = Files.readAllLines(csv);
        assertEquals(2, righe.size());
        assertTrue(righe.get(1).startsWith("CUSTOM,limite-propagato,Casuale,4,3,2,-1,0,0,3,2,"),
                righe.get(1));
    }

    @Test
    void chiusuraStampaMappaDistanzaLiberaESalvaGliStati() throws Exception {
        Path json = tempDir.resolve("chiusura_input.json");
        Files.writeString(json, "[[0,0,0,0,0],[0,128,0,0,0],[0,0,0,0,0]]");
        Path stati = tempDir.resolve("stati.json");

        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originale = System.out;
        int exitCode;
        try {
            System.setOut(new java.io.PrintStream(buffer, true, java.nio.charset.StandardCharsets.UTF_8));
            exitCode = Main.esegui(new String[]{
                    "--grid-input", json.toString(), "--origin", "0,0", "--destination", "2,2",
                    "--chiusura", "--chiusura-output", stati.toString()
            });
        } finally {
            System.setOut(originale);
        }
        String output = buffer.toString(java.nio.charset.StandardCharsets.UTF_8);

        assertEquals(0, exitCode);
        assertTrue(output.contains("Legenda"), output);
        assertTrue(output.contains("dlib(O,D) = 0 + 2*sqrt(2) = 2.828427"), output);
        assertTrue(output.contains(
                "Contesto: 7 celle | complemento: 6 celle | frontiera: 4 celle (0 di tipo 1, 4 di tipo 2)"),
                output);
        assertTrue(output.contains("fuori dalla chiusura di O"), output);
        String[] mappa = output.split("\\R");
        assertEquals("Occcc", mappa[0]);
        assertEquals("c#KKk", mappa[1]);
        assertEquals("cKDKk", mappa[2]);

        var g = mio.compito1.LettoreGriglia.carica(stati.toString());
        assertTrue(mio.compito2.StatoCella.ORIGINE.is(g.getCellaAt(0, 0).stato()));
        assertTrue(mio.compito2.StatoCella.OSTACOLO.is(g.getCellaAt(1, 1).stato()));
        assertTrue(mio.compito2.StatoCella.COMPLEMENTO.is(g.getCellaAt(4, 2).stato()));
    }

    @Test
    void opzioniIncompatibiliOVarianteSconosciutaRestituisconoDue() throws Exception {
        Path json = tempDir.resolve("x.json");
        Files.writeString(json, "[[0,0],[0,0]]");
        assertEquals(2, Main.esegui(new String[]{"--grid-input", json.toString(), "--width", "5"}));
        assertEquals(2, Main.esegui(new String[]{
                "--grid-input", json.toString(), "--grid-output", tempDir.resolve("y.json").toString()}));
        assertEquals(2, Main.esegui(new String[]{"--variante", "veloce"}));
        assertEquals(2, Main.esegui(new String[]{"--grid-input", json.toString(), "--origin", "5,5"}));
    }

    private static Map<String, String> csvSemplice(String intestazione, String riga) {
        String[] colonne = intestazione.split(",", -1);
        String[] valori = riga.split(",", -1);
        assertEquals(colonne.length, valori.length,
                "Il caso INF usa sequenze vuote e deve essere un CSV senza campi quotati complessi");
        Map<String, String> risultato = new HashMap<>();
        for (int i = 0; i < colonne.length; i++) {
            risultato.put(colonne[i], valori[i]);
        }
        return risultato;
    }
}
