package ch.epfl.sdp;

import android.app.Activity;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ch.epfl.sdp.contamination.CachingDataSender;
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.ConcreteCachingDataSender;
import ch.epfl.sdp.contamination.GridFirestoreInteractor;
import ch.epfl.sdp.contamination.Layman;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;

import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.contamination.GridFirestoreInteractor.COORDINATE_PRECISION;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import java.util.UUID;

/**
 * This class is used for creating fake data for the app demo.
 * <p>
 * DEMO FOR USERS LOCATED ON MAP:
 * Create a grid with lots of users at some place and less at some other place.
 * These places are located around EPFL.
 *
 * DEMO FOR USER PATH ON MAP:
 * Create 2 different paths on 2 different days of the same user.
 * Create infected people met on these paths.
 */
@Ignore("This is not a proper test, it is used for testing and demos, but it does not test anything, only generates data.")
public class DataForDemo {
   /* private Random r = new Random();
    private GridFirestoreInteractor gridFirestoreInteractor = new GridFirestoreInteractor();
    private CachingDataSender dataSender = new ConcreteCachingDataSender(gridFirestoreInteractor) {
        @Override
        public void registerLocation(Carrier carrier, Location location, Date time) {
            gridFirestoreInteractor.write(location, String.valueOf(time.getTime()), carrier,
                    o -> System.out.println("location successfully added to firestore"),
                    e -> System.out.println("location could not be uploaded to firestore")
            );
        }

        @Override
        public void registerLocation(Carrier carrier, Location location, Date time, OnSuccessListener successListener, OnFailureListener failureListener) {
            throw new UnsupportedOperationException();
        }
    };

    private static double DENSE_INITIAL_EPFL_LATITUDE = 46.51800;
    private static double DENSE_INITIAL_EPFL_LONGITUDE = 6.56600;
    private static double SPARSE_INITIAL_EPFL_LATITUDE = 46.51900;
    private static double SPARSE_INITIAL_EPFL_LONGITUDE = 6.56700;
    private Date rightNow = new Date(System.currentTimeMillis());

    double getRandomNumberBetweenBounds(double lower, double upper){
        return r.nextDouble() * (upper-lower) + lower;
    }

    /**
     * Generate 30 users around 46.51700,6.56600 and 5 users around 46.51800, 6.56700.
     * These latitude, longitude correspond to areas at EPFL.
     */
   /* @Test
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
                element.put("geoPoint", new GeoPoint(userLocation.getLatitude(), userLocation.getLongitude()));
                element.put("timeStamp", Timestamp.now());
                // db.writeDocument("History/" + userAccount.getId() + "/Positions", element, o -> { }, e -> { });

                gridFirestoreInteractor.writeDocument("LastPositions/", element, o -> {
                }, e -> {
                });
            }
        }

        // sparse location forms square of side 6
        // there are 2 infected people in this location, and 3 healthy

        // infected at coordinate (0, 0) of the square
        sparseCarrierAndPositionCreation(Carrier.InfectionStatus.INFECTED, 1, 0, 0);

        // infected at (0, 5)
        sparseCarrierAndPositionCreation(Carrier.InfectionStatus.INFECTED, 1, 0, 5);

        // healthy at (2, 2)
        sparseCarrierAndPositionCreation(Carrier.InfectionStatus.HEALTHY, 0, 2, 2);

        // healthy at (5, 0)
        sparseCarrierAndPositionCreation(Carrier.InfectionStatus.HEALTHY, 0, 5, 0);

        // healthy at (5, 5)
        sparseCarrierAndPositionCreation(Carrier.InfectionStatus.HEALTHY, 0, 5, 5);
    }

    /**
     * Generate 1000 HEALTHY,HEALTHY_CARRIER,INFECTED,IMMUNE,UNKNOWN users starting from location 4600000, 600000
     */

   /* @Test
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
                element.put("geoPoint", new GeoPoint(userLocation.getLatitude(), userLocation.getLongitude()));
                element.put("timeStamp", Timestamp.now());
                element.put("infectionStatus", carrier.getInfectionStatus());
                // db.writeDocument("History/" + userAccount.getId() + "/Positions", element, o -> { }, e -> { });

                gridFirestoreInteractor.writeDocument("LastPositions/", element, o -> {
                }, e -> {
                });
            }
        }
    }

    private void sparseCarrierAndPositionCreation(Carrier.InfectionStatus infectionStatus,
                                                  float infectionProbability, double latiOffset,
                                                  double longiOffset) {
        Carrier carrier = new Layman(infectionStatus, infectionProbability);
        Location userLocation = newLoc(SPARSE_INITIAL_EPFL_LATITUDE + getRandomNumberBetweenBounds(-0.01, 0.01),
                SPARSE_INITIAL_EPFL_LONGITUDE + getRandomNumberBetweenBounds(-0.01, 0.01));
        dataSender.registerLocation(carrier, userLocation, rightNow);

        Map<String, Object> element = new HashMap<>();
        element.put("geoPoint", new GeoPoint(userLocation.getLatitude(), userLocation.getLongitude()));
        element.put("timeStamp", Timestamp.now());
        // db.writeDocument("History/" + userAccount.getId() + "/Positions", element, o -> { }, e -> { });

        gridFirestoreInteractor.writeDocument("LastPositions/", element, o -> { }, e -> { });
    }*/

    // write in History Collection on Firestore, user with ID USER_PATH_DEMO
    @Test
    public void uploadUserPaths() {
        ConcreteFirestoreInteractor cfi = new ConcreteFirestoreInteractor();
        final boolean[] pathLoaded = new boolean[1];
        double lat = 50.0;//33.39767645465177;
        double longi = -73.0;//-118.39439114221236;
        for (double i=0; i<50*0.001; i=i+0.001) {
            Location location = TestUtils.buildLocation(lat + i, longi + i);
            Map<String, Object> position = new HashMap();
            position.put("Position", new PositionRecord(Timestamp.now(),
                    new GeoPoint(location.getLatitude(), location.getLongitude())));
            cfi.writeDocument("History/USER_PATH_DEMO2/Positions/", position,
                    s -> pathLoaded[0] = true,
                    f -> {
                        pathLoaded[0] = false;
                        Log.d("PATH UPLOAD", "Error uploading positions Firestore.", f);
                    });
        }
    }

}