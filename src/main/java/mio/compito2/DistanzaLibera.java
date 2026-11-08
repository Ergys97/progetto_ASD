package mio.compito2;

import mio.griglia.IHave2DCoordinate;

import java.util.Objects;

public record DistanzaLibera(int distanzaTorre, int distanzaAlfiere, double distanza)
        implements Comparable<DistanzaLibera> {

    public static final double EPSILON = 1e-10;
    private static final double SQRT2 = Math.sqrt(2.0);
    public static final DistanzaLibera ZERO     = new DistanzaLibera(0, 0, 0.0);
    public static final DistanzaLibera INFINITY = new DistanzaLibera(
            Integer.MAX_VALUE, Integer.MAX_VALUE, Double.POSITIVE_INFINITY);

    public static DistanzaLibera from(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int alfiere = Math.min(dx, dy);
        int torre   = Math.max(dx, dy) - alfiere;
        return new DistanzaLibera(torre, alfiere, torre + alfiere * SQRT2);
    }

    public static DistanzaLibera from(IHave2DCoordinate a, IHave2DCoordinate b) {
        return from(a.x(), a.y(), b.x(), b.y());
    }

    public DistanzaLibera add(DistanzaLibera other) {
        Objects.requireNonNull(other, "La distanza da sommare non puo essere null");
        if (Double.isInfinite(distanza) || Double.isInfinite(other.distanza)) {
            return INFINITY;
        }
        return new DistanzaLibera(
                distanzaTorre   + other.distanzaTorre,
                distanzaAlfiere + other.distanzaAlfiere,
                distanza        + other.distanza
        );
    }

    @Override
    public int compareTo(DistanzaLibera other) {
        if (Double.isInfinite(this.distanza) || Double.isInfinite(other.distanza)) {
            return Double.compare(this.distanza, other.distanza);
        }
        double diff = this.distanza - other.distanza;
        if (Math.abs(diff) <= EPSILON) {
            return 0;
        }
        return diff < 0 ? -1 : 1;
    }
}
