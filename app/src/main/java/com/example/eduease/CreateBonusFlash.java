package com.example.eduease;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.util.HashMap;
import java.util.Map;

public class CreateBonusFlash extends BaseActivity {

    private EditText bonusQuizTitle, bonusQuizDescription;
    private LinearLayout bonusQaContainer;
    private Vibrator vibrator;
    private DatabaseReference secondaryDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ayan_activity_create_bonus_flash);

        bonusQuizTitle = findViewById(R.id.flashquiz_input_title);
        bonusQuizDescription = findViewById(R.id.flashquiz_input_description);
        bonusQaContainer = findViewById(R.id.flashquiz_qa_block_container);
        MaterialButton saveBtn = findViewById(R.id.flashquiz_button_submit);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:882141634417:android:ac69b51d83d01def3460d0")
                    .setApiKey("AIzaSyBlECTZf28SbEc4xHsz7JnH99YtTw6T58I")
                    .setProjectId("edu-ease-ni-ayan")
                    .setDatabaseUrl("https://edu-ease-ni-ayan-default-rtdb.firebaseio.com/")
                    .build();

            FirebaseApp secondaryApp = FirebaseApp.initializeApp(getApplicationContext(), options, "secondary");
            secondaryDb = FirebaseDatabase.getInstance(secondaryApp).getReference("bonus_quizzes");
            Log.d("CreateBonusFlash", "Secondary Firebase initialized with Realtime Database");
        } catch (IllegalStateException e) {
            Log.e("CreateBonusFlash", "Firebase initialization error: " + e.getMessage());
        }

        saveBtn.setOnClickListener(v -> {
            Log.d("CreateBonusFlash", "Save button clicked");
            saveQuiz();
        });

        for (int i = 0; i < 3; i++) {
            addBonusQABlock();
        }
    }

    private void saveQuiz() {
        String title = bonusQuizTitle.getText().toString().trim();
        String desc = bonusQuizDescription.getText().toString().trim();

        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Title and Description can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

       showLoading();

        Map<String, Object> quizData = new HashMap<>();
        quizData.put("title", title);
        quizData.put("description", desc);
        quizData.put("creatorId", user.getUid());
        quizData.put("flash", true);
        quizData.put("type", "public");

        int validQACount = 0;

        for (int i = 0; i < bonusQaContainer.getChildCount(); i++) {
            View qaView = bonusQaContainer.getChildAt(i);
            EditText questionField = qaView.findViewById(R.id.bonus_question_field);
            EditText answerField = qaView.findViewById(R.id.bonus_answer_field);
            EditText bonusField = qaView.findViewById(R.id.bonus_points_field);

            String question = questionField.getText().toString().trim();
            String answer = answerField.getText().toString().trim();
            String pointsStr = bonusField.getText().toString().trim();

            if (question.isEmpty() || answer.isEmpty() || pointsStr.isEmpty()) {
hideLoading();
                Toast.makeText(this, "All questions must have question, answer, and bonus points", Toast.LENGTH_SHORT).show();
                return;
            }

            int points;
            try {
                points = Integer.parseInt(pointsStr);
                if (points < 0 || points > 99) {
hideLoading();
                    Toast.makeText(this, "Bonus points must be between 0 and 99", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
hideLoading();
                Toast.makeText(this, "Invalid bonus points input", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> qa = new HashMap<>();
            qa.put("question", question);
            qa.put("answer", answer);
            qa.put("bonusPoints", points);
            quizData.put("BonusQA" + (validQACount + 1), qa);
            validQACount++;
        }

        if (validQACount < 3) {
    hideLoading();
            Toast.makeText(this, "At least 3 complete question-answer-bonus sets are required", Toast.LENGTH_SHORT).show();
            return;
        }

        secondaryDb.push().setValue(quizData)
                .addOnSuccessListener(aVoid -> {
                    hideLoading();
//                    Toast.makeText(this, "Bonus quiz saved to secondary DB!", Toast.LENGTH_SHORT).show();
                    // Delay a bit before exiting so user sees the toast
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(this, Home.class));
                        finish();
                    }, 800); // Optional: 800ms delay
                })
                .addOnFailureListener(e -> {
                   hideLoading();
                    Toast.makeText(this, "Error saving quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    private void addBonusQABlock() {
        View qaBlock = getLayoutInflater().inflate(R.layout.bonus_qa_block, bonusQaContainer, false);


        ImageButton addBtn = qaBlock.findViewById(R.id.add_button);
        ImageButton deleteBtn = qaBlock.findViewById(R.id.delete_button);



        addBtn.setOnClickListener(v -> {
            vibrate();
            addBonusQABlock();
        });

        deleteBtn.setOnClickListener(v -> {
            vibrate();
            bonusQaContainer.removeView(qaBlock);
            updateDeleteButtons();
        });

        bonusQaContainer.addView(qaBlock);
        updateDeleteButtons();
    }


    @SuppressLint("NotifyDataSetChanged")
    private void updateDeleteButtons() {
        int count = bonusQaContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View qaView = bonusQaContainer.getChildAt(i);
            ImageButton deleteBtn = qaView.findViewById(R.id.delete_button);
            if (deleteBtn != null) {
                deleteBtn.setEnabled(count > 3);
            }

            deleteBtn.setEnabled(count > 3); // Prevent deleting if only 3 remain
        }
    }

    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }
}
