package ch.epfl.sdp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

public class UserInfectionActivity extends AppCompatActivity {

    private Button infectionStatusButton;
    private TextView infectionStatusView;
    private TextView infectionUploadView;
    private User user = new User("Test", User.DEFAULT_FAMILY_NAME, User.DEFAULT_EMAIL,
            User.DEFAULT_URI, User.DEFAULT_PLAYERID, User.DEFAULT_USERID, 25, false);
    private String userName = "Test"; // TODO: this is temporary: we need to get the real current user

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_infection);

        infectionStatusView = findViewById(R.id.infectionStatusView);
        infectionStatusButton = findViewById(R.id.infectionStatusButton);
        infectionUploadView = findViewById(R.id.infectionStatusUploadConfirmation);

        infectionStatusView.setSaveEnabled(true);
        infectionStatusButton.setSaveEnabled(true);

        infectionStatusButton.setOnClickListener(v -> {
            CharSequence buttonText = ((Button)v).getText();
            if (buttonText.equals(getResources().getString(R.string.i_am_infected))) {
                clickAction(infectionStatusButton, infectionStatusView, R.string.i_am_cured,
                        R.string.your_user_status_is_set_to_infected, R.color.colorGreenCured);
                //upload to firebase
                user.modifyUserInfectionStatus(userName, true,
                        value -> infectionUploadView.setText(value));


            }
            else {
                clickAction(infectionStatusButton, infectionStatusView, R.string.i_am_infected,
                        R.string.your_user_status_is_set_to_not_infected, R.color.colorRedInfected);
                //upload to firebase
                user.modifyUserInfectionStatus(userName, false,
                        value -> infectionUploadView.setText(value));
            }
        });
    }

    private void clickAction(Button button, TextView textView, int buttonText, int textViewText, int buttonColor) {
        button.setText(buttonText);
        button.setBackgroundTintList(
                getResources().getColorStateList(buttonColor));
        textView.setText(textViewText);
    }


    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        TextView infectionText = findViewById(R.id.infectionStatusView);
        Button infectionButton = findViewById(R.id.infectionStatusButton);
        outState.putCharSequence("INFECTION_STATUS_TEXT", infectionText.getText());
        outState.putCharSequence("INFECTION_STATUS_BUTTON", infectionButton.getText());
    }

    @Override
    public void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        TextView infectionText = findViewById(R.id.infectionStatusView);
        Button infectionButton = findViewById(R.id.infectionStatusButton);
        CharSequence DEFAULT_INFECTION_TEXT = getResources().getString(R.string.your_user_status_is_set_to_not_infected);
        CharSequence DEFAULT_INFECTION_BUTTON = getResources().getString(R.string.i_am_infected);
        infectionText.setText(savedInstanceState
                .getCharSequence("INFECTION_STATUS_TEXT", DEFAULT_INFECTION_TEXT));
        infectionButton.setText(savedInstanceState
                .getCharSequence("INFECTION_STATUS_BUTTON", DEFAULT_INFECTION_BUTTON));
    }
}
