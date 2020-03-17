package ch.epfl.sdp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

public class UserInfectionActivity extends AppCompatActivity {

    private Button infectionStatusButton;
    private TextView infectionStatusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_infection);

        infectionStatusView = findViewById(R.id.infectionStatusView);
        infectionStatusButton = findViewById(R.id.infectionStatusButton);
        infectionStatusView.setSaveEnabled(true);
        infectionStatusButton.setSaveEnabled(true);

        infectionStatusButton.setOnClickListener(v -> {
            CharSequence buttonText = ((Button)v).getText();
            if (buttonText.equals(getResources().getString(R.string.i_am_infected))) {
                infectionStatusButton.setText(R.string.i_am_cured);
                infectionStatusButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorGreenCured));
                infectionStatusView.setText(R.string.your_user_status_is_set_to_infected);
            }
            else if (buttonText.equals(getResources().getString(R.string.i_am_cured))) {
                infectionStatusButton.setText(R.string.i_am_infected);
                infectionStatusButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorRedInfected));
                infectionStatusView.setText(R.string.your_user_status_is_set_to_not_infected);
            }
        });
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
        infectionText.setText(savedInstanceState.getCharSequence("INFECTION_STATUS_TEXT", DEFAULT_INFECTION_TEXT));
        infectionButton.setText(savedInstanceState.getCharSequence("INFECTION_STATUS_BUTTON", DEFAULT_INFECTION_BUTTON));
    }
}
