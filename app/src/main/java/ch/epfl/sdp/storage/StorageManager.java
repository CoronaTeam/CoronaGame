package ch.epfl.sdp.storage;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Abstraction for component that stores and retrieve key-value pairs
 * from a file located in internal storage
 */
public interface StorageManager<A extends Comparable<A>, B> {

    boolean write(Map<A, B> payload);
    Map<A, B> read();
    Map<A, B> filter(BiFunction<A, B, Boolean> rule);
    void close() throws IOException;
    void delete();
}
