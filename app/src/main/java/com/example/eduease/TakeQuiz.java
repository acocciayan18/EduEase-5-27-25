package com.example.eduease;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import java.util.List;
import java.util.Map;

public class TakeQuiz extends BaseActivity {

    private LinearLayout qaContainer;
    private List<Map<String, Object>> questionsList;

    private String quizId;

    private String title;
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

        quizId = getIntent().getStringExtra("QUIZ_ID");
        if (quizId != null) {
            questionsList = new ArrayList<>();
            loadQuizDataFromRealtimeDB(quizId);

        } else {
            Toast.makeText(this, "Quiz ID not provided.", Toast.LENGTH_SHORT).show();
            hideLoading();
            finish();
        }

        TextView titleTextView = findViewById(R.id.quiz_title);
        String title = getIntent().getStringExtra("QUIZ_TITLE"); // use "quizTitle", not "QUIZ_TITLE"
        if (title != null) {
            titleTextView.setText(title);
        }


        Button submitButton = findViewById(R.id.submit_btn);
        submitButton.setOnClickListener(v -> handleSubmit());
    }

    private void loadQuizDataFromRealtimeDB(String quizId) {
        DatabaseReference quizRef = FirebaseDatabase.getInstance().getReference("local_quizzes").child(quizId);
        quizRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int questionNumber = 1;

                DataSnapshot mcSnapshot = snapshot.child("multiple_choice");
                for (DataSnapshot item : mcSnapshot.getChildren()) {
                    Object raw = item.getValue();
                    if (raw instanceof Map) {
                        addQuestionBlock(questionNumber++, (Map<String, Object>) raw, "multiple_choice");
                    }
                }

                DataSnapshot idSnapshot = snapshot.child("identification");
                for (DataSnapshot item : idSnapshot.getChildren()) {
                    Object raw = item.getValue();
                    if (raw instanceof Map) {
                        addQuestionBlock(questionNumber++, (Map<String, Object>) raw, "identification");
                    }
                }

                DataSnapshot tfSnapshot = snapshot.child("true_or_false");
                for (DataSnapshot item : tfSnapshot.getChildren()) {
                    Object raw = item.getValue();
                    if (raw instanceof Map) {
                        addQuestionBlock(questionNumber++, (Map<String, Object>) raw, "true_or_false");
                    }
                }

                // ✅ Hide loading once done
                hideLoading();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TakeQuiz.this, "Failed to load quiz.", Toast.LENGTH_SHORT).show();
                hideLoading(); // Also hide on error
            }
        });
    }



    private void addQuestionBlock(int questionNumber, Map<String, Object> questionData, String type) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View qaBlock = inflater.inflate(R.layout.ayan_take_quiz_edittext, qaContainer, false);

        TextView questionNumberView = qaBlock.findViewById(R.id.question_number);
        EditText questionField = qaBlock.findViewById(R.id.question_field);
        EditText answerField = qaBlock.findViewById(R.id.answer_field);
        RadioGroup choicesGroup = qaBlock.findViewById(R.id.choices_group);
        RadioButton choiceA = qaBlock.findViewById(R.id.choice_a);
        RadioButton choiceB = qaBlock.findViewById(R.id.choice_b);
        RadioButton choiceC = qaBlock.findViewById(R.id.choice_c);
        RadioButton choiceD = qaBlock.findViewById(R.id.choice_d);

        questionNumberView.setText("#" + questionNumber + " Question:");
        questionField.setText((String) questionData.get("question"));

        switch (type) {
            case "multiple_choice":
                answerField.setVisibility(View.GONE);
                choicesGroup.setVisibility(View.VISIBLE);

                choiceA.setVisibility(View.VISIBLE);
                choiceB.setVisibility(View.VISIBLE);
                choiceC.setVisibility(View.VISIBLE);
                choiceD.setVisibility(View.VISIBLE);

                List<String> choices = (List<String>) questionData.get("choices");

                choiceA.setVisibility(View.GONE);
                choiceB.setVisibility(View.GONE);
                choiceC.setVisibility(View.GONE);
                choiceD.setVisibility(View.GONE);

                if (choices != null) {
                    int size = Math.min(choices.size(), 4);
                    if (size > 0) {
                        choiceA.setText(choices.get(0));
                        choiceA.setVisibility(View.VISIBLE);
                    }
                    if (size > 1) {
                        choiceB.setText(choices.get(1));
                        choiceB.setVisibility(View.VISIBLE);
                    }
                    if (size > 2) {
                        choiceC.setText(choices.get(2));
                        choiceC.setVisibility(View.VISIBLE);
                    }
                    if (size > 3) {
                        choiceD.setText(choices.get(3));
                        choiceD.setVisibility(View.VISIBLE);
                    }
                }

                break;



            case "true_or_false":
                answerField.setVisibility(View.GONE);
                choicesGroup.setVisibility(View.VISIBLE);

                choiceA.setText("True");
                choiceB.setText("False");

                choiceC.setVisibility(View.GONE);
                choiceD.setVisibility(View.GONE);
                break;


            case "identification":
                answerField.setVisibility(View.VISIBLE);
                choicesGroup.setVisibility(View.GONE);
                break;
        }

        questionData.put("type", type); // Store the type
        questionsList.add(questionData);
        // Add to list for scoring

        qaBlock.setTag(questionData);
        qaContainer.addView(qaBlock);
    }


    private void handleSubmit() {

        showLoading();
        boolean allAnswered = true;

        for (int i = 0; i < qaContainer.getChildCount(); i++) {
            View child = qaContainer.getChildAt(i);
            EditText answerField = child.findViewById(R.id.answer_field);
            if (answerField.getVisibility() == View.VISIBLE && answerField.getText().toString().trim().isEmpty()) {
                allAnswered = false;

                hideLoading();
                break;
            }

            RadioGroup choicesGroup = child.findViewById(R.id.choices_group);
            if (choicesGroup.getVisibility() == View.VISIBLE && choicesGroup.getCheckedRadioButtonId() == -1) {
                allAnswered = false;
                hideLoading();
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
            Object tag = child.getTag();

            // Ensure tag is a valid map
            if (!(tag instanceof Map)) continue;

            Map<String, Object> questionData = (Map<String, Object>) tag;
            String type = (String) questionData.get("type");
            String userAnswer = "";

            RadioGroup choicesGroup = child.findViewById(R.id.choices_group);
            EditText answerField = child.findViewById(R.id.answer_field);

            if ("multiple_choice".equals(type) || "true_or_false".equals(type)) {
                int selectedId = choicesGroup.getCheckedRadioButtonId();
                if (selectedId != -1) {
                    RadioButton selected = child.findViewById(selectedId);
                    if (selected != null) {
                        userAnswer = selected.getText().toString().trim();
                    }
                }
            } else if ("identification".equals(type)) {
                if (answerField.getVisibility() == View.VISIBLE) {
                    userAnswer = answerField.getText().toString().trim();
                }
            }

            userAnswers.add(userAnswer);
        }

        QuizDataHolder.setQuestionsList(questionsList);
        QuizDataHolder.setUserAnswers(userAnswers);

        hideLoading();

        Intent intent = new Intent(TakeQuiz.this, QuizResult.class);
        startActivity(intent);
        finish();
    }

}
