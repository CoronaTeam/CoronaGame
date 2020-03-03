package ch.epfl.sdp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntro2Fragment;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class IntroActivity extends AppIntro2 {

    private static int BG_COLOR = Color.rgb(255, 255, 255);
    private static int TITLE_COLOR = Color.rgb(0, 0, 0);
    private static int DESC_COLOR = Color.rgb(30, 30, 30);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(AppIntro2Fragment.newInstance(
                "Page 1",
                "Description 1",
                R.drawable.ic_launcher_foreground,
                BG_COLOR,
                TITLE_COLOR,
                DESC_COLOR
        ));

        addSlide(AppIntro2Fragment.newInstance(
                "Page 2",
                "Description 2",
                R.drawable.ic_launcher_foreground,
                BG_COLOR,
                TITLE_COLOR,
                DESC_COLOR
        ));

        addSlide(AppIntro2Fragment.newInstance(
                "Page 3",
                "Description 3",
                R.drawable.ic_launcher_foreground,
                BG_COLOR,
                TITLE_COLOR,
                DESC_COLOR
        ));

    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        // Navigate to main screen
        startActivity(new Intent(this, MainActivity.class));
    }
}
