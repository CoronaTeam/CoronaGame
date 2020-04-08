package ch.epfl.sdp;

import android.location.Location;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;

import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.DataSender;
import ch.epfl.sdp.contamination.GridFirestoreInteractor;
import ch.epfl.sdp.contamination.Layman;

import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.contamination.GridFirestoreInteractor.COORDINATE_PRECISION;

/*
This class is used for creating fake data for the app demo.

DEMO FOR USERS LOCATED ON MAP:
Create a grid with lots of users at some place and less at some other place.
These places are located around EPFL.
 */
@Ignore
public class DataForDemo {

    private GridFirestoreInteractor gridFirestoreInteractor = new GridFirestoreInteractor();
    private DataSender dataSender = (carrier, location, time) ->
            gridFirestoreInteractor.write(location, String.valueOf(time.getTime()), carrier,
                    o -> System.out.println("location successfully added to firestore"),
                    e -> System.out.println("location could not be uploaded to firestore")
                );
    private static double DENSE_INITIAL_EPFL_LATITUDE = 46.51700;
    private static double DENSE_INITIAL_EPFL_LONGITUDE = 6.56600;
    private static double SPARSE_INITIAL_EPFL_LATITUDE = 46.51800;
    private static double SPARSE_INITIAL_EPFL_LONGITUDE = 6.56700;

    /**
     * Generate 30 users around 46.51700,6.56600 and 5 users around 46.51800, 6.56700.
     * These latitude, longitude correspond to areas at EPFL.
     */
     @Test
    public void upload2GroupsFakeUsersLocations() {
        Date rightNow = new Date(System.currentTimeMillis());

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
                Location userLocation = newLoc(DENSE_INITIAL_EPFL_LATITUDE + i/COORDINATE_PRECISION,
                        DENSE_INITIAL_EPFL_LONGITUDE + j/COORDINATE_PRECISION);
                dataSender.registerLocation(carrier,userLocation,rightNow);
            }
        }

        // sparse location forms square of side 6
        // there are 2 infected people in this location, and 3 healthy

        // infected at (0, 0)
        Carrier infectedCarrier1 = new Layman(Carrier.InfectionStatus.INFECTED, 1);
        Location location1 = newLoc(SPARSE_INITIAL_EPFL_LATITUDE,
                SPARSE_INITIAL_EPFL_LONGITUDE);
        dataSender.registerLocation(infectedCarrier1,location1,rightNow);

        // infected at (0, 5)
        Carrier infectedCarrier2 = new Layman(Carrier.InfectionStatus.INFECTED, 1);
        Location location2 = newLoc(SPARSE_INITIAL_EPFL_LATITUDE,
                SPARSE_INITIAL_EPFL_LONGITUDE+5/COORDINATE_PRECISION);
        dataSender.registerLocation(infectedCarrier2,location2,rightNow);

        // healthy at (2, 2)
        Carrier healthyCarrier1 = new Layman(Carrier.InfectionStatus.HEALTHY, 0);
        Location location3 = newLoc(SPARSE_INITIAL_EPFL_LATITUDE+2/COORDINATE_PRECISION,
                SPARSE_INITIAL_EPFL_LONGITUDE+2/COORDINATE_PRECISION);
        dataSender.registerLocation(healthyCarrier1,location3,rightNow);

        //healthy at (5, 0)
        Carrier healthyCarrier2 = new Layman(Carrier.InfectionStatus.HEALTHY, 0);
        Location location4 = newLoc(SPARSE_INITIAL_EPFL_LATITUDE+5/COORDINATE_PRECISION,
                SPARSE_INITIAL_EPFL_LONGITUDE);
        dataSender.registerLocation(healthyCarrier2,location4,rightNow);

        // healthy at (5, 5)
        Carrier healthyCarrier3 = new Layman(Carrier.InfectionStatus.HEALTHY, 0);
        Location location5 = newLoc(SPARSE_INITIAL_EPFL_LATITUDE+5/COORDINATE_PRECISION,
                SPARSE_INITIAL_EPFL_LONGITUDE+5/COORDINATE_PRECISION);
        dataSender.registerLocation(healthyCarrier3,location5,rightNow);
    }

    /**
     * Generate 1000 HEALTHY,HEALTHY_CARRIER,INFECTED,IMMUNE,UNKNOWN users starting from location 4600000, 600000
     */
    @Test
    public void uploadBunchOfUsersAtEPFL() {
        Date rightNow = new Date(System.currentTimeMillis());
        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j < 100; ++j){
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
}
