package ch.epfl.sdp;

import android.graphics.Color;
import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import ch.epfl.sdp.identity.fragment.AuthenticationFragment;

public class IntroActivity extends AppIntro {

    private static int BG_COLOR = Color.rgb(255, 255, 255);
    private static int TITLE_COLOR = Color.rgb(0, 0, 0);
    private static int DESC_COLOR = Color.rgb(30, 30, 30);

    private static class Slide {
        final int title, description;
        final int drawable;
        final int backgroundColor, titleColor, descriptionColor;

        Slide(
                int title, int description,
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

    private static Slide[] slides = {
            new Slide(
                    R.string.intro_page1_title,
                    R.string.intro_page1_description,
                    R.drawable.heatmap_pin,
                    BG_COLOR,
                    TITLE_COLOR,
                    DESC_COLOR
            ),
            new Slide(
                    R.string.intro_page2_title,
                    R.string.intro_page2_description,
                    R.drawable.cured_sick,
                    BG_COLOR,
                    TITLE_COLOR,
                    DESC_COLOR
            ),
            new Slide(
                    R.string.intro_page3_title,
                    R.string.intro_page3_description,
                    R.drawable.infection_probability_curve,
                    BG_COLOR,
                    TITLE_COLOR,
                    DESC_COLOR
            ),
            new Slide(
                    R.string.intro_page4_title,
                    R.string.intro_page4_description,
                    R.drawable.lock,
                    BG_COLOR,
                    TITLE_COLOR,
                    DESC_COLOR
            )
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        for (Slide slide : slides) {
            addSlide(AppIntroFragment.newInstance(new SliderPage(
                    getString(slide.title),
                    getString(slide.description),
                    slide.drawable,
                    slide.backgroundColor,
                    slide.titleColor,
                    slide.descriptionColor
            )));
        }
        addSlide(new AuthenticationFragment());
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) AuthenticationFragment.signInComplete(this);

        showSkipButton(false);
        setIndicatorColor(Color.BLACK, Color.DKGRAY);
        setNextArrowColor(Color.BLACK);
        findViewById(R.id.done).setAlpha(0); //FIXME: showDoneButton(bool) is deprecated
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
    }
}
