package ch.epfl.sdp.storage;

import androidx.test.rule.ActivityTestRule;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import ch.epfl.sdp.CoronaGame;
import ch.epfl.sdp.IntroActivity;
import ch.epfl.sdp.TestTools;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ProbabilityStorageTest {

    @Rule
    public final ActivityTestRule<IntroActivity> mActivityRule = new ActivityTestRule<>(IntroActivity.class);

    @Rule
    public ExpectedException testException = ExpectedException.none();

    private static final String TEST_FILENAME = "history_test_file.csv";

    private StorageManager<Integer, Double> getIntDoubleManager() {
        return new ConcreteManager<>(
                CoronaGame.getContext(),
                TEST_FILENAME,
                Integer::valueOf,
                Double::valueOf);
    }

    private DateFormat format = new SimpleDateFormat("E MMM dd HH:mm:ss zzz yyyy");

    private StorageManager<Date, Double> getDateDoubleManager() {
        return new ConcreteManager<>(
                CoronaGame.getContext(),
                TEST_FILENAME,
                k -> {
                    try {
                        return format.parse(k);
                    } catch (ParseException e) {
                        throw new IllegalArgumentException();
                    }
                },
                Double::valueOf);
    }

    @BeforeClass
    public static void DeleteTestFile() {
        new ConcreteManager<>(
                CoronaGame.getContext(),
                TEST_FILENAME,
                k -> 0,
                v -> null
        ).delete();
    }

    @Test
    public void fileCanBeSuccessfullyDeleted() {
        StorageManager<Integer, Double> manager = getIntDoubleManager();
        manager.write(Collections.singletonMap(1, 60.3));
        manager.delete();
    }

    @Test
    public void fileCannotBeDeletedThenRead() {
        testException.expect(IllegalStateException.class);

        StorageManager<Integer, Double> manager = getIntDoubleManager();
        manager.delete();

        Map<Integer, Double> res = manager.read();
    }

    @Test
    public void fileCannotBeDeletedThenWritten() {
        testException.expect(IllegalStateException.class);

        StorageManager<Integer, Double> manager = getIntDoubleManager();
        manager.delete();

        manager.write(Collections.emptyMap());
    }

    @Test
    public void fileCannotBeDeletedThenFiltered() {
        testException.expect(IllegalStateException.class);

        StorageManager<Integer, Double> manager = getIntDoubleManager();
        manager.delete();

        Map<Integer, Double> res = manager.filter((a, b) -> false);
    }

    @Test
    public void contentCanBeWrittenAndRetrieved() {
        StorageManager<Integer, Double> manager = new ConcreteManager<>(
                CoronaGame.getContext(),
                "history_test_file.csv",
                Integer::valueOf,
                Double::valueOf);

        boolean insertionSuccess = manager.write(Collections.singletonMap(4, 53.4));

        assertThat(insertionSuccess, equalTo(true));

        Map<Integer, Double> content = manager.read();

        assertThat(content.size(), equalTo(1));
        assertThat(content.getOrDefault(4, 0.d), equalTo(53.4));

        manager.delete();
    }

    @Test
    public void cacheIsKeptInSync() {
        StorageManager<Integer, Double> manager = getIntDoubleManager();

        boolean insertionSuccess = manager.write(Collections.singletonMap(4, 53.4));
        assertThat(insertionSuccess, equalTo(true));

        Map<Integer, Double> content = manager.read();
        assertThat(content.size(), equalTo(1));

        insertionSuccess = manager.write(Collections.singletonMap(5, 10.));
        assertThat(insertionSuccess, equalTo(true));
        assertThat(content.size(), equalTo(2));

        manager.delete();
    }

    @Test
    public void filterExcludesNonMatchingElements() {
        StorageManager<Integer, Double> manager = getIntDoubleManager();

        boolean insertionSuccess = manager.write(Collections.singletonMap(2, .4));
        assertThat(insertionSuccess, equalTo(true));
        insertionSuccess = manager.write(Collections.singletonMap(6, .6));
        assertThat(insertionSuccess, equalTo(true));

        Map<Integer, Double> res1 = manager.filter((k, v) -> k < 2);
        assertThat(res1.size(), equalTo(0));

        Map<Integer, Double> res2 = manager.filter((k, v) -> k < 4);
        assertThat(res2.size(), equalTo(1));
        assertThat(res2.containsKey(2), equalTo(true));

        manager.delete();
    }

    @Test
    public void readReturnsUnmodifiableMap() {
        StorageManager<Integer, Double> manager = getIntDoubleManager();

        boolean insertionSuccess = manager.write(Collections.singletonMap(2, .4));
        assertThat(insertionSuccess, equalTo(true));

        Map<Integer, Double> res1 = manager.read();
        assertThat(res1.size(), equalTo(1));

        manager.delete();

        testException.expect(UnsupportedOperationException.class);
        res1.put(10, -1.);
    }

    @Test
    public void dataAreStoredPersistently() throws IOException {
        StorageManager<Date, Double> createFile = getDateDoubleManager();

        Date before = new Date();

        boolean insertionSuccess = createFile.write(Collections.singletonMap(before, .4));
        assertThat(insertionSuccess, equalTo(true));

        createFile.close();

        StorageManager<Date, Double> addSomething = getDateDoubleManager();

        TestTools.sleep();
        Date middle = new Date();
        TestTools.sleep();

        Date now = new Date();

        insertionSuccess = addSomething.write(Collections.singletonMap(now, 1.4));
        assertThat(insertionSuccess, equalTo(true));

        assertThat(addSomething.read().size(), equalTo(2));

        addSomething.close();

        StorageManager<Date, Double> readEverything = getDateDoubleManager();

        Map<Date, Double> happenedBefore = readEverything.filter((dt, vl) -> dt.before(middle));

        assertThat(happenedBefore.size(), equalTo(1));
        assertThat(happenedBefore.values().iterator().next(), equalTo(.4));

        Map<Date, Double> happenedSometime = readEverything.read();

        assertThat(happenedSometime.size(), equalTo(2));

        readEverything.delete();

        StorageManager<Date, Double> freshFile = getDateDoubleManager();

        assertThat(freshFile.read().size(), equalTo(0));

        freshFile.delete();
    }
}
