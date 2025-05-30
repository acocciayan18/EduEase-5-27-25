package com.example.eduease;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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
import java.util.List;
import java.util.Map;

public class TakePublicQuiz extends BaseActivity {

    private LinearLayout qaContainer;
    private FirebaseApp secondaryApp;
    private List<Map<String, Object>> questionsList = new ArrayList<>();
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


        FirebaseDatabase db = FirebaseDatabase.getInstance();
        db.getReference("public_quizzes").child(quizId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(TakePublicQuiz.this, "Quiz not found in database.", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        // Set the title
                        String title = snapshot.child("title").getValue(String.class);
                        if (title != null) {
                            TextView quizTitle = findViewById(R.id.quiz_title);
                            quizTitle.setText(title);
                        }

                        qaContainer.removeAllViews();
                        int questionNumber = 1;

                        // Load multiple choice questions
                        DataSnapshot mcSnapshot = snapshot.child("multiple_choice");
                        for (DataSnapshot item : mcSnapshot.getChildren()) {
                            Object raw = item.getValue();
                            if (raw instanceof Map) {
                                addQuestionBlock(questionNumber++, (Map<String, Object>) raw, "multiple_choice");
                            }
                        }

                        // Load identification questions
                        DataSnapshot idSnapshot = snapshot.child("identification");
                        for (DataSnapshot item : idSnapshot.getChildren()) {
                            Object raw = item.getValue();
                            if (raw instanceof Map) {
                                addQuestionBlock(questionNumber++, (Map<String, Object>) raw, "identification");
                            }
                        }

                        // Load true/false questions
                        DataSnapshot tfSnapshot = snapshot.child("true_or_false");
                        for (DataSnapshot item : tfSnapshot.getChildren()) {
                            Object raw = item.getValue();
                            if (raw instanceof Map) {
                                addQuestionBlock(questionNumber++, (Map<String, Object>) raw, "true_or_false");
                            }
                        }

                        hideLoading(); // Hide loader once done
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(TakePublicQuiz.this, "DB Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        hideLoading(); // Also hide on error
                        finish();
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
        questionField.setFocusable(false);
        questionField.setClickable(false);
        questionField.setFocusableInTouchMode(false);
        questionField.setBackground(null);
        questionField.setTextIsSelectable(false);

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

                answerField.setHint("Enter your answer here");
                answerField.setOnFocusChangeListener((v, hasFocus) -> {
                    if (!hasFocus && !answerField.getText().toString().trim().isEmpty()) {
                        moveBlockToBottom(qaBlock);
                    }
                });
                break;
        }

        questionData.put("type", type); // Store the type
        questionsList.add(questionData); // For result processing

        qaBlock.setTag(questionData); // Tagging for lookup
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
            Object tag = child.getTag();

            if (!(tag instanceof Map)) continue;
            Map<String, Object> questionData = (Map<String, Object>) tag;
            String type = (String) questionData.get("type");

            EditText answerField = child.findViewById(R.id.answer_field);
            RadioGroup choicesGroup = child.findViewById(R.id.choices_group);

            if ("identification".equals(type)) {
                if (answerField.getText().toString().trim().isEmpty()) {
                    allAnswered = false;
                    break;
                }
            } else if ("multiple_choice".equals(type) || "true_or_false".equals(type)) {
                if (choicesGroup.getCheckedRadioButtonId() == -1) {
                    allAnswered = false;
                    break;
                }
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

        Intent intent = new Intent(TakePublicQuiz.this, QuizResult.class);
        startActivity(intent);
        finish();
    }





}
