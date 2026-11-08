package mio.compito2;

/**
 * Bit dello stato di una cella. I bit 5 e 6 non sono usati.
 */
public enum StatoCella {
    CONTESTO    (0b00000001, 0b00000001),
    REGINA      (0b00000010, 0b00000010),
    ORIGINE     (0b00000100, 0b00000100),
    COMPLEMENTO (0b00001000, 0b00001000),
    FRONTIERA   (0b00010000, 0b00010000),
    OSTACOLO    (0b10000000, 0b10000000),
    CHIUSURA    (0b00000000, 0b00001001); // CONTESTO | COMPLEMENTO

    public final int value;
    public final int mask;

    StatoCella(int value, int mask) {
        this.value = value;
        this.mask  = mask;
    }

    /** true se il bitmask n ha almeno un bit di mask settato */
    public boolean is(int n)    { return (n & mask) != 0; }
    public boolean isNot(int n) { return (n & mask) == 0; }

    /** aggiunge il value al bitmask n (OR) */
    public int addTo(int n)    { return n | value; }
    /** rimuove la mask dal bitmask n (AND NOT) */
    public int removeTo(int n) { return n & ~mask; }
}
