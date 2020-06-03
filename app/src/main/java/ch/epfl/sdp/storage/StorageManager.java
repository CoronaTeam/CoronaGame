package ch.epfl.sdp.storage;

import java.io.IOException;
import java.util.SortedMap;
import java.util.function.BiFunction;

/**
 * Abstraction for component that stores and retrieve key-value pairs
 * from a file located in internal storage
 */
public interface StorageManager<A extends Comparable<A>, B> {
    /**
     * Write something on a local file
     * @param payload
     * @return
     */
    boolean write(SortedMap<A, B> payload);

    /**
     * Read the data on the local file
     * @return
     */
    SortedMap<A, B> read();

    /**
     * Read the data on the local file, filtered by the given predicate
     * @param rule
     * @return
     */
    SortedMap<A, B> filter(BiFunction<A, B, Boolean> rule);

    /**
     * Check if the data on the disk can be read
     * @return
     */
    boolean isReadable();

    void close() throws IOException;

    /**
     * Deletes the data stored on the disk
     */
    void delete();

    /**
     * If a problem occurs (ex. the file is not readable),
     * tries to reset the fileReader. If it fails to, it will do a delete.
     */
    void reset();
}
