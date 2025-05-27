package com.example.eduease;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TakeQuiz extends BaseActivity {

    private LinearLayout qaContainer;
    private List<Map<String, String>> questionsList;
    private String quizId;
    private DatabaseReference localQuizzesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_take_quiz);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        qaContainer = findViewById(R.id.qa_container);

        showLoading();

        // Initialize secondary Firebase app
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

        FirebaseDatabase secondaryDatabase = FirebaseDatabase.getInstance(secondaryApp);
        localQuizzesRef = secondaryDatabase.getReference("local_quizzes");

        // Get quiz ID
        quizId = getIntent().getStringExtra("QUIZ_ID");
        if (quizId != null) {
            loadQuizDataFromRealtimeDB(quizId);
        } else {
            Toast.makeText(this, "Quiz ID not provided.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Submit button
        Button submitButton = findViewById(R.id.submit_btn);
        submitButton.setOnClickListener(v -> handleSubmit());
    }

    @SuppressLint("SetTextI18n")
    private void loadQuizDataFromRealtimeDB(String quizId) {
        localQuizzesRef.child(quizId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(TakeQuiz.this, "Quiz not found.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Title
                String title = snapshot.child("title").getValue(String.class);
                if (title != null) {
                    TextView quizTitle = findViewById(R.id.quiz_title);
                    quizTitle.setText(title);
                }

                qaContainer.removeAllViews();
                questionsList = new ArrayList<>();

                int i = 1;
                while (snapshot.hasChild("Question " + i)) {
                    DataSnapshot questionSnap = snapshot.child("Question " + i);
                    Map<String, String> qa = (Map<String, String>) questionSnap.getValue();
                    if (qa != null) {
                        questionsList.add(qa);
                    }
                    i++;
                }

                Collections.shuffle(questionsList);

                int questionNumber = 1;
                for (Map<String, String> qa : questionsList) {
                    addQuestionBlock(questionNumber++, qa.get("question"));
                }

                hideLoading();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                hideLoading();
                Toast.makeText(TakeQuiz.this, "Error loading quiz: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addQuestionBlock(int questionNumber, String question) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View qaBlock = inflater.inflate(R.layout.ayan_take_quiz_edittext, qaContainer, false);

        TextView questionNumberView = qaBlock.findViewById(R.id.question_number);
        EditText questionField = qaBlock.findViewById(R.id.question_field);
        EditText answerField = qaBlock.findViewById(R.id.answer_field);

        questionNumberView.setText("#" + questionNumber + " Question:");
        questionField.setText(question);
        questionField.setFocusable(false);
        questionField.setClickable(false);
        questionField.setFocusableInTouchMode(false);
        questionField.setBackground(null);
        questionField.setTextIsSelectable(false);

        answerField.setHint("Enter your answer here");
        qaBlock.setTag(questionNumber - 1);

        qaContainer.addView(qaBlock);
    }

    private void handleSubmit() {
        boolean allAnswered = true;

        for (int i = 0; i < qaContainer.getChildCount(); i++) {
            View child = qaContainer.getChildAt(i);
            EditText answerField = child.findViewById(R.id.answer_field);
            if (answerField.getText().toString().trim().isEmpty()) {
                allAnswered = false;
                break;
            }
        }

        if (!allAnswered) {
            new AlertDialog.Builder(this)
                    .setTitle("Unanswered Questions")
                    .setMessage("You have unanswered questions. Are you sure you want to submit?")
                    .setPositiveButton("Yes", (dialog, which) -> calculateScore())
                    .setNegativeButton("No", null)
                    .show();
        } else {
            calculateScore();
        }
    }

    private void calculateScore() {
        ArrayList<String> userAnswers = new ArrayList<>();

        for (int i = 0; i < qaContainer.getChildCount(); i++) {
            View child = qaContainer.getChildAt(i);
            EditText answerField = child.findViewById(R.id.answer_field);
            String userAnswer = answerField.getText().toString().trim();
            userAnswers.add(userAnswer);
        }

        QuizDataHolder.setQuestionsList(questionsList);
        QuizDataHolder.setUserAnswers(userAnswers);

        Intent intent = new Intent(TakeQuiz.this, QuizResult.class);
        startActivity(intent);
        finish();
    }
}
