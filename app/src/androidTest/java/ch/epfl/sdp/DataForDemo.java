package ch.epfl.sdp;

import android.location.Location;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.mapbox.geojson.Point;

import org.junit.Ignore;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.Layman;
import ch.epfl.sdp.contamination.databaseIO.CachingDataSender;
import ch.epfl.sdp.contamination.databaseIO.ConcreteCachingDataSender;
import ch.epfl.sdp.contamination.databaseIO.GridFirestoreInteractor;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.location.LocationUtils;

import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.firestore.FirestoreInteractor.collectionReference;
import static ch.epfl.sdp.firestore.FirestoreLabels.GEOPOINT_TAG;
import static ch.epfl.sdp.firestore.FirestoreLabels.INFECTION_STATUS_TAG;
import static ch.epfl.sdp.firestore.FirestoreLabels.LAST_POSITIONS_COLL;
import static ch.epfl.sdp.firestore.FirestoreLabels.TIMESTAMP_TAG;

/**
 * This class is used for creating fake data for the app demo.
 * <p>
 * DEMO FOR USERS LOCATED ON MAP:
 * Create a grid with lots of users at some place and less at some other place.
 * These places are located around EPFL.
 * <p>
 * DEMO FOR USER PATH ON MAP:
 * Create 2 different paths on 2 different days of the same user.
 * Create infected people met on these paths.
 */
@Ignore("This is not a proper test, it is used for testing and demos, but it does not test anything, only generates data.")
public class DataForDemo {
    private static final double DENSE_INITIAL_EPFL_LATITUDE = 46.51800;
    private static final double DENSE_INITIAL_EPFL_LONGITUDE = 6.56600;
    private final Random r = new Random();
    private final GridFirestoreInteractor gridFirestoreInteractor = new GridFirestoreInteractor();
    private final CachingDataSender dataSender = new ConcreteCachingDataSender(gridFirestoreInteractor);

    private final Date rightNow = new Date(System.currentTimeMillis());

    private List<Point> routeCoordinates;
    private int[] infectedOnRoute;

    private double getRandomNumberBetweenBounds(double lower, double upper) {
        return r.nextDouble() * (upper - lower) + lower;
    }


    @Test
    public void addInfectedPoints() throws ParseException {
        Carrier carrier = new Layman(Carrier.InfectionStatus.INFECTED);
        Location userLocation = newLoc(46.52383, 6.56564);
        Date time = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse("04/06/2020 18:08:30");
        dataSender.registerLocation(carrier, userLocation, time);
    }

    /**
     * Generate 30 users around 46.51700,6.56600 and 5 users around 46.51800, 6.56700.
     * These latitude, longitude correspond to areas at EPFL.
     */
    @Test
    public void upload2GroupsFakeUsersLocations() {
        // dense location forms a square of side 6
        // dense location infected forms a square of side 4 (16 infected people and 20 healthy)
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 6; ++j) {
                Carrier carrier;
                if (i < 4 && j < 4) {
                    carrier = new Layman(Carrier.InfectionStatus.INFECTED);
                } else {
                    carrier = new Layman(Carrier.InfectionStatus.HEALTHY);

                }
                Location userLocation = newLoc(DENSE_INITIAL_EPFL_LATITUDE + getRandomNumberBetweenBounds(-0.01, 0.01),
                        DENSE_INITIAL_EPFL_LONGITUDE + getRandomNumberBetweenBounds(-0.01, 0.01));
                dataSender.registerLocation(carrier, userLocation, rightNow);

                Map<String, Object> element = new HashMap<>();
                element.put(GEOPOINT_TAG, new GeoPoint(userLocation.getLatitude(), userLocation.getLongitude()));
                element.put(TIMESTAMP_TAG, Timestamp.now());
                // db.writeDocument("History/" + userAccount.getId() + "/Positions", element, o -> { }, e -> { });

                gridFirestoreInteractor.writeDocument(collectionReference(LAST_POSITIONS_COLL), element);
            }
        }

        // sparse location forms square of side 6
        // there are 2 infected people in this location, and 3 healthy

        // infected at coordinate (0, 0) of the square
        double SPARSE_INITIAL_EPFL_LONGITUDE = 6.56700;
        double SPARSE_INITIAL_EPFL_LATITUDE = 46.51900;
        carrierAndPositionCreationUpload(Carrier.InfectionStatus.INFECTED, 1,
                SPARSE_INITIAL_EPFL_LATITUDE + getRandomNumberBetweenBounds(-0.01, 0.01),
                SPARSE_INITIAL_EPFL_LONGITUDE + getRandomNumberBetweenBounds(-0.01, 0.01),
                rightNow);

        // infected at (0, 5)
        carrierAndPositionCreationUpload(Carrier.InfectionStatus.INFECTED, 1,
                SPARSE_INITIAL_EPFL_LATITUDE + getRandomNumberBetweenBounds(-0.01, 0.01),
                SPARSE_INITIAL_EPFL_LONGITUDE + getRandomNumberBetweenBounds(-0.01, 0.01),
                rightNow);

        // healthy at (2, 2)
        carrierAndPositionCreationUpload(Carrier.InfectionStatus.HEALTHY, 0,
                SPARSE_INITIAL_EPFL_LATITUDE + getRandomNumberBetweenBounds(-0.01, 0.01),
                SPARSE_INITIAL_EPFL_LONGITUDE + getRandomNumberBetweenBounds(-0.01, 0.01),
                rightNow);

        // healthy at (5, 0)
        carrierAndPositionCreationUpload(Carrier.InfectionStatus.HEALTHY, 0,
                SPARSE_INITIAL_EPFL_LATITUDE + getRandomNumberBetweenBounds(-0.01, 0.01),
                SPARSE_INITIAL_EPFL_LONGITUDE + getRandomNumberBetweenBounds(-0.01, 0.01),
                rightNow);

        // healthy at (5, 5)
        carrierAndPositionCreationUpload(Carrier.InfectionStatus.HEALTHY, 0,
                SPARSE_INITIAL_EPFL_LATITUDE + getRandomNumberBetweenBounds(-0.01, 0.01),
                SPARSE_INITIAL_EPFL_LONGITUDE + getRandomNumberBetweenBounds(-0.01, 0.01),
                rightNow);
    }

    /**
     * Generate 1000 HEALTHY,HEALTHY_CARRIER,INFECTED,IMMUNE,UNKNOWN users starting from location 4600000, 600000
     */
    @Test
    public void uploadBunchOfUsersAtEPFL() {
        Date rightNow = new Date(System.currentTimeMillis());


        for (int i = 0; i < 150; ++i) {
            for (int j = 0; j < 150; ++j) {
                Carrier carrier;
                if (i < 50 && j < 50) {
                    carrier = new Layman(Carrier.InfectionStatus.INFECTED);
                } else if (i < 70 && j < 70) {
                    carrier = new Layman(Carrier.InfectionStatus.UNKNOWN, 0.5f);
                } else {
                    carrier = new Layman(Carrier.InfectionStatus.HEALTHY);
                }
                Location userLocation = newLoc(DENSE_INITIAL_EPFL_LATITUDE + getRandomNumberBetweenBounds(-0.01, 0.01),
                        DENSE_INITIAL_EPFL_LONGITUDE + getRandomNumberBetweenBounds(-0.02, 0.02));
                dataSender.registerLocation(carrier, userLocation, rightNow);

                Map<String, Object> element = new HashMap<>();
                element.put(GEOPOINT_TAG, new GeoPoint(userLocation.getLatitude(), userLocation.getLongitude()));
                element.put(TIMESTAMP_TAG, Timestamp.now());
                element.put(INFECTION_STATUS_TAG, carrier.getInfectionStatus());
                //db.writeDocument("History/" + userAccount.getId() + "/Positions", element, o -> { }, e -> { });

                gridFirestoreInteractor.writeDocument(collectionReference(LAST_POSITIONS_COLL), element);
            }
        }
    }

    private void carrierAndPositionCreationUpload(Carrier.InfectionStatus infectionStatus,
                                                  float infectionProbability, double lat,
                                                  double lon, Date date) {
        Carrier carrier = new Layman(infectionStatus, infectionProbability);
        Location userLocation = newLoc(lat, lon);
        dataSender.registerLocation(carrier, userLocation, date);

        Map<String, Object> element = new HashMap<>();
        element.put(GEOPOINT_TAG, new GeoPoint(userLocation.getLatitude(), userLocation.getLongitude()));
        element.put(TIMESTAMP_TAG, Timestamp.now());
        // db.writeDocument("History/" + userAccount.getId() + "/Positions", element, o -> { }, e -> { });

        gridFirestoreInteractor.writeDocument(collectionReference(LAST_POSITIONS_COLL), element);
    }

    // write in History Collection on Firestore, user with ID USER_PATH_DEMO
    @Test
    public void uploadUserPaths() {
        ConcreteFirestoreInteractor cfi = new ConcreteFirestoreInteractor();
        final boolean[] pathLoaded = new boolean[1];
        double lat = 50.0;//33.39767645465177;
        double longitude = -73.0;//-118.39439114221236;
        for (double i = 0; i < 50 * 0.001; i = i + 0.001) {
            Location location = LocationUtils.buildLocation(lat + i, longitude + i);
            Map<String, Object> element = new HashMap<>();
            element.put(GEOPOINT_TAG, new GeoPoint(
                    location.getLatitude(),
                    location.getLongitude()
            ));
            element.put(TIMESTAMP_TAG, Timestamp.now());
            element.put(INFECTION_STATUS_TAG, Carrier.InfectionStatus.UNKNOWN);
            cfi.writeDocument(collectionReference("History/USER_PATH_DEMO2/Positions/"), element)
                    .thenRun(() -> pathLoaded[0] = true)
                    .exceptionally(e -> {
                        pathLoaded[0] = false;
                        Log.d("PATH UPLOAD", "Error uploading positions Firestore.", e);
                        return null;
                    });
        }
    }

    @Test
    public void uploadBetterPath() {
        ConcreteFirestoreInteractor cfi = new ConcreteFirestoreInteractor();
        initRouteCoordinates();
        initInfectedOnRoute();
        int i = 0;
        Log.d("ROUTE SIZE: ", String.valueOf(routeCoordinates.size()));
        for (Point point : routeCoordinates) {
            double lat = point.latitude();
            double lon = point.longitude();
            //Location location = LocationUtils.buildLocation(lat, lon);
            Timestamp timestamp = Timestamp.now();
            Map<String, Object> element = new HashMap<>();
            element.put(GEOPOINT_TAG, new GeoPoint(
                    point.latitude(),
                    point.longitude()
            ));
            element.put(TIMESTAMP_TAG, timestamp);
            element.put(INFECTION_STATUS_TAG, Carrier.InfectionStatus.UNKNOWN);
            cfi.writeDocument(collectionReference("History/THAT_BETTER_PATH/Positions/"), element)
                    .thenRun(() -> Log.d("BETTER PATH UPLOAD", "Success upload positions"))
                    .exceptionally(e -> {
                        Log.d("BETTER PATH UPLOAD", "Error uploading positions", e);
                        return null;
                    });
            if (infectedOnRoute[i] == 1) {
                carrierAndPositionCreationUpload(Carrier.InfectionStatus.INFECTED, 1f, lat, lon, timestamp.toDate());
            }
            i += 1;
        }
        Log.d("LOOPINDEX: ", String.valueOf(i));
    }

    /**
     * This method creates a better looking path for demo.
     * source: https://docs.mapbox.com/android/maps/examples/create-a-line-layer/
     */
    private void initRouteCoordinates() {
        routeCoordinates = new ArrayList<>();
        routeCoordinates.add(Point.fromLngLat(-118.39439114221236, 33.397676454651766));
        routeCoordinates.add(Point.fromLngLat(-118.39421054012902, 33.39769799454838));
        routeCoordinates.add(Point.fromLngLat(-118.39408583869053, 33.39761901490136));
        routeCoordinates.add(Point.fromLngLat(-118.39388373635917, 33.397328225582285));
        routeCoordinates.add(Point.fromLngLat(-118.39372033447427, 33.39728514560042));
        routeCoordinates.add(Point.fromLngLat(-118.3930882271826, 33.39756875508861));
        routeCoordinates.add(Point.fromLngLat(-118.3928216241072, 33.39759029501192));
        routeCoordinates.add(Point.fromLngLat(-118.39227981785722, 33.397234885594564));
        routeCoordinates.add(Point.fromLngLat(-118.392021814881, 33.397005125197666));
        routeCoordinates.add(Point.fromLngLat(-118.39090810203379, 33.396814854409186));
        routeCoordinates.add(Point.fromLngLat(-118.39040499623022, 33.39696563506828));
        routeCoordinates.add(Point.fromLngLat(-118.39005669221234, 33.39703025527067));
        routeCoordinates.add(Point.fromLngLat(-118.38953208616074, 33.39691896489222));
        routeCoordinates.add(Point.fromLngLat(-118.38906338075398, 33.39695127501678));
        routeCoordinates.add(Point.fromLngLat(-118.38891287901787, 33.39686511465794));
        routeCoordinates.add(Point.fromLngLat(-118.38898167981154, 33.39671074380141));
        routeCoordinates.add(Point.fromLngLat(-118.38984598978178, 33.396064537239404));
        routeCoordinates.add(Point.fromLngLat(-118.38983738968255, 33.39582400356976));
        routeCoordinates.add(Point.fromLngLat(-118.38955358640874, 33.3955978295119));
        routeCoordinates.add(Point.fromLngLat(-118.389041880506, 33.39578092284221));
        routeCoordinates.add(Point.fromLngLat(-118.38872797688494, 33.3957916930261));
        routeCoordinates.add(Point.fromLngLat(-118.38817327048618, 33.39561218978703));
        routeCoordinates.add(Point.fromLngLat(-118.3872530598711, 33.3956265500598));
        routeCoordinates.add(Point.fromLngLat(-118.38653065153775, 33.39592811523983));
        routeCoordinates.add(Point.fromLngLat(-118.38638444985126, 33.39590657490452));
        routeCoordinates.add(Point.fromLngLat(-118.38638874990086, 33.395737842093304));
        routeCoordinates.add(Point.fromLngLat(-118.38723155962309, 33.395027006653244));
        routeCoordinates.add(Point.fromLngLat(-118.38734766096238, 33.394441819579285));
        routeCoordinates.add(Point.fromLngLat(-118.38785936686516, 33.39403972556368));
        routeCoordinates.add(Point.fromLngLat(-118.3880743693453, 33.393616088784825));
        routeCoordinates.add(Point.fromLngLat(-118.38791956755958, 33.39331092541894));
        routeCoordinates.add(Point.fromLngLat(-118.3874852625497, 33.39333964672257));
        routeCoordinates.add(Point.fromLngLat(-118.38686605540683, 33.39387816940854));
        routeCoordinates.add(Point.fromLngLat(-118.38607484627983, 33.39396792286514));
        routeCoordinates.add(Point.fromLngLat(-118.38519763616081, 33.39346171215717));
        routeCoordinates.add(Point.fromLngLat(-118.38523203655761, 33.393196040109466));
        routeCoordinates.add(Point.fromLngLat(-118.3849955338295, 33.393023711860515));
        routeCoordinates.add(Point.fromLngLat(-118.38355931726203, 33.39339708930139));
        routeCoordinates.add(Point.fromLngLat(-118.38323251349217, 33.39305243325907));
        routeCoordinates.add(Point.fromLngLat(-118.3832583137898, 33.39244928189641));
        routeCoordinates.add(Point.fromLngLat(-118.3848751324406, 33.39108499551671));
        routeCoordinates.add(Point.fromLngLat(-118.38522773650804, 33.38926830725471));
        routeCoordinates.add(Point.fromLngLat(-118.38508153482152, 33.38916777794189));
        routeCoordinates.add(Point.fromLngLat(-118.38390332123025, 33.39012280171983));
        routeCoordinates.add(Point.fromLngLat(-118.38318091289693, 33.38941192035707));
        routeCoordinates.add(Point.fromLngLat(-118.38271650753981, 33.3896129783018));
        routeCoordinates.add(Point.fromLngLat(-118.38275090793661, 33.38902416443619));
        routeCoordinates.add(Point.fromLngLat(-118.38226930238106, 33.3889451769069));
        routeCoordinates.add(Point.fromLngLat(-118.38258750605169, 33.388420985121336));
        routeCoordinates.add(Point.fromLngLat(-118.38177049662707, 33.388083490107284));
        routeCoordinates.add(Point.fromLngLat(-118.38080728551597, 33.38836353925403));
        routeCoordinates.add(Point.fromLngLat(-118.37928506795642, 33.38717870977523));
        routeCoordinates.add(Point.fromLngLat(-118.37898406448423, 33.3873079646849));
        routeCoordinates.add(Point.fromLngLat(-118.37935386875012, 33.38816247841951));
        routeCoordinates.add(Point.fromLngLat(-118.37794345248027, 33.387810620840135));
        routeCoordinates.add(Point.fromLngLat(-118.37546662390886, 33.38847843095069));
        routeCoordinates.add(Point.fromLngLat(-118.37091717142867, 33.39114243958559));
    }

    private void initInfectedOnRoute() {
        infectedOnRoute = new int[routeCoordinates.size()];
        // we put 1 where we want to add infected points along the routeCoordinates
        infectedOnRoute[4] = 1;
        infectedOnRoute[5] = 1;
        infectedOnRoute[12] = 1;
        infectedOnRoute[14] = 1;
        infectedOnRoute[15] = 1;
        infectedOnRoute[16] = 1;
        infectedOnRoute[32] = 1;
        infectedOnRoute[33] = 1;
        infectedOnRoute[34] = 1;
    }

}