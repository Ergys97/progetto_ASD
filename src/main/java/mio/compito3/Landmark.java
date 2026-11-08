package mio.compito3;

import mio.griglia.ICella2D;

public record Landmark(int stato, int x, int y) implements ICella2D {

    public Landmark {
        if (stato < 0 || stato > 2) {
            throw new IllegalArgumentException("Il tipo landmark deve essere 0, 1 o 2");
        }
    }

    @Override
    public String toString() {
        return "(x=" + x + ",y=" + y + ",tipo=" + stato + ")";
    }
}
