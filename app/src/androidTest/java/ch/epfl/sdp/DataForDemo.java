package ch.epfl.sdp;

import android.location.Location;

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

import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.contamination.GridFirestoreInteractor.COORDINATE_PRECISION;

import java.util.Random;
import java.util.UUID;

/**
 * This class is used for creating fake data for the app demo.
 * <p>
 * DEMO FOR USERS LOCATED ON MAP:
 * Create a grid with lots of users at some place and less at some other place.
 * These places are located around EPFL.
 */
//@Ignore("This is not a proper test, it is used for testing and demos, but it does not test anything, only generates data.")
public class DataForDemo {
    private Random r = new Random();
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
    @Test
    public void upload2GroupsFakeUsersLocations() {
        // dense location forms a square of side 6
        // dense location infected forms a square of side 4 (16 infected people and 20 healthy)
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 6; ++j) {
                Carrier carrier;
                if (i < 4 && j < 4) {
                    carrier = new Layman(Carrier.InfectionStatus.INFECTED, 1);
                } else {
                    carrier = new Layman(Carrier.InfectionStatus.HEALTHY, 0);

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
    @Test
    public void uploadBunchOfUsersAtEPFL() {
        Date rightNow = new Date(System.currentTimeMillis());


        for (int i = 0; i < 150; ++i) {
            for (int j = 0; j < 150; ++j) {
                Carrier carrier;
                if (i < 50 && j < 50) {
                    carrier = new Layman(Carrier.InfectionStatus.INFECTED, 1);
                } else if (i < 70 && j < 70) {
                    carrier = new Layman(Carrier.InfectionStatus.UNKNOWN, 0.5f);
                } else if (i < 90 && j < 90) {
                    carrier = new Layman(Carrier.InfectionStatus.IMMUNE, 0);
                } else if (i < 110 && j < 110) {
                    carrier = new Layman(Carrier.InfectionStatus.HEALTHY_CARRIER, 1);
                } else {
                    carrier = new Layman(Carrier.InfectionStatus.HEALTHY, 0);
                }
                Location userLocation = newLoc(DENSE_INITIAL_EPFL_LATITUDE + getRandomNumberBetweenBounds(-0.01, 0.01),
                        DENSE_INITIAL_EPFL_LONGITUDE + getRandomNumberBetweenBounds(-0.02, 0.02));
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

        gridFirestoreInteractor.writeDocument("LastPositions/", element, o -> {
        }, e -> {
        });
    }
}