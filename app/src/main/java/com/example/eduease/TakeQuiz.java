package com.example.eduease;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar; // Still needed for show/hideLoading
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects; // Added for Objects.requireNonNull

public class TakeQuiz extends BaseActivity {

    private LinearLayout qaContainer;
    private List<Map<String, Object>> questionsList;
    private String quizId;
    private TextView titleTextView;
    // Removed private String title; as it's not needed as a field

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
        titleTextView = findViewById(R.id.quiz_title); // Initialize titleTextView here

        showLoading();

        // FirebaseApp.initializeApp(this); // Generally done in Application class or once per app lifecycle
        // No need for secondaryDatabase if you're using the default instance
        // localQuizzesRef = FirebaseDatabase.getInstance().getReference("local_quizzes"); // Not directly used here, can be local

        quizId = getIntent().getStringExtra("QUIZ_ID");
        if (quizId != null && !quizId.isEmpty()) { // Added isEmpty check
            questionsList = new ArrayList<>();
            loadQuizDataFromRealtimeDB(quizId);
        } else {
            Toast.makeText(this, "Quiz ID not provided.", Toast.LENGTH_SHORT).show();
            hideLoading();
            finish();
        }

        Button submitButton = findViewById(R.id.submit_btn);
        submitButton.setOnClickListener(v -> handleSubmit());
    }

    /**
     * Loads quiz data (title and questions) from Firebase Realtime Database.
     * @param quizId The ID of the quiz to load.
     */
    private void loadQuizDataFromRealtimeDB(String quizId) {
        DatabaseReference quizRef = FirebaseDatabase.getInstance().getReference("local_quizzes").child(quizId);
        quizRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Fetch and set the quiz title
                String quizTitle = snapshot.child("title").getValue(String.class);
                if (quizTitle != null) {
                    titleTextView.setText(quizTitle);
                } else {
                    titleTextView.setText("Unknown Quiz"); // Default if title is missing
                }

                qaContainer.removeAllViews(); // Clear any existing views before populating
                questionsList.clear(); // Clear list for fresh data

                int questionNumber = 1;

                // Load Multiple Choice questions
                DataSnapshot mcSnapshot = snapshot.child("multiple_choice");
                for (DataSnapshot item : mcSnapshot.getChildren()) {
                    Object raw = item.getValue();
                    if (raw instanceof Map) {
                        Map<String, Object> questionMap = (Map<String, Object>) raw;
                        // Ensure 'choices' is treated as a List<String> or handle potential null
                        Object choicesObj = questionMap.get("choices");
                        if (choicesObj instanceof List) {
                            // Firebase often stores lists as ArrayList<Object>, so cast safely
                            List<String> choices = new ArrayList<>();
                            for (Object choiceItem : (List<?>) choicesObj) {
                                if (choiceItem instanceof String) {
                                    choices.add((String) choiceItem);
                                }
                            }
                            questionMap.put("choices", choices); // Update the map with correct type
                        } else {
                            questionMap.put("choices", new ArrayList<String>()); // Default to empty list
                        }
                        addQuestionBlock(questionNumber++, questionMap, "multiple_choice");
                    }
                }

                // Load Identification questions
                DataSnapshot idSnapshot = snapshot.child("identification");
                for (DataSnapshot item : idSnapshot.getChildren()) {
                    Object raw = item.getValue();
                    if (raw instanceof Map) {
                        addQuestionBlock(questionNumber++, (Map<String, Object>) raw, "identification");
                    }
                }

                // Load True or False questions
                DataSnapshot tfSnapshot = snapshot.child("true_or_false");
                for (DataSnapshot item : tfSnapshot.getChildren()) {
                    Object raw = item.getValue();
                    if (raw instanceof Map) {
                        addQuestionBlock(questionNumber++, (Map<String, Object>) raw, "true_or_false");
                    }
                }

                // Hide loading once all data is processed and UI is built
                hideLoading();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TakeQuiz.this, "Failed to load quiz: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                hideLoading(); // Also hide loading on error
            }
        });
    }

    /**
     * Dynamically adds a question block to the UI.
     * @param questionNumber The sequential number of the question.
     * @param questionData   A Map containing the question details (question, answer, choices, etc.).
     * @param type           The type of the question ("multiple_choice", "true_or_false", "identification").
     */
    @SuppressLint("SetTextI18n") // For "#" + questionNumber + " Question:"
    private void addQuestionBlock(int questionNumber, Map<String, Object> questionData, String type) {
        LayoutInflater inflater = LayoutInflater.from(this);
        // Using `qaContainer` as the root for inflation is correct as it's the parent where views will be added
        View qaBlock = inflater.inflate(R.layout.ayan_take_quiz_edittext, qaContainer, false);

        TextView questionNumberView = qaBlock.findViewById(R.id.question_number);
        TextView questionField = qaBlock.findViewById(R.id.question_field); // Changed to TextView as it's not editable
        EditText answerField = qaBlock.findViewById(R.id.answer_field);
        RadioGroup choicesGroup = qaBlock.findViewById(R.id.choices_group);
        RadioButton choiceA = qaBlock.findViewById(R.id.choice_a);
        RadioButton choiceB = qaBlock.findViewById(R.id.choice_b);
        RadioButton choiceC = qaBlock.findViewById(R.id.choice_c);
        RadioButton choiceD = qaBlock.findViewById(R.id.choice_d);

        questionNumberView.setText("#" + questionNumber + " Question:");
        // Use Objects.requireNonNull for clarity, assuming "question" key exists
        questionField.setText(Objects.requireNonNull((String) questionData.get("question")));

        // Set initial visibility for all choice RadioButtons to GONE
        choiceA.setVisibility(View.GONE);
        choiceB.setVisibility(View.GONE);
        choiceC.setVisibility(View.GONE);
        choiceD.setVisibility(View.GONE);

        switch (type) {
            case "multiple_choice":
                answerField.setVisibility(View.GONE);
                choicesGroup.setVisibility(View.VISIBLE);

                // Safely cast and use choices list
                List<String> choices = (List<String>) questionData.get("choices");

                if (choices != null) {
                    // Using an array of RadioButtons for cleaner iteration
                    RadioButton[] mcChoices = {choiceA, choiceB, choiceC, choiceD};
                    for (int i = 0; i < mcChoices.length; i++) {
                        if (i < choices.size()) {
                            mcChoices[i].setText(choices.get(i));
                            mcChoices[i].setVisibility(View.VISIBLE);
                        } else {
                            mcChoices[i].setVisibility(View.GONE); // Hide unused radio buttons
                        }
                    }
                }
                break;

            case "true_or_false":
                answerField.setVisibility(View.GONE);
                choicesGroup.setVisibility(View.VISIBLE);

                choiceA.setText("True");
                choiceB.setText("False");
                choiceA.setVisibility(View.VISIBLE); // Make True/False visible
                choiceB.setVisibility(View.VISIBLE); // Make True/False visible
                // C and D remain GONE as set initially
                break;

            case "identification":
                answerField.setVisibility(View.VISIBLE);
                choicesGroup.setVisibility(View.GONE);
                break;
        }

        // Store the type in the questionData map itself for easier retrieval during scoring
        // This is important because the 'type' string passed here isn't stored in 'questionsList' otherwise
        questionData.put("type", type);
        questionsList.add(questionData); // Add to list for scoring

        qaBlock.setTag(questionData); // Store the entire question data map as a tag for easy retrieval
        qaContainer.addView(qaBlock);
    }

    /**
     * Handles the quiz submission process, including validation and score calculation.
     */
    private void handleSubmit() {
        showLoading(); // Show loading while processing submission

        boolean allAnswered = true;
        for (int i = 0; i < qaContainer.getChildCount(); i++) {
            View child = qaContainer.getChildAt(i);
            Map<String, Object> questionData = (Map<String, Object>) child.getTag(); // Retrieve stored data
            String type = (String) questionData.get("type"); // Get type from stored data

            if ("identification".equals(type)) {
                EditText answerField = child.findViewById(R.id.answer_field);
                if (answerField.getText().toString().trim().isEmpty()) {
                    allAnswered = false;
                    break;
                }
            } else if ("multiple_choice".equals(type) || "true_or_false".equals(type)) {
                RadioGroup choicesGroup = child.findViewById(R.id.choices_group);
                if (choicesGroup.getCheckedRadioButtonId() == -1) {
                    allAnswered = false;
                    break;
                }
            }
        }

        if (!allAnswered) {
            hideLoading(); // Hide loading if not all answered and showing dialog
            new AlertDialog.Builder(this)
                    .setTitle("Unanswered Questions")
                    .setMessage("You have unanswered questions. Are you sure you want to submit?")
                    .setPositiveButton("Yes", (dialog, which) -> calculateScore())
                    .setNegativeButton("No", (dialog, which) -> hideLoading()) // Hide loading if cancelled
                    .show();
        } else {
            calculateScore(); // Proceed to calculate score if all answered
        }
    }

    /**
     * Calculates the quiz score and prepares data for the QuizResult activity.
     */
    private void calculateScore() {
        ArrayList<String> userAnswers = new ArrayList<>();

        for (int i = 0; i < qaContainer.getChildCount(); i++) {
            View child = qaContainer.getChildAt(i);
            Object tag = child.getTag();

            // Ensure tag is a valid map (it should always be after addQuestionBlock)
            if (!(tag instanceof Map)) {
                userAnswers.add(""); // Add empty answer if data is corrupt
                continue;
            }

            Map<String, Object> questionData = (Map<String, Object>) tag;
            String type = (String) questionData.get("type"); // Retrieve type from stored data
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
                // For identification, always use the EditText
                userAnswer = answerField.getText().toString().trim();
            }

            userAnswers.add(userAnswer);
        }

        // Store data in a singleton or static holder to pass to the next activity
        QuizDataHolder.setQuestionsList(questionsList);
        QuizDataHolder.setUserAnswers(userAnswers);

        hideLoading(); // Hide loading before starting new activity

        Intent intent = new Intent(TakeQuiz.this, QuizResult.class);
        startActivity(intent);
        finish(); // Finish this activity so user can't navigate back to it
    }
}