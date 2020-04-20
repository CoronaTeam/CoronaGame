package ch.epfl.sdp;

import android.location.Location;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.contamination.CachingDataSender;
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.GridFirestoreInteractor;
import ch.epfl.sdp.contamination.Layman;

import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.contamination.GridFirestoreInteractor.COORDINATE_PRECISION;

/**
 * This class is used for creating fake data for the app demo.
 *
 * DEMO FOR USERS LOCATED ON MAP:
 * Create a grid with lots of users at some place and less at some other place.
 * These places are located around EPFL.
 */
@Ignore("This is not a proper test, it is used for testing and demos, but it does not test anything, only generates data.")
public class DataForDemo {

    private GridFirestoreInteractor gridFirestoreInteractor = new GridFirestoreInteractor();
    private CachingDataSender dataSender = new CachingDataSender() {
        @Override
        public CompletableFuture<Void> registerLocation(Carrier carrier, Location location, Date time) {
            gridFirestoreInteractor.write(location, String.valueOf(time.getTime()), carrier,
                    o -> System.out.println("location successfully added to firestore"),
                    e -> System.out.println("location could not be uploaded to firestore")
            );
            return null;
        }

        @Override
        public CompletableFuture<Void> registerLocation(Carrier carrier, Location location, Date time, OnSuccessListener successListener, OnFailureListener failureListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sendAlert(String userId) {

        }

        @Override
        public void sendAlert(String userId, float previousIllnessProbability) {

        }

        @Override
        public void resetSickAlerts(String userId) {

        }

        @Override
        public SortedMap<Date, Location> getLastPositions() {
            return null;
        }
    };

    private static double DENSE_INITIAL_EPFL_LATITUDE = 46.51700;
    private static double DENSE_INITIAL_EPFL_LONGITUDE = 6.56600;
    private static double SPARSE_INITIAL_EPFL_LATITUDE = 46.51800;
    private static double SPARSE_INITIAL_EPFL_LONGITUDE = 6.56700;
    private Date rightNow = new Date(System.currentTimeMillis());

    /**
     * Generate 30 users around 46.51700,6.56600 and 5 users around 46.51800, 6.56700.
     * These latitude, longitude correspond to areas at EPFL.
     */
     @Test
     public void upload2GroupsFakeUsersLocations() {
         // dense location forms a square of side 6
         // dense location infected forms a square of side 4 (16 infected people and 20 healthy)
         for (double i = 0; i < 6; ++i) {
             for (double j = 0; j < 6; ++j) {
                 Carrier carrier;
                 if (i < 4 && j < 4) {
                     carrier = new Layman(Carrier.InfectionStatus.INFECTED, 1);
                 } else {
                     carrier = new Layman(Carrier.InfectionStatus.HEALTHY, 0);

                 }
                 Location userLocation = newLoc(DENSE_INITIAL_EPFL_LATITUDE + i/COORDINATE_PRECISION,
                         DENSE_INITIAL_EPFL_LONGITUDE + j /COORDINATE_PRECISION);
                 dataSender.registerLocation(carrier,userLocation,rightNow);
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
        for (double i = 0; i < 100; ++i) {
            for (double j = 0; j < 100; ++j){
                Carrier carrier;
                if (i < 40 && j < 40) {
                    carrier = new Layman(Carrier.InfectionStatus.INFECTED, 1);
                } else if (i < 60 && j < 60) {
                    carrier = new Layman(Carrier.InfectionStatus.UNKNOWN, 0.5f);
                } else if (i < 80 && j < 80) {
                    carrier = new Layman(Carrier.InfectionStatus.IMMUNE, 0);
                } else if (i < 90 && j < 90) {
                    carrier = new Layman(Carrier.InfectionStatus.HEALTHY_CARRIER, 1);
                } else {
                    carrier = new Layman(Carrier.InfectionStatus.HEALTHY, 0);
                }
                Location userLocation = newLoc(DENSE_INITIAL_EPFL_LATITUDE + i/COORDINATE_PRECISION,
                        DENSE_INITIAL_EPFL_LONGITUDE + j/COORDINATE_PRECISION);
                dataSender.registerLocation(carrier,userLocation,rightNow);
            }
        }
    }
    private void sparseCarrierAndPositionCreation(Carrier.InfectionStatus infectionStatus,
                                                  float infectionProbability, double latiOffset,
                                                  double longiOffset){
        Carrier carrier = new Layman(infectionStatus, infectionProbability);
        Location location = newLoc(SPARSE_INITIAL_EPFL_LATITUDE+latiOffset/COORDINATE_PRECISION,
                SPARSE_INITIAL_EPFL_LONGITUDE+longiOffset/COORDINATE_PRECISION);
        dataSender.registerLocation(carrier, location, rightNow);
    }
}
