package ch.epfl.sdp.contamination;

import android.location.Location;

import androidx.test.espresso.intent.Intents;
import androidx.test.rule.ActivityTestRule;

import com.google.firebase.firestore.GeoPoint;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observer;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import ch.epfl.sdp.R;
import ch.epfl.sdp.TestTools;
import ch.epfl.sdp.contamination.databaseIO.CachingDataSender;
import ch.epfl.sdp.contamination.databaseIO.DataReceiver;
import ch.epfl.sdp.contamination.fragment.InfectionFragment;
import ch.epfl.sdp.location.LocationService;
import ch.epfl.sdp.testActivities.InfectionActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sdp.TestTools.clickBack;
import static ch.epfl.sdp.TestTools.getMapValue;
import static ch.epfl.sdp.TestTools.initSafeTest;
import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.HEALTHY;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.INFECTED;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.UNKNOWN;
import static ch.epfl.sdp.contamination.InfectionAnalyst.IMMUNITY_FACTOR;
import static ch.epfl.sdp.contamination.InfectionAnalyst.TRANSMISSION_FACTOR;
import static ch.epfl.sdp.firestore.FirestoreLabels.privateRecoveryCounter;
import static ch.epfl.sdp.firestore.FirestoreLabels.publicAlertAttribute;
import static ch.epfl.sdp.location.LocationUtils.buildLocation;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class ConcreteAnalysisTest {

    private static final HashMap<String, Float> recentSickMeetingCounter = new HashMap<>();
    private static Layman man1;
    private static Layman man2;
    private static Layman man3;
    private static Layman man4;
    private static SortedMap<Date, Location> lastPositions;
    private static Set<Carrier> peopleAround;
    private static Map<Long, Carrier> rangePeople;
    private static final Map<GeoPoint, Map<Long, Set<Carrier>>> city = new HashMap<>();
    private static int recoveryCounter;
    @Rule
    public final ActivityTestRule<InfectionActivity> mActivityRule = new ActivityTestRule<>(InfectionActivity.class);
    private final Location testLocation = buildLocation(65, 63);
    private final Date testDate = new Date(System.currentTimeMillis());
    private final DataReceiver mockReceiver = new DataReceiver() {
        @Override
        public CompletableFuture<Set<Carrier>> getUserNearby(Location location, Date date) {
            HashSet<Carrier> res = new HashSet<>();
            switch ((int) (location.getLatitude())) {
                case 20:
                    res.add(man2);
                    break;
                case 10:
                    res.add(man1);
                    break;
                case 30:
                    res.add(man3);
                    break;
                case 40:
                    res.add(man4);
                    break;
                default:
                    if (location == testLocation && date.equals(testDate)) {
                        return CompletableFuture.completedFuture(peopleAround);
                    }
            }
            return CompletableFuture.completedFuture(res);
        }

        @Override
        public CompletableFuture<Map<Carrier, Integer>> getUserNearbyDuring(Location location, Date startDate, Date endDate) {
            if (location == null) {
                return CompletableFuture.completedFuture(Collections.emptyMap());
            } else {
                Map<Carrier, Integer> met = new HashMap<>();
                for (long t : rangePeople.keySet()) {
                    if (startDate.getTime() <= t && t <= endDate.getTime()) {
                        met.put(rangePeople.get(t), 1);
                    }
                }
                return CompletableFuture.completedFuture(met);
            }
        }

        @Override
        public CompletableFuture<Location> getMyLastLocation(String accountId) {
            return null;
        }

        @Override
        public CompletableFuture<Map<String, Object>> getRecoveryCounter(String userId) {
            return CompletableFuture.completedFuture(getRecoveryCount());
        }

        @Override
        public CompletableFuture<Map<String, Object>> getNumberOfSickNeighbors(String userId) {
            if (recentSickMeetingCounter.containsKey(userId)) {
                HashMap<String, Object> res = new HashMap<>();
                recentSickMeetingCounter.get(userId);
                res.put(publicAlertAttribute, recentSickMeetingCounter.get(userId));
                return CompletableFuture.completedFuture(res);
            } else {
                return CompletableFuture.completedFuture(Collections.emptyMap());
            }
        }
    };
    private final CachingDataSender sender = new FakeCachingDataSender() {
        @Override
        public CompletableFuture<Void> registerLocation(Carrier carrier, Location location, Date time) {
            fakeFirebaseStore.put(time, location);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> sendAlert(String userId, float previousIllnessProbability) {
            recentSickMeetingCounter.computeIfPresent(userId,
                    (k, v) -> v + 1 - previousIllnessProbability);
            recentSickMeetingCounter.computeIfAbsent(userId, k -> 1 - previousIllnessProbability);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> sendAlert(String userId) {
            return sendAlert(userId, 0);
        }

        @Override
        public CompletableFuture<Void> resetSickAlerts(String userId) {
            recentSickMeetingCounter.remove(userId);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public SortedMap<Date, Location> getLastPositions() {
            return lastPositions;
        }
    };

    @BeforeClass
    public static void initiateData() {
        recoveryCounter = 0;

        lastPositions = new TreeMap<>();
        lastPositions.put(PositionAggregator.getWindowForDate(Calendar.getInstance().getTime()), newLoc(20, 20));
        lastPositions.put(PositionAggregator.getWindowForDate(new Date(System.currentTimeMillis() - PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION)), newLoc(10, 10));
        lastPositions.put(PositionAggregator.getWindowForDate(new Date(System.currentTimeMillis() - 2 * PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION)), newLoc(40, 40));
        lastPositions.put(PositionAggregator.getWindowForDate(new Date(System.currentTimeMillis() - 3 * PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION)), newLoc(30, 30));


        peopleAround = new HashSet<>();
        peopleAround.add(new Layman(HEALTHY, "Woman1"));
        peopleAround.add(new Layman(INFECTED, "Woman2"));
        peopleAround.add(new Layman(UNKNOWN, "Woman3"));

        rangePeople = new HashMap<>();
        man1 = new Layman(Carrier.InfectionStatus.INFECTED, "Man1");
        man2 = new Layman(HEALTHY, "Man2");
        man3 = new Layman(Carrier.InfectionStatus.UNKNOWN, 0.3f, "Man3");
        man4 = new Layman(HEALTHY, "Man4");

        rangePeople.put(1585223363913L, man1);
        rangePeople.put(1585223373883L, man2);
        rangePeople.put(1585223373893L, man3);
        rangePeople.put(1585223373903L, man4);
    }

    private static Map<String, Object> getRecoveryCount() {
        Map<String, Object> map = new HashMap<>();
        if (recoveryCounter != 0) {
            map.put(privateRecoveryCounter, recoveryCounter);
        } else {
            map = Collections.emptyMap();
        }
        return map;

    }

    @Before
    public void init() {
        initSafeTest(mActivityRule, false);
    }

    @After
    public void release() {
        Intents.release();
    }

    @Test
    public void canEvolveIfInfected() {

        ObservableCarrier me = new Layman(HEALTHY);

        assertThat(me.setIllnessProbability(new Date(), .5f), equalTo(true));

        InfectionAnalyst analyst = new ConcreteAnalysis(me, mockReceiver, sender);
        analyst.updateInfectionPredictions(testLocation, new Date(1585223373980L), new Date());
        assertThat(me.getInfectionStatus(), equalTo(UNKNOWN));

    }

    @Test
    public void probabilityIsUpdatedAfterContactWithInfected() {

        ObservableCarrier me = new Layman(HEALTHY);

        InfectionAnalyst analyst = new ConcreteAnalysis(me, mockReceiver, sender);
        analyst.updateInfectionPredictions(testLocation, new Date(1585220363913L), new Date()).thenRun(() -> {
            assertThat(me.getInfectionStatus(), equalTo(HEALTHY));
            assertThat(me.getIllnessProbability(), greaterThan(0.f));
        });

    }

    @Test
    public void infectionProbabilityIsUpdated() throws Throwable {
        recoveryCounter = 0;
        CityDataReceiver cityReceiver = new CityDataReceiver();
        ObservableCarrier me = new Layman(HEALTHY, "TEST_USER");

        InfectionFragment fragment = ((InfectionFragment) mActivityRule.getActivity().getSupportFragmentManager().findFragmentById(R.id.fragmentContainer));

        LocationService service = fragment.getLocationService().join();

        DataReceiver originalReceiver = service.getReceiver();
        service.setReceiver(cityReceiver);
        service.setSender(sender);
        InfectionAnalyst analysis = new ConcreteAnalysis(me, cityReceiver, sender);
        InfectionAnalyst originalAnalyst = service.getAnalyst();

        service.setAnalyst(analysis);


        cityReceiver.setMyCurrentLocation(buildLocation(0, 0));

        //fragment.onModelRefresh(null);
        mActivityRule.getActivity().runOnUiThread(() -> fragment.onModelRefresh(null));

        TestTools.sleep();
        clickBack();
        onView(withId(R.id.my_infection_status)).check(matches(withText("HEALTHY")));

        // I'm going to a healthy location
        GeoPoint healthyLocation = new GeoPoint(12, 13);
        cityReceiver.setMyCurrentLocation(buildLocation(12, 13));

        Thread.sleep(10);

        city.put(healthyLocation, new HashMap<>());
        city.get(healthyLocation).put(System.currentTimeMillis(), Collections.singleton(new Layman(UNKNOWN, .89f)));
        city.get(healthyLocation).put(System.currentTimeMillis() + 1, Collections.singleton(new Layman(UNKNOWN, .91f)));
        city.get(healthyLocation).put(System.currentTimeMillis() + 2, Collections.singleton(new Layman(UNKNOWN, .90f)));
        city.get(healthyLocation).put(System.currentTimeMillis() + 3, Collections.singleton(new Layman(UNKNOWN, .92f)));

        Thread.sleep(10);

        mActivityRule.getActivity().runOnUiThread(() -> fragment.onModelRefresh(null));
        Thread.sleep(10);
        clickBack();

        onView(withId(R.id.my_infection_status)).check(matches(withText("HEALTHY")));
        Thread.sleep(3000);
        // This location is full of ill people
        GeoPoint badLocation = new GeoPoint(40, 113.4);
        city.put(badLocation, new HashMap<>());
        long nowMillis = System.currentTimeMillis();
        for (int i = 0; i < 30; i++) {
//            city.get(badLocation).put(nowMillis+i+14, Collections.singleton(new Layman(INFECTED)));
        }

        city.get(badLocation).put(nowMillis + 13, Collections.singleton((new Layman(INFECTED))));
        city.get(badLocation).put(nowMillis + 14, Collections.singleton(man1));
        Thread.sleep(30);
        mActivityRule.getActivity().runOnUiThread(() -> fragment.onModelRefresh(null));

        clickBack();

        // I was still on healthyLocation
        onView(withId(R.id.my_infection_refresh)).perform(click());
        sleep(5000);
        clickBack();
        onView(withId(R.id.my_infection_status)).check(matches(withText("HEALTHY")));

        Thread.sleep(1500);

        nowMillis = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            city.get(badLocation).put(nowMillis + i * 1000, Collections.singleton(new Layman(UNKNOWN, .99f + i)));
        }
        city.get(badLocation).put(nowMillis + 13, Collections.singleton((new Layman(INFECTED))));
        city.get(badLocation).put(nowMillis + 14, Collections.singleton(man1));
        city.get(badLocation).put(nowMillis + 12, Collections.singleton((new Layman(INFECTED, "Joseph"))));
        city.get(badLocation).put(nowMillis + 11, Collections.singleton((new Layman(INFECTED, "Amélie Poulain"))));
        city.get(badLocation).put(nowMillis + 10, Collections.singleton((new Layman(INFECTED, "Jean-Yves le Boudecque"))));

        sleep(5000);

        // I go to the bad location
        cityReceiver.setMyCurrentLocation(buildLocation(40, 113.4));

        mActivityRule.getActivity().runOnUiThread(() -> fragment.onModelRefresh(null));
        clickBack(2500);
        // Now there should be some risk that I was infected
        onView(withId(R.id.my_infection_refresh)).perform(click());
        clickBack(2500);
        onView(withId(R.id.my_infection_status)).check(matches(withText("UNKNOWN")));

        // TODO: Restore original components
        service.setReceiver(originalReceiver);
        service.setAnalyst(originalAnalyst);

    }

    @Test
    public void locationEqualityTest() {
        GeoPoint a = new GeoPoint(12, 13.24);
        GeoPoint b = new GeoPoint(12, 13.24);
        assertThat(a.equals(b), equalTo(true));
    }

    @Test
    public void getCarrierReturnsTheSameCarrier() {
        ObservableCarrier me = new Layman(HEALTHY);
        InfectionAnalyst analyst = new ConcreteAnalysis(me, mockReceiver, sender);
        assertNotNull(analyst.getCarrier());
        assertSame(me, analyst.getCarrier());
    }

    @Test
    public void observersKnowIfStatusChanged() {
        AtomicInteger counter = new AtomicInteger(0);

        Observer fakeObserver = (o, arg) -> {
            if (((Optional<Float>) arg).isPresent()) {
                counter.incrementAndGet();
            }
        };

        ObservableCarrier me = new Layman(HEALTHY);
        me.addObserver(fakeObserver);

        // No status change
        assertTrue(me.evolveInfection(new Date(), HEALTHY, me.getIllnessProbability()));
        sleep();
        assertThat(counter.get(), equalTo(0));

        // With status change
        assertTrue(me.evolveInfection(new Date(), UNKNOWN, me.getIllnessProbability()));
        sleep();
        assertThat(counter.get(), equalTo(1));
    }

    @Test
    public void notifiesSickNeighborsWhenYouGetSick() {
        ObservableCarrier me = new Layman(HEALTHY);
        InfectionAnalyst analyst = new ConcreteAnalysis(me, mockReceiver, sender);
        me.evolveInfection(new Date(), INFECTED, 1f);
        mockReceiver.getNumberOfSickNeighbors("Man1").thenAccept(res ->
                assertTrue(res.isEmpty()));
        mockReceiver.getNumberOfSickNeighbors("Man2").thenAccept(res ->
                assertEquals(1f, getMapValue(res), 0.0001));
        mockReceiver.getNumberOfSickNeighbors("Man3").thenAccept(res ->
                assertEquals(1f, getMapValue(res), 0.0001));
        mockReceiver.getNumberOfSickNeighbors("Man4").thenAccept(res ->
                assertEquals(1f, getMapValue(res), 0.0001));
    }

    @Test
    public void adaptYourProbabilityOfInfectionAccordingToSickMeetingsAndThenResetItsCounter() {
        recoveryCounter = 0;
        ObservableCarrier me = new Layman(HEALTHY, "TESTUSER");
        InfectionAnalyst analyst = new ConcreteAnalysis(me, mockReceiver, sender);
        sender.sendAlert(me.getUniqueId());
        sender.sendAlert(me.getUniqueId(), 0.4f);
        analyst.updateInfectionPredictions(null, null, null);
        System.out.println(me.toString());
        assertEquals(TRANSMISSION_FACTOR * (1 + (1 - 0.4)), me.getIllnessProbability(), 0.00001f);
        mockReceiver.getNumberOfSickNeighbors(me.getUniqueId()).thenAccept(res -> assertTrue((res).isEmpty()));
    }

    @Test
    public void doesUpdateCorrectlySicknessState() {
        InfectionFragment fragment = ((InfectionFragment) mActivityRule.getActivity().getSupportFragmentManager().findFragmentById(R.id.fragmentContainer));
        LocationService service = fragment.getLocationService().join();
        ObservableCarrier me = new Layman(HEALTHY);
        InfectionAnalyst analyst = new ConcreteAnalysis(me, mockReceiver, sender);
        me.evolveInfection(new Date(), INFECTED, 1f);
        assertSame(INFECTED, analyst.getCarrier().getInfectionStatus());
    }

    @Test
    public void adaptInfectionProbabilityOfBadMeetingsIfRecovered() {
        recoveryCounter = 1;
        ObservableCarrier me = new Layman(HEALTHY);
        InfectionAnalyst analyst = new ConcreteAnalysis(me, mockReceiver, sender);
        sender.sendAlert(me.getUniqueId());
        sender.sendAlert(me.getUniqueId(), 0.4f);
        analyst.updateInfectionPredictions(null, null, null).thenAccept(res -> assertEquals(1.6 * Math.pow(IMMUNITY_FACTOR, recoveryCounter) * TRANSMISSION_FACTOR, me.getIllnessProbability(), 0.00001f));
    }

    @Test
    public void decreaseSickProbabilityWhenRecovered() {
        recoveryCounter = 2;
        CityDataReceiver cityReceiver = new CityDataReceiver();
        ObservableCarrier me = new Layman(HEALTHY);

        InfectionFragment fragment = ((InfectionFragment) mActivityRule.getActivity().getSupportFragmentManager().findFragmentById(R.id.fragmentContainer));
        LocationService service = fragment.getLocationService().join();
        service.setReceiver(cityReceiver);
        InfectionAnalyst analysis = new ConcreteAnalysis(me, cityReceiver, sender);
        service.setAnalyst(analysis);

        GeoPoint badLocations = new GeoPoint(42, 113.4);
        city.put(badLocations, new HashMap<>());
        long nowMillis = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            city.get(badLocations).put(nowMillis + i * 100, Collections.singleton(new Layman(UNKNOWN, .98f + i)));
        }

        sleep(5001);

        cityReceiver.setMyCurrentLocation(buildLocation(42, 113.4));
        // Now there should be some risk that I was infected

        mActivityRule.getActivity().runOnUiThread(() -> fragment.onModelRefresh(null));
        sleep(1000);
        clickBack();
        sleep(11);
        float threshold = 0.05f;
        //In case the TRANSMISSION_FACTOR changes in the future, the test still works by doing:
        if (TRANSMISSION_FACTOR >= 0.9) {
            threshold = 0.10f;
        } else if (TRANSMISSION_FACTOR >= 0.7f) {
            threshold = 0.08f;
        } else if (TRANSMISSION_FACTOR > 0.6f) {
            threshold = 0.06f;
        }
        assertTrue(me.getIllnessProbability() < threshold);
    }

    static class CityDataReceiver implements DataReceiver {
        Location myCurrentLocation;

        @Override
        public CompletableFuture<Set<Carrier>> getUserNearby(Location l, Date date) {
            GeoPoint location = new GeoPoint(l.getLatitude(), l.getLongitude());
            if (city.containsKey(location) && city.get(location).containsKey(date)) {
                return CompletableFuture.completedFuture(city.get(location).get(date));
            } else {
                return CompletableFuture.completedFuture(Collections.emptySet());
            }
        }

        private boolean timeIsInRange(long k, Date startDate, Date endDate) {
            return startDate.getTime() <= k && k <= endDate.getTime();
        }

        private void merge(Map<Carrier, Integer> r, Carrier carrier) {
            if (r.containsKey(carrier)) {
                r.put(carrier, r.get(carrier) + 1);
            } else {
                r.put(carrier, 1);
            }
        }

        private Map<Carrier, Integer> filterByTime(GeoPoint location, Date startDate, Date endDate) {
            Map<Carrier, Integer> res = new HashMap<>();
            for (long k : city.get(location).keySet()) {
                if (timeIsInRange(k, startDate, endDate)) {
                    for (Carrier carrier : city.get(location).get(k)) {
                        merge(res, carrier);
                    }
                }
            }
            return res;
        }

        @Override
        public CompletableFuture<Map<Carrier, Integer>> getUserNearbyDuring(Location l, Date startDate, Date endDate) {
            GeoPoint location = new GeoPoint(l.getLatitude(), l.getLongitude());

            if (city.containsKey(location)) {
                return CompletableFuture.completedFuture(filterByTime(location, startDate, endDate));
            } else {
                return CompletableFuture.completedFuture(Collections.emptyMap());
            }
        }

        void setMyCurrentLocation(Location here) {
            myCurrentLocation = here;
        }

        @Override
        public CompletableFuture<Location> getMyLastLocation(String accountId) {
            return CompletableFuture.completedFuture(myCurrentLocation);
        }

        @Override
        public CompletableFuture<Map<String, Object>> getNumberOfSickNeighbors(String userId) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }

        public CompletableFuture<Map<String, Object>> getRecoveryCounter(String userId) {
            return CompletableFuture.completedFuture(getRecoveryCount());
        }
    }
}
