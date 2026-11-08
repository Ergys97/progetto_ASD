package mio.compito3;

import mio.compito2.DistanzaLibera;
import java.util.List;
import java.util.Objects;

public record Cammino(DistanzaLibera distanza, List<Landmark> landmarks)
        implements Comparable<Cammino> {

    public Cammino {
        Objects.requireNonNull(distanza, "La distanza non puo essere null");
        landmarks = List.copyOf(Objects.requireNonNull(landmarks, "I landmark non possono essere null"));
    }

    public static final Cammino INFINITY =
            new Cammino(DistanzaLibera.INFINITY, List.of());

    @Override
    public int compareTo(Cammino other) {
        return distanza.compareTo(other.distanza);
    }
}
