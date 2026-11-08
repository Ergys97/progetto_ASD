package mio.compito3;

import java.util.Arrays;
import java.util.Locale;

/**
 * Varianti di CamminoMin confrontate nella sperimentazione.
 *
 * Tutte restituiscono la stessa lunghezza minima; cambiano il numero di chiamate
 * ricorsive e quindi tempo e memoria.
 * <ul>
 *   <li>{@link #BASE}: pseudocodice della traccia con ordinamento della frontiera,
 *       condizione di riga 17 e memoizzazione;</li>
 *   <li>{@link #SENZA_CACHE}: come BASE ma senza memoizzazione;</li>
 *   <li>{@link #SENZA_ORDINAMENTO}: come BASE ma con la frontiera visitata
 *       nell'ordine di scansione della griglia;</li>
 *   <li>{@link #LIMITE_PROPAGATO}: come BASE, ma ogni chiamata annidata riceve il
 *       limite {@code lunghezzaMin - dlib(O,F)} e cerca solo cammini piu corti.</li>
 * </ul>
 */
public enum VarianteCamminoMin {
    BASE("base", true, true, false),
    SENZA_CACHE("senza-cache", false, true, false),
    SENZA_ORDINAMENTO("senza-ordinamento", true, false, false),
    LIMITE_PROPAGATO("limite-propagato", true, true, true);

    private final String nome;
    private final boolean cache;
    private final boolean ordinaFrontiera;
    private final boolean limitePropagato;

    VarianteCamminoMin(String nome, boolean cache, boolean ordinaFrontiera, boolean limitePropagato) {
        this.nome = nome;
        this.cache = cache;
        this.ordinaFrontiera = ordinaFrontiera;
        this.limitePropagato = limitePropagato;
    }

    /** Nome usato nella riga di comando e nei file CSV. */
    public String nome() { return nome; }
    public boolean cache() { return cache; }
    public boolean ordinaFrontiera() { return ordinaFrontiera; }
    public boolean limitePropagato() { return limitePropagato; }

    /** Converte il nome usato da riga di comando nella variante corrispondente. */
    public static VarianteCamminoMin daNome(String nome) {
        String cercato = nome == null ? "" : nome.trim().toLowerCase(Locale.ROOT);
        for (VarianteCamminoMin v : values()) {
            if (v.nome.equals(cercato)) return v;
        }
        throw new IllegalArgumentException("Variante sconosciuta: " + nome + " (valori ammessi: "
                + String.join(", ", Arrays.stream(values()).map(VarianteCamminoMin::nome).toList()) + ")");
    }

    @Override
    public String toString() {
        return nome;
    }
}
