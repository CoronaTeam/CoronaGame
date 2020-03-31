package ch.epfl.sdp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import static ch.epfl.sdp.MainActivity.IS_ONLINE;
import static ch.epfl.sdp.MainActivity.checkNetworkStatus;

public class UserInfectionActivity extends AppCompatActivity {

    private Button infectionStatusButton;
    private TextView infectionStatusView;
    private TextView infectionUploadView;
    private TextView userNameView;
    private TextView onlineStatusView;
    private Button refreshButton;
    private Account account;
    private User user;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_infection);

        infectionStatusView = findViewById(R.id.infectionStatusView);
        infectionStatusButton = findViewById(R.id.infectionStatusButton);
        infectionUploadView = findViewById(R.id.infectionStatusUploadConfirmation);
        userNameView = findViewById(R.id.userName);

        checkOnline();

        refreshButton.setOnClickListener(v -> checkOnline());

        getLoggedInUser();

        userNameView.setText(userName);

        infectionStatusButton.setOnClickListener(v -> statusButtonAction((Button)v));
    }

    private void setView(Button button, TextView textView, boolean infected) {
        int buttonText = infected ? R.string.i_am_cured : R.string.i_am_infected;
        int textViewText = infected ? R.string.your_user_status_is_set_to_infected : R.string.your_user_status_is_set_to_not_infected;
        int buttonColor = infected ? R.color.colorGreenCured : R.color.colorRedInfected;
        button.setText(buttonText);
        button.setBackgroundTintList(
                getResources().getColorStateList(buttonColor));
        textView.setText(textViewText);
    }

    private void checkOnline() {
        onlineStatusView = findViewById(R.id.onlineStatusView);
        refreshButton = findViewById(R.id.refreshButton);
        checkNetworkStatus(this);
        if (!IS_ONLINE) {
            onlineStatusView.setVisibility(View.VISIBLE);
            refreshButton.setVisibility(View.VISIBLE);
            infectionStatusButton.setVisibility(View.INVISIBLE);
            infectionStatusView.setVisibility(View.INVISIBLE);
            infectionUploadView.setVisibility(View.INVISIBLE);
            userNameView.setVisibility(View.INVISIBLE);
        } else {
            onlineStatusView.setVisibility(View.INVISIBLE);
            refreshButton.setVisibility(View.INVISIBLE);
            infectionStatusButton.setVisibility(View.VISIBLE);
            infectionStatusView.setVisibility(View.VISIBLE);
            infectionUploadView.setVisibility(View.VISIBLE);
            userNameView.setVisibility(View.VISIBLE);
        }
    }

    private void getLoggedInUser() {
        account = AccountGetting.getAccount(this);
        userName = account.getDisplayName();
        user = new User(userName, account.getFamilyName(), account.getEmail(),
                account.getPhotoUrl(), account.getPlayerId(this), account.getId(), User.DEFAULT_AGE, false);
        user.retrieveUserInfectionStatus(
                value -> setView(infectionStatusButton, infectionStatusView, value));
    }

    private void statusButtonAction(Button b) {
        checkOnline();
        CharSequence buttonText = b.getText();
        if (buttonText.equals(getResources().getString(R.string.i_am_infected))) {
            setView(infectionStatusButton, infectionStatusView, true);
            //upload to firebase
            user.modifyUserInfectionStatus(userName, true,
                    value -> infectionUploadView.setText(value));


        } else {
            setView(infectionStatusButton, infectionStatusView, false);
            //upload to firebase
            user.modifyUserInfectionStatus(userName, false,
                    value -> infectionUploadView.setText(value));
        }
    }
}
