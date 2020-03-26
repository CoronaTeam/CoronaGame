package ch.epfl.sdp.contamination;

import android.annotation.TargetApi;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;

import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;

public class GridSenderTest {

    @Rule
    public final ActivityTestRule<DataExchangeActivity> mActivityRule = new ActivityTestRule<>(DataExchangeActivity.class);

    @TargetApi(17)
    private Location buildLocation(double latitude, double longitude) {
        Location l = new Location(LocationManager.GPS_PROVIDER);
        l.setLatitude(latitude);
        l.setLongitude(longitude);
        l.setTime(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            // Also need to set the et field
            l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        l.setAltitude(400);
        l.setAccuracy(1);
        return l;
    }

    @Test
    public void dataSenderUploadsInformation() {


        //mActivityRule.getActivity().sender.registerLocation(new Layman(Carrier.InfectionStatus.INFECTED), buildLocation(10, 11), new Date(System.currentTimeMillis()));

        /*
        DataSender sender = new ConcreteDataSender(
                new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance()),
                AccountGetting.getAccount(mActivityRule.getActivity()));


        Date d1 = new Date(System.currentTimeMillis());
        Date d2 = new Date(System.currentTimeMillis()+1);
        //sender.registerLocation(new Layman(Carrier.InfectionStatus.HEALTHY), buildLocation(10, 11), d1);
        //sender.registerLocation(new Layman(Carrier.InfectionStatus.HEALTHY_CARRIER), buildLocation(10, 11), d2);

        QueryHandler showResult = new QueryHandler() {
            @Override
            public void onSuccess(QuerySnapshot snapshot) {
                for (QueryDocumentSnapshot q : snapshot) {
                    Log.e("DB", q.getData().toString());
                }
            }

            @Override
            public void onFailure() {
                assert false : "Failed";
            }
        };

        ((GridFirestoreInteractor)sender).read(buildLocation(10, 11), "1585179092002", showResult);
         */
    }

}
