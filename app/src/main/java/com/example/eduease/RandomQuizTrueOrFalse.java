package com.example.eduease;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RandomQuizTrueOrFalse extends BaseActivity {

    private TextView questionText;
    private RadioGroup choicesGroup;
    private Button submitButton;

    private String correctAnswer = "";
    private List<TrueOrFalseQuestion> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;

    private int score = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ayan_activity_random_quiz_true_or_false);

        questionText = findViewById(R.id.question_text);
        choicesGroup = findViewById(R.id.choices_group);
        submitButton = findViewById(R.id.submit_button);

        // Load questions from Firebase
        loadQuestionsFromFirebase();
        RecyclerView quizzesRecyclerView = findViewById(R.id.quizzes_recycler_view);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2); // 2 columns
        quizzesRecyclerView.setLayoutManager(gridLayoutManager);


        submitButton.setOnClickListener(v -> {
            // Disable button to prevent multiple clicks
            submitButton.setEnabled(false);

            int selectedId = choicesGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show();
                submitButton.setEnabled(true); // Re-enable on early return
                return;
            }

            if (currentQuestionIndex >= questionList.size()) {
                // Safety check: all questions answered
                showResult();
                return;
            }

            RadioButton selectedRadio = findViewById(selectedId);
            String selectedAnswer = selectedRadio.getText().toString().toLowerCase();

            if (selectedAnswer.equalsIgnoreCase(correctAnswer)) {
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
                score++;
            } else {
                Toast.makeText(this, "Incorrect. Correct answer: " + correctAnswer, Toast.LENGTH_LONG).show();
            }

            currentQuestionIndex++;
            if (currentQuestionIndex < questionList.size()) {
                displayQuestion(questionList.get(currentQuestionIndex));
                submitButton.setEnabled(true); // Re-enable for next question
            } else {
                showResult(); // Quiz finished
            }
        });

    }

    private void loadQuestionsFromFirebase() {

        showLoading();
        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.getInstance("Secondary");
        } catch (IllegalStateException e) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:882141634417:android:ac69b51d83d01def3460d0")
                    .setApiKey("AIzaSyBlECTZf28SbEc4xHsz7JnH99YtTw6T58I")
                    .setProjectId("edu-ease-ni-ayan")
                    .setDatabaseUrl("https://edu-ease-ni-ayan-default-rtdb.firebaseio.com/")
                    .build();

            secondaryApp = FirebaseApp.initializeApp(getApplicationContext(), options, "Secondary");
        }

        FirebaseDatabase db = FirebaseDatabase.getInstance(secondaryApp);

        String topicTitle = getIntent().getStringExtra("topicTitle").toLowerCase();

        String path = "random_quiz_true_false_" + topicTitle;
        DatabaseReference ref = db.getReference(path);


        ref.limitToFirst(15).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DataSnapshot snap : task.getResult().getChildren()) {
                    TrueOrFalseQuestion q = snap.getValue(TrueOrFalseQuestion.class);
                    questionList.add(q);
                }

                hideLoading();

                if (!questionList.isEmpty()) {
                    // Shuffle the question list to get random questions
                    Collections.shuffle(questionList);

                    displayQuestion(questionList.get(currentQuestionIndex));
                } else {
                    questionText.setText("No questions available.");
                }
            } else {
                questionText.setText("Failed to fetch from Firebase.");
            }
        });
    }

    private void displayQuestion(TrueOrFalseQuestion q) {
        questionText.setText(q.getQuestion());
        correctAnswer = q.getAnswer().toLowerCase();

        choicesGroup.removeAllViews();

        // Convert 200dp to pixels
        int widthInPx = (int) (200 * getResources().getDisplayMetrics().density);

        // Create true RadioButton with white background and green border on selection
        RadioButton rbTrue = new RadioButton(this);
        rbTrue.setText("True");
        rbTrue.setBackgroundResource(R.drawable.quiz_choice_selector);  // Apply custom background
        rbTrue.setTypeface(ResourcesCompat.getFont(this, R.font.poppinsregular));
        rbTrue.setTextColor(Color.parseColor("#000000")); // Set text color to black
        rbTrue.setTextSize(16);
        rbTrue.setPadding(24, 24, 24, 24);  // Padding for better appearance

        // Set layout parameters: width = 200dp, height = wrap_content, margin = 16dp
        LinearLayout.LayoutParams rbTrueLayoutParams = new LinearLayout.LayoutParams(widthInPx, LinearLayout.LayoutParams.WRAP_CONTENT);
        rbTrueLayoutParams.setMargins(0, 16, 0, 16); // Set margin to 16dp (convert dp to pixels)
        rbTrue.setLayoutParams(rbTrueLayoutParams);

        choicesGroup.addView(rbTrue);

        // Create false RadioButton with white background and green border on selection
        RadioButton rbFalse = new RadioButton(this);
        rbFalse.setText("False");
        rbFalse.setBackgroundResource(R.drawable.quiz_choice_selector);  // Apply custom background
        rbFalse.setTypeface(ResourcesCompat.getFont(this, R.font.poppinsregular));
        rbFalse.setTextColor(Color.parseColor("#000000")); // Set text color to black
        rbFalse.setTextSize(16);
        rbFalse.setPadding(24, 24, 24, 24);  // Padding for better appearance

        // Set layout parameters: width = 200dp, height = wrap_content, margin = 16dp
        LinearLayout.LayoutParams rbFalseLayoutParams = new LinearLayout.LayoutParams(widthInPx, LinearLayout.LayoutParams.WRAP_CONTENT);
        rbFalseLayoutParams.setMargins(0, 16, 0, 16); // Set margin to 16dp (convert dp to pixels)
        rbFalse.setLayoutParams(rbFalseLayoutParams);

        choicesGroup.addView(rbFalse);
    }





    private void showResult() {
        Intent intent = new Intent(RandomQuizTrueOrFalse.this, RandomQuizResult.class);
        intent.putExtra("score", score);  // Pass the score to the result screen
        startActivity(intent);
        finish();  // Close the current quiz activity
    }
}
