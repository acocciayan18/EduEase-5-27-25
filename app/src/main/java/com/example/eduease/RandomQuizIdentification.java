package com.example.eduease;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;

import java.util.*;

public class RandomQuizIdentification extends BaseActivity {

    private TextView questionText;
    private EditText answerInput;
    private Button submitButton;

    private List<IdentificationQuestion> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int score = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ayan_activity_random_quiz_identification); // Link this to your provided XML

        questionText = findViewById(R.id.identification_question);
        answerInput = findViewById(R.id.identification_answer);
        submitButton = findViewById(R.id.submit_identification_button);

        loadIdentificationQuestions();
        RecyclerView quizzesRecyclerView = findViewById(R.id.quizzes_recycler_view);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2); // 2 columns
        quizzesRecyclerView.setLayoutManager(gridLayoutManager);


        submitButton.setOnClickListener(v -> {
            submitButton.setEnabled(false); // Disable to prevent multiple clicks

            if (currentQuestionIndex >= questionList.size()) {
                showResult();
                return;
            }

            String userAnswer = answerInput.getText().toString().trim();
            if (userAnswer.isEmpty()) {
                Toast.makeText(this, "Please enter your answer", Toast.LENGTH_SHORT).show();
                submitButton.setEnabled(true); // Re-enable on invalid input
                return;
            }

            String correctAnswer = questionList.get(currentQuestionIndex).getAnswer().trim();

            if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
                score++;
            } else {
                Toast.makeText(this, "Incorrect. Correct answer: " + correctAnswer, Toast.LENGTH_LONG).show();
            }

            currentQuestionIndex++;
            answerInput.setText("");

            if (currentQuestionIndex < questionList.size()) {
                displayQuestion(questionList.get(currentQuestionIndex));
                submitButton.setEnabled(true); // Re-enable for next question
            } else {
                showResult(); // Quiz finished
            }
        });

    }

    private void loadIdentificationQuestions() {
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

        String path = "random_quiz_identification_" + topicTitle;
        DatabaseReference ref = db.getReference(path);


        ref.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<IdentificationQuestion> allQuestions = new ArrayList<>();
                for (DataSnapshot snap : task.getResult().getChildren()) {
                    IdentificationQuestion q = snap.getValue(IdentificationQuestion.class);
                    allQuestions.add(q);
                }

                hideLoading();

                if (!allQuestions.isEmpty()) {
                    Collections.shuffle(allQuestions); // Shuffle for randomness
                    questionList = allQuestions.subList(0, Math.min(15, allQuestions.size()));
                    displayQuestion(questionList.get(currentQuestionIndex));
                } else {
                    questionText.setText("No questions available.");
                }
            } else {
                questionText.setText("Failed to load questions.");
            }
        });
    }

    private void displayQuestion(IdentificationQuestion q) {
        questionText.setText(q.getQuestion());
    }

    private void showResult() {
        Intent intent = new Intent(RandomQuizIdentification.this, RandomQuizResult.class);
        intent.putExtra("score", score);
        startActivity(intent);
        finish();
    }


    
    // Make sure this class matches your Firebase structure
    public static class IdentificationQuestion {
        private String question;
        private String answer;

        public IdentificationQuestion() {} // Needed for Firebase

        public String getQuestion() { return question; }

        public String getAnswer() { return answer; }
    }
}
