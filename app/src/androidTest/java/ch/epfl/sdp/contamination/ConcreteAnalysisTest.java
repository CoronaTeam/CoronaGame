package ch.epfl.sdp.contamination;

import android.location.Location;

import androidx.test.rule.ActivityTestRule;

import com.google.firebase.firestore.GeoPoint;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ch.epfl.sdp.Account;
import ch.epfl.sdp.Callback;
import ch.epfl.sdp.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sdp.TestUtils.buildLocation;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.HEALTHY;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.INFECTED;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.UNKNOWN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class ConcreteAnalysisTest {

    Location testLocation = buildLocation(65, 63);
    Date testDate = new Date(System.currentTimeMillis());

    static Set<Carrier> peopleAround;

    static Map<Long, Carrier> rangePeople;

    static Map<GeoPoint, Map<Long, Set<Carrier>>> city = new HashMap<>();

    @Rule
    public final ActivityTestRule<InfectionActivity> mActivityRule = new ActivityTestRule<>(InfectionActivity.class);

    @BeforeClass
    public static void createFewNeighbors() {
        peopleAround = new HashSet<>();
        peopleAround.add(new Layman(HEALTHY));

        rangePeople = new HashMap<>();
        rangePeople.put(1585223363913L, new Layman(Carrier.InfectionStatus.INFECTED));
        rangePeople.put(1585223373883L, new Layman(Carrier.InfectionStatus.IMMUNE));
        rangePeople.put(1585223373893L, new Layman(Carrier.InfectionStatus.UNKNOWN, 0.3f));
        rangePeople.put(1585223373903L, new Layman(HEALTHY));

    }

    DataReceiver mockReceiver = new DataReceiver() {
        @Override
        public void getUserNearby(Location location, Date date, Callback<Set<? extends Carrier>> callback) {
            if (location == testLocation && date.equals(testDate)) {
                callback.onCallback(peopleAround);
            }
        }

        @Override
        public void getUserNearbyDuring(Location location, Date startDate, Date endDate, Callback<Map<? extends Carrier, Integer>> callback) {
            Map<Carrier, Integer> met = new HashMap<>();
            for (long t : rangePeople.keySet()) {
                if (startDate.getTime() <= t && t <= endDate.getTime()) {
                    met.put(rangePeople.get(t), 1);
                }
            }

            callback.onCallback(met);
        }

        @Override
        public void getMyLastLocation(Account account, Callback<Location> callback) {
        }
    };

    @Test
    public void noEvolutionWithInfection() {

        Carrier me = new Layman(INFECTED);

        assertThat(me.setIllnessProbability(.5f), equalTo(false));

        InfectionAnalyst analyst = new ConcreteAnalysis(me, mockReceiver);
        analyst.updateInfectionPredictions(testLocation, new Date(1585223373980L), n -> {});
        assertThat(me.getInfectionStatus(), equalTo(INFECTED));

    }

    @Test
    public void probabilityIsUpdatedAfterContactWithInfected() {

        Carrier me = new Layman(HEALTHY);

        InfectionAnalyst analyst = new ConcreteAnalysis(me, mockReceiver);
        analyst.updateInfectionPredictions(testLocation, new Date(1585220363913L), n -> {});
        assertThat(me.getInfectionStatus(), equalTo(HEALTHY));
        assertThat(me.getIllnessProbability(),greaterThan(0.f));
    }

    class CityDataReceiver implements DataReceiver {
        @Override
        public void getUserNearby(Location l, Date date, Callback<Set<? extends Carrier>> callback) {
            GeoPoint location = new GeoPoint(l.getLatitude(), l.getLongitude());
            if (city.containsKey(location) && city.get(location).containsKey(date)) {
                callback.onCallback(city.get(location).get(date));
            } else {
                callback.onCallback(Collections.emptySet());
            }
        }

        private boolean timeIsInRange(long k, Date startDate, Date endDate) {
            return startDate.getTime() <= k && k <= endDate.getTime();
        }

        private void merge(Map<Carrier, Integer> r, Carrier carrier) {
            if (r.containsKey(carrier)) {
                r.put(carrier, r.get(carrier)+1);
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
        public void getUserNearbyDuring(Location l, Date startDate, Date endDate, Callback<Map<? extends Carrier, Integer>> callback) {
            GeoPoint location = new GeoPoint(l.getLatitude(), l.getLongitude());

            if (city.containsKey(location)) {
                callback.onCallback(filterByTime(location, startDate, endDate));
            } else {
                callback.onCallback(Collections.emptyMap());
            }

    }

        Location myCurrentLocation;

        void setMyCurrentLocation(Location here) {
            myCurrentLocation = here;
        }

        @Override
        public void getMyLastLocation(Account account, Callback<Location> callback) {
            callback.onCallback(myCurrentLocation);
        }
    }

    private void populateLocation(GeoPoint location, Carrier.InfectionStatus status, int rounds, float startProbability) {
        city.put(location, new HashMap<>());
        for (int i = 0; i < rounds; i++) {
            city.get(location).put(System.currentTimeMillis()+1000*i, Collections.singleton(new Layman(status, startProbability + i)));
        }
    }

    @Test
    public void infectionProbabilityIsUpdated() throws Throwable {

        CityDataReceiver cityReceiver = new CityDataReceiver();
        Carrier me = new Layman(HEALTHY);

        mActivityRule.getActivity().setReceiver(cityReceiver);
        InfectionAnalyst analysis = new ConcreteAnalysis(me, cityReceiver);
        mActivityRule.getActivity().setAnalyst(analysis);

        cityReceiver.setMyCurrentLocation(buildLocation(0, 0));

        mActivityRule.getActivity().runOnUiThread(() -> mActivityRule.getActivity().onModelRefresh(null));

        onView(withId(R.id.my_infection_status)).check(matches(withText("HEALTHY")));

        // I'm going to a healthy location
        GeoPoint healthyLocation = new GeoPoint(12, 13);
        cityReceiver.setMyCurrentLocation(buildLocation(12,13));

        Thread.sleep(10);

        city.put(healthyLocation, new HashMap<>());
        city.get(healthyLocation).put(System.currentTimeMillis(), Collections.singleton(new Layman(UNKNOWN, .89f)));
        city.get(healthyLocation).put(System.currentTimeMillis()+1, Collections.singleton(new Layman(UNKNOWN, .91f)));
        city.get(healthyLocation).put(System.currentTimeMillis()+2, Collections.singleton(new Layman(UNKNOWN, .90f)));
        city.get(healthyLocation).put(System.currentTimeMillis()+3, Collections.singleton(new Layman(UNKNOWN, .92f)));

        Thread.sleep(10);

        mActivityRule.getActivity().runOnUiThread(() -> mActivityRule.getActivity().onModelRefresh(null));
        Thread.sleep(10);

        onView(withId(R.id.my_infection_status)).check(matches(withText("HEALTHY")));
        Thread.sleep(3000);
        // This location is full of ill people
        GeoPoint badLocation = new GeoPoint(40, 113.4);
        city.put(badLocation, new HashMap<>());
        long nowMillis = System.currentTimeMillis();
        for (int i = 0; i < 20; i++) {
            city.get(badLocation).put(nowMillis+i, Collections.singleton(new Layman(INFECTED)));
        }
        Thread.sleep(30);

        mActivityRule.getActivity().runOnUiThread(() -> mActivityRule.getActivity().onModelRefresh(null));

        // I was still on healthyLocation
        onView(withId(R.id.my_infection_status)).check(matches(withText("HEALTHY")));

        Thread.sleep(2000);

        nowMillis = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            city.get(badLocation).put(nowMillis+i*1000, Collections.singleton(new Layman(UNKNOWN, .99f + i)));
        }
        Thread.sleep(5000);

        // I go to the bad location
        cityReceiver.setMyCurrentLocation(buildLocation(40, 113.4));

        mActivityRule.getActivity().runOnUiThread(() -> mActivityRule.getActivity().onModelRefresh(null));
        Thread.sleep(10);

        // Now there should be some risk that I was infected
        onView(withId(R.id.my_infection_status)).check(matches(withText("UNKNOWN")));

    }

    @Test
    public void locationEqualityTest() {
        GeoPoint a = new GeoPoint(12,13.24);
        GeoPoint b = new GeoPoint(12,13.24);
        assertThat(a.equals(b), equalTo(true));
    }
    @Test
    public void getCarrierReturnsACopyOfTheCarrier(){
        Carrier me = new Layman(HEALTHY);
        InfectionAnalyst analyst = new ConcreteAnalysis(me, mockReceiver);
        assertNotNull(analyst.getCarrier());
        assertSame(me,analyst.getCarrier());
    }
}
