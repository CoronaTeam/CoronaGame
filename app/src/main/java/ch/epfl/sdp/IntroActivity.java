package ch.epfl.sdp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntro2Fragment;

public class IntroActivity extends AppIntro2 {

    private static final int BG_COLOR = Color.rgb(255, 255, 255);
    private static final int TITLE_COLOR = Color.rgb(0, 0, 0);
    private static final int DESC_COLOR = Color.rgb(30, 30, 30);
    private static final Slide[] slides = {
            new Slide(
                    "Page 1",
                    "Description 1",
                    R.drawable.one,
                    BG_COLOR,
                    TITLE_COLOR,
                    DESC_COLOR
            ),
            new Slide(
                    "Page 2",
                    "Description 2",
                    R.drawable.two,
                    BG_COLOR,
                    TITLE_COLOR,
                    DESC_COLOR
            ),
            new Slide(
                    "Page 3",
                    "Description 3",
                    R.drawable.three,
                    BG_COLOR,
                    TITLE_COLOR,
                    DESC_COLOR
            )
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        for (Slide slide : slides) {
            addSlide(AppIntro2Fragment.newInstance(
                    slide.title,
                    slide.description,
                    slide.drawable,
                    slide.backgroundColor,
                    slide.titleColor,
                    slide.descriptionColor
            ));
        }
        showSkipButton(false);
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        // Navigate to main screen
        startActivity(new Intent(this, MainActivity.class));
    }

    private static class Slide {
        final String title, description;
        final int drawable;
        final int backgroundColor, titleColor, descriptionColor;

        Slide(
                String title, String description,
                int drawable,
                int backgroundColor, int titleColor, int descriptionColor
        ) {
            this.title = title;
            this.description = description;
            this.drawable = drawable;
            this.backgroundColor = backgroundColor;
            this.titleColor = titleColor;
            this.descriptionColor = descriptionColor;
        }
    }
}
