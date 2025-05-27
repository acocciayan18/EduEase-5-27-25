package com.example.eduease;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class RandomQuizResult extends AppCompatActivity {

    private TextView resultMessage;
    private TextView scoreMessage;
    private Button finishButton;
    private ConfettiView confettiView; // Reference to the custom ConfettiView class

//    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ayan_activity_random_quiz_result);

        // Initialize ConfettiView, TextViews, and Button
        confettiView = findViewById(R.id.confettiView);
        resultMessage = findViewById(R.id.result_message);
        scoreMessage = findViewById(R.id.score_message);
        finishButton = findViewById(R.id.finish_button);

        int score = getIntent().getIntExtra("score", 0);
        scoreMessage.setText("Your score: " + score + "/15");

        finishButton.setOnClickListener(v -> finish());

//         Start confetti animation
        confettiView.startConfetti();
    }
}
