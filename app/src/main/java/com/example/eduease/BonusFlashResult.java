package com.example.eduease;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class BonusFlashResult extends AppCompatActivity {

    private TextView resultMessageTextView;
    private TextView congratsTextView;
    private TextView scoreMessageTextView;
    private ConfettiView confettiView;


//    FIX ANG LOADING DHIL KAPAG NAG LOKO ANG BUTTON NAG EEXIT AT NAKA LOOP NA ANG LOADDING, OR FIX ANG BUTTON PARA DI MAG BACK



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bonus_flash_result);

        AppCompatButton finishButton = findViewById(R.id.finish_button);
        finishButton.setOnClickListener(v -> finish());


        resultMessageTextView = findViewById(R.id.result_message);
        congratsTextView = findViewById(R.id.eme);
        scoreMessageTextView = findViewById(R.id.score_message);
        confettiView = findViewById(R.id.confettiView);

        // Get data from intent
        Intent intent = getIntent();
        int totalBonusPoints = intent.getIntExtra("totalBonusPoints", 0);

        // Set score message
        scoreMessageTextView.setText("Total Bonus Points: " + totalBonusPoints);

        // Optional: Start confetti animation
        confettiView.startConfetti();

    }

}
