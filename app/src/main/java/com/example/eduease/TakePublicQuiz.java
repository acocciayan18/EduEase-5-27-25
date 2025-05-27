package com.example.eduease;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Build;
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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TakePublicQuiz extends AppCompatActivity {

    private LinearLayout qaContainer;
    private FirebaseApp secondaryApp;
    private List<Map<String, String>> questionsList;
    private String quizId, typeQuiz;

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
        quizId = getIntent().getStringExtra("quizId");
        typeQuiz = getIntent().getStringExtra("typeQuiz");

        if (quizId != null && typeQuiz != null) {
            loadFromRealtimeDatabase(typeQuiz, quizId);
        } else {
            Toast.makeText(this, "Quiz ID or Type not provided.", Toast.LENGTH_SHORT).show();
            finish();
        }

        Button submitButton = findViewById(R.id.submit_btn);
        submitButton.setOnClickListener(v -> handleSubmit());
    }

    private void loadFromRealtimeDatabase(String typeQuiz, String quizId) {
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
        db.getReference("public_quizzes").child(quizId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String title = snapshot.child("title").getValue(String.class);
                            if (title != null) {
                                TextView quizTitle = findViewById(R.id.quiz_title);
                                quizTitle.setText(title);
                            }

                            qaContainer.removeAllViews();
                            questionsList = new ArrayList<>();
                            int i = 1;
                            while (true) {
                                DataSnapshot questionSnap = snapshot.child("Question_" + i);
                                if (!questionSnap.exists()) break;

                                String question = questionSnap.child("question").getValue(String.class);
                                String answer = questionSnap.child("answer").getValue(String.class);
                                if (question != null && answer != null) {
                                    Map<String, String> qa = Map.of("question", question, "answer", answer);
                                    questionsList.add(qa);
                                }
                                i++;
                            }

                            Collections.shuffle(questionsList);
                            int number = 1;
                            for (Map<String, String> qa : questionsList) {
                                addQuestionBlock(number++, qa.get("question"));
                            }

                        } else {
                            Toast.makeText(TakePublicQuiz.this, "Quiz not found in database.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(TakePublicQuiz.this, "DB Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
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

        answerField.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !answerField.getText().toString().trim().isEmpty()) {
                moveBlockToBottom(qaBlock);
            }
        });

        qaContainer.addView(qaBlock);
    }

    private void moveBlockToBottom(View qaBlock) {
        qaContainer.removeView(qaBlock);
        qaContainer.addView(qaBlock);
        qaContainer.post(() -> qaContainer.getChildAt(0).requestFocus());
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

    @SuppressLint("SetTextI18n")
    private void calculateScore() {
        int score = 0;
        List<String> userAnswers = new ArrayList<>();

        for (int i = 0; i < qaContainer.getChildCount(); i++) {
            View child = qaContainer.getChildAt(i);
            EditText answerField = child.findViewById(R.id.answer_field);
            userAnswers.add(answerField.getText().toString().trim());
        }

        qaContainer.removeAllViews(); // clear previous input blocks
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < questionsList.size(); i++) {
            Map<String, String> questionData = questionsList.get(i);
            String questionText = questionData.get("question");
            String correctAnswer = questionData.get("answer").trim();
            String userAnswer = i < userAnswers.size() ? userAnswers.get(i).trim() : "";

            if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                score++;
            }

            // Question TextView
            TextView questionView = new TextView(this);
            questionView.setText("Q: " + questionText);
            questionView.setTextColor(getResources().getColor(R.color.white));
            questionView.setTextSize(15);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                questionView.setTypeface(getResources().getFont(R.font.poppinsmedium));
            }

            // User Answer TextView
            TextView userAnswerView = new TextView(this);
            userAnswerView.setText("Your Answer: " + (userAnswer.isEmpty() ? "No Answer" : userAnswer));
            userAnswerView.setTextColor(getResources().getColor(R.color.white));
            userAnswerView.setTextSize(14);
            userAnswerView.setPadding(0, 4, 0, 0);

            // Correct Answer TextView
            TextView correctAnswerView = new TextView(this);
            correctAnswerView.setText("Correct Answer: " + correctAnswer);
            correctAnswerView.setTextColor(getResources().getColor(R.color.white));
            correctAnswerView.setTextSize(14);
            correctAnswerView.setPadding(0, 0, 0, 8);

            // Add all three views to the container
            qaContainer.addView(questionView);
            qaContainer.addView(userAnswerView);
            qaContainer.addView(correctAnswerView);
        }

        int totalQuestions = questionsList.size();
        float rawPercentage = (score / (float) totalQuestions) * 100;
        float roundedPercentage = rawPercentage >= 75 ? (float) Math.floor(rawPercentage) : (float) Math.ceil(rawPercentage);

        TextView resultMessage = findViewById(R.id.result_message);
        TextView scoreMessage = findViewById(R.id.score_message);

        resultMessage.setText("QUIZ COMPLETED!");
        scoreMessage.setText("Your Score: " + score + "/" + totalQuestions + " (" + (int) roundedPercentage + "%)");

        int soundResId = roundedPercentage >= 75 ? R.raw.pass : R.raw.fail;
        MediaPlayer mediaPlayer = MediaPlayer.create(this, soundResId);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);

        findViewById(R.id.finish_button).setVisibility(View.VISIBLE);
    }




}
