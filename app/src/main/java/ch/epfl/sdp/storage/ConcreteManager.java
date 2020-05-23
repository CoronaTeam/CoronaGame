package ch.epfl.sdp.storage;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private static final String SEPARATOR = ",";
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

    public ConcreteManager(Context context, String filename, Function<String, A> convertToA, Function<String, B> convertToB) {
        if (convertToA == null || convertToB == null) {
            throw new IllegalArgumentException();
        }

        stringToA = convertToA;
        stringToB = convertToB;
        this.context = context;
        this.filename = filename;

        createFile();

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
    private void createFile(){
        file = new File(context.getFilesDir(), filename);
        if (file.isDirectory()) {
            //TODO: [LOG]
            System.out.println("TEST : it is directory");
            throw new IllegalArgumentException("Need a file and not a directory");
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                //TODO: [LOG]
                System.out.println("TEST : could not create file");
                throw new IllegalArgumentException("Unable to create a new file, must select existing one");
            }
        }
        if (!file.canWrite() || !file.canRead()) {
            //TODO: [LOG]
            System.out.println("TEST : cannot read or write the specified file");
            throw new IllegalArgumentException("Cannot read or write on the specified file");
        }
        isDeleted = false;
        cache = new TreeMap<>();
        setAtomicBooleans();
    }

    private void checkCacheStatus() {
        if (!isReadable()) {
            //TODO: [LOG]
            System.out.println("TEST : checkStatus FAILED");
            throw new IllegalStateException("Could not perform initial cache loading");
        }
    }

    @Override
    public boolean write(SortedMap<A, B> payload) {
        if (isDeleted) {
            createFile();
        }

        try {
            if (writer == null) {
                //TODO: [LOG]
                System.out.println("TEST : writer is null, check cache status " + this + " with name "+filename);
                checkCacheStatus();
                if(file==null){
                    //TODO: [LOG]
                    System.out.println("TEST : FILE IS NULL :( c'est nul");
                }
                writer = new FileWriter(file, true);
            }
            //TODO: [LOG]
            System.out.println("TEST : writer is not null with name "+filename);
            for (Map.Entry<A, B> e : payload.entrySet()) {
                // Add to cache
                cache.put(e.getKey(), e.getValue());

                writer.write(e.getKey().toString() + "," + e.getValue().toString() + LINE_END);
            }
            writer.flush();
            return true;
        } catch (IOException e) {
            //TODO: [LOG]
            System.out.println("TEST : IO EXCEPTION IN WRITE EXTERNAL");
            try {
                close();
            } catch (IOException ex) {
                //TODO: [LOG]
                System.out.println("TEST : IO EXCEPTION IN WRITE INTERNAL");
                writer = null;
            }
            return false;
        } catch (NullPointerException e){
            //TODO: [LOG]
            System.out.println("TEST :IN THE NULLPOINTER OF WRITER" +e.getStackTrace());
            return false;
        }
    }

    private boolean loadCache() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] lineContent = line.split(SEPARATOR);
                if (lineContent.length != 2) {
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
