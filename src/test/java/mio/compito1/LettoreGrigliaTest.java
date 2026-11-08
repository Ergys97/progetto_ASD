package mio.compito1;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LettoreGrigliaTest {

    @Test
    void salvaECaricaMantengonoLaStessaGriglia() throws Exception {
        var originale = GeneratoreGriglia.genera(12, 8, 15, 42L);
        Path file = Files.createTempFile("griglia-", ".json");

        try {
            LettoreGriglia.salva(originale, file.toString());
            var caricata = LettoreGriglia.carica(file.toString());

            assertEquals(originale.width(), caricata.width());
            assertEquals(originale.height(), caricata.height());

            for (int y = originale.ymin(); y <= originale.ymax(); y++) {
                for (int x = originale.xmin(); x <= originale.xmax(); x++) {
                    assertEquals(
                            originale.getCellaAt(x, y).stato(),
                            caricata.getCellaAt(x, y).stato(),
                            "Mismatch su cella (" + x + "," + y + ")"
                    );
                }
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }
}