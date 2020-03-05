package ch.epfl.sdp;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FirebaseActivityTest {

    @Rule
    public GrantPermissionRule internetPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.INTERNET);


    @Test
    public void testDetectNoInternetConnection(){

    }

    @Test
    public void testDataDownloadIsDisplayed(){

    }
    @Test
    public void testHandleDataDownloadWithNoInternetConnection(){

    }

    @Test
    public void testDataIsCorrectlyUploaded(){

    }

    @Test
    public void testDataUploadIsDisplayed(){

    }

}
