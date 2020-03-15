package ch.epfl.sdp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class UserInfectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_infection);

        TextView infectionStatusView = findViewById(R.id.infectionStatusView);
        Button infectionStatusButton = findViewById(R.id.infectionStatusButton);

        infectionStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.infectionStatusButton) {
                    String buttonText = ((Button)v).getText().toString();
                    if (buttonText.equals("I am infected")) {
                        infectionStatusButton.setText("I am cured");
                        infectionStatusButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorGreenCured));
                        infectionStatusView.setText("Your user status is set to infected.");
                    }
                    else if (buttonText.equals("I am cured")) {
                        infectionStatusButton.setText("I am infected");
                        infectionStatusButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorRedInfected));
                        infectionStatusView.setText("Your user status is set to not infected.");
                    }
                }
            }
        });
    }
}
