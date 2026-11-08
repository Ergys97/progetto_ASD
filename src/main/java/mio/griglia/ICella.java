package mio.griglia;

public interface ICella {
    int stato();
    default boolean is(int mask)    { return (stato() & mask) > 0; }
    default boolean isNot(int mask) { return (stato() & mask) == 0; }
}
