package ch.epfl.sdp.storage;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implements a StorageManager with cache (asynchronously preloaded)
 *
 * @param <A> The type of the keys
 * @param <B> The type of the values
 */
public class ConcreteManager<A extends Comparable<A>, B> implements StorageManager<A, B> {
    //The separator should be something that is not in the string representation of the data stored
    private String separator;
    private static final String LINE_END = "\n";
    private boolean isDeleted = false;
    private File file;
    private FileWriter writer = null;
    private volatile SortedMap<A, B> cache;
    private Function<String, A> stringToA;
    private Function<String, B> stringToB;
    private AtomicBoolean loadingCache;
    private AtomicBoolean cacheOk;
    private Context context;
    private String filename;

    public ConcreteManager(Context context, String filename, Function<String, A> convertToA, Function<String, B> convertToB,String separator) {
        if (convertToA == null || convertToB == null) {
            throw new IllegalArgumentException();
        }
        this.separator = separator;
        stringToA = convertToA;
        stringToB = convertToB;
        this.context = context;
        this.filename = filename;

        createFileIfAbsent();

    }
    public ConcreteManager(Context context, String filename, Function<String, A> convertToA, Function<String, B> convertToB){
        this(context,filename,convertToA,convertToB,",");
    }
    private void setAtomicBooleans(){
        loadingCache = new AtomicBoolean(false);
        cacheOk = new AtomicBoolean(false);
        AsyncTask.execute(() -> {
            boolean result = loadCache();
            cacheOk.set(result);
            loadingCache.set(true);
        });
    }
    private void createFileIfAbsent(){
        file = new File(context.getFilesDir(), filename);
        if (file.isDirectory()) {
            throw new IllegalArgumentException("Need a file and not a directory");
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to create a new file, must select existing one");
            }
        }
        if (!file.canWrite() || !file.canRead()) {
            throw new IllegalArgumentException("Cannot read or write on the specified file");
        }
        isDeleted = false;
        cache = new TreeMap<>();
        setAtomicBooleans();
    }

    private void checkCacheStatus() {
        if (!isReadable()) {
            throw new IllegalStateException("Could not perform initial cache loading");
        }
    }

    @Override
    public boolean write(SortedMap<A, B> payload) {
        if (isDeleted) {
            createFileIfAbsent();
        }

        try {
            if (writer == null) {
                checkCacheStatus();
                writer = new FileWriter(file, true);
            }

            for (Map.Entry<A, B> e : payload.entrySet()) {
                // Add to cache
                cache.put(e.getKey(), e.getValue());

                writer.write(e.getKey().toString() + "," + e.getValue().toString() + LINE_END);
            }
            writer.flush();
            return true;
        } catch (IOException e) {
            writer = null;
            return false;
        }
    }

    private boolean loadCache() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] lineContent = line.split(separator);
                if (lineContent.length != 2) {
                    //TODO : [LOG]
                    System.out.println("LINE CONTENTT NOT  IN MANAGER : "+ Arrays.toString(lineContent)+lineContent.length);
                    for( String s : lineContent){
                        System.out.println("LINE CONTENTT NOT  IN MANAGER : "+s);
                    }

                    return false;
                }
                A key = stringToA.apply(lineContent[0]);
                B value = stringToB.apply(lineContent[1]);
                cache.put(key, value);
            }
            return true;
        } catch (IOException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isReadable() {
        while (!loadingCache.get()) {
        }
        return cacheOk.get();
    }

    @Override
    public SortedMap<A, B> read() {
        if (isDeleted) {
            throw new IllegalStateException("Cannot read file after deletion");
        }

        checkCacheStatus();

        if (cache.isEmpty()) {
            boolean cacheSuccess = loadCache();
            if (!cacheSuccess) {
                return new TreeMap<>();
            }
        }
        return Collections.unmodifiableSortedMap(new TreeMap<>(cache));
    }

    @Override
    public SortedMap<A, B> filter(BiFunction<A, B, Boolean> rule) {
        if (isDeleted) {
            throw new IllegalStateException("Cannot filter on file after deletion");
        }

        checkCacheStatus();

        if (cache.isEmpty()) {
            boolean cacheSuccess = loadCache();
            if (!cacheSuccess) {
                return new TreeMap<>();
            }
        }

        SortedMap<A, B> result = new TreeMap<>();
        cache.forEach((k, v) -> {
            if (rule.apply(k, v)) {
                result.put(k, v);
            }
        });

        return Collections.unmodifiableSortedMap(result);
    }

    @Override
    public void delete() {
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (file.exists()) {
                file.delete();
            }
            isDeleted = true;
        }
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }
}
