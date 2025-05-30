package com.example.eduease;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter; // Added for spinner adapter
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap; // Added for better data structuring in Firebase
import java.util.List;
import java.util.Map; // Added for better data structuring in Firebase
import java.util.Objects; // Added for null checks

public class Edit_Create_Public_Quiz extends AppCompatActivity {

    private LinearLayout qaContainer;
    private Vibrator vibrator;
    private boolean hasChanges = false;
    private String quizId;

    private EditText titleField, descriptionField;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_create_public_quiz);


        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // Initialize UI elements
        titleField = findViewById(R.id.edit_quiz_title);
        descriptionField = findViewById(R.id.edit_quiz_description);
        qaContainer = findViewById(R.id.qa_container);

        // Retrieve quiz ID from intent
        quizId = getIntent().getStringExtra("QUIZ_ID");
        if (quizId != null && !quizId.isEmpty()) { // Added isEmpty check
            loadPublicQuiz(quizId);
        } else {
            // If no quizId, this is a new quiz, add an initial question block
            addQuestionBlock(qaContainer, "", "Identification", "", new ArrayList<>());
            updateDeleteButtons(); // Ensure delete button is enabled/disabled correctly
        }

        // Set up listeners for changes to enable the 'hasChanges' flag
        setupChangeListeners(titleField);
        setupChangeListeners(descriptionField);
        // We'll also need to set up listeners for dynamically added question blocks

        Button saveButton = findViewById(R.id.edit_save_btn);
        saveButton.setOnClickListener(v -> handleSaveButtonClick()); // Renamed for clarity
    }


    private void handleSaveButtonClick() {
        if (!hasChanges) {
            showAlertDialog("No Changes", "You haven't made any changes.");
            return;
        }

        if (!validateQuiz()) {
            return;
        }

        updateQuizInDatabase();
    }


    private void loadPublicQuiz(String quizId) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference quizRef = database.getReference("public_quizzes").child(quizId);

        quizRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Use Objects.requireNonNull for better null handling
                titleField.setText(Objects.requireNonNull(snapshot.child("title").getValue(String.class)));
                descriptionField.setText(Objects.requireNonNull(snapshot.child("description").getValue(String.class)));

                qaContainer.removeAllViews(); // Clear existing views before loading
                // allQuestions.clear(); // Not strictly needed if we rebuild from UI

                // Load questions for each type
                loadQuestionType(snapshot.child("identification"), "Identification");
                loadQuestionType(snapshot.child("true_or_false"), "True or False");
                loadQuestionType(snapshot.child("multiple_choice"), "Multiple Choice");

                // After loading all questions, add them to the UI
                for (PublicQuizQuestion q : allQuestions) { // Using allQuestions to build UI
                    addQuestionBlock(qaContainer, q.question, q.type, q.answer, q.choices);
                }
                updateDeleteButtons(); // Update delete button state after loading
                hasChanges = false; // Reset changes flag after loading
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Edit_Create_Public_Quiz.this, "Failed to load quiz: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final List<PublicQuizQuestion> allQuestions = new ArrayList<>();


    private void loadQuestionType(DataSnapshot typeSnapshot, String typeName) {
        if (typeSnapshot.exists()) {
            for (DataSnapshot qSnap : typeSnapshot.getChildren()) {
                String question = qSnap.child("question").getValue(String.class);
                String answer = qSnap.child("answer").getValue(String.class);
                List<String> choices = new ArrayList<>();

                if (typeName.equals("Multiple Choice")) {
                    // It's safer to iterate children and get values, especially if keys aren't sequential numbers
                    for (DataSnapshot choiceSnap : qSnap.child("choices").getChildren()) {
                        String choice = choiceSnap.getValue(String.class);
                        if (choice != null) {
                            choices.add(choice);
                        }
                    }
                }
                // Ensure question and answer are not null before adding
                if (question != null && answer != null) {
                    allQuestions.add(new PublicQuizQuestion(typeName, question, answer, choices));
                }
            }
        }
    }


    private void addQuestionBlock(LinearLayout container, String questionText, String type, String answerText, List<String> choices) {
        @SuppressLint("InflateParams") // Suppress lint warning for passing null as root
        View block = LayoutInflater.from(this).inflate(R.layout.question_and_answer, container, false);

        EditText questionField = block.findViewById(R.id.question_field);
        Spinner typeSpinner = block.findViewById(R.id.question_type_spinner);
        LinearLayout answerContainer = block.findViewById(R.id.answer_container);

        questionField.setText(questionText);
        setupChangeListeners(questionField); // Attach listener to new question field

        // Set up the Spinner with question types
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.question_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        // Set initial selection for the spinner
        String[] types = getResources().getStringArray(R.array.question_types);
        for (int i = 0; i < types.length; i++) {
            if (types[i].equalsIgnoreCase(type)) {
                typeSpinner.setSelection(i);
                break;
            }
        }

        // Handle question type selection changes
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                answerContainer.removeAllViews(); // Clear previous answer fields

                // Re-inflate answer section based on selected type
                inflateAnswerSection(answerContainer, selectedType, answerText, choices);
                hasChanges = true; // Mark changes when type is changed
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Initially inflate the answer section based on the provided type
        inflateAnswerSection(answerContainer, type, answerText, choices);

        container.addView(block);

        // Set up listeners for the add and delete buttons
        ImageButton addButton = block.findViewById(R.id.add_qa);
        addButton.setOnClickListener(v -> {
            vibrate();
            addQuestionBlock(qaContainer, "", "Identification", "", new ArrayList<>());
            updateDeleteButtons(); // Update after adding a new question
        });

        ImageButton deleteButton = block.findViewById(R.id.delete_qa);
        deleteButton.setOnClickListener(v -> {
            vibrate();
            qaContainer.removeView(block);
            updateDeleteButtons(); // Update after deleting a question
            hasChanges = true; // Mark changes when a question is deleted
        });
        updateDeleteButtons(); // Ensure button states are correct for newly added block
    }


    private void inflateAnswerSection(LinearLayout answerContainer, String selectedType, String answerText, List<String> choices) {
        switch (selectedType) {
            case "Identification":
                EditText idAnswer = new EditText(Edit_Create_Public_Quiz.this);
                idAnswer.setHint("Enter answer here");
                styleAnswerField(idAnswer);
                idAnswer.setText(answerText);
                setupChangeListeners(idAnswer); // Attach listener to new answer field
                answerContainer.addView(idAnswer);
                break;

            case "True or False":
                LinearLayout tfLayout = new LinearLayout(Edit_Create_Public_Quiz.this);
                tfLayout.setOrientation(LinearLayout.VERTICAL);
                List<RadioButton> tfButtons = new ArrayList<>();
                String[] tfOptions = {"True", "False"};

                for (String option : tfOptions) {
                    LinearLayout row = new LinearLayout(Edit_Create_Public_Quiz.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(0, 8, 0, 8); // Add padding for better spacing

                    RadioButton rb = new RadioButton(Edit_Create_Public_Quiz.this);
                    rb.setId(View.generateViewId()); // Generate unique ID for each RadioButton
                    rb.setChecked(option.equalsIgnoreCase(answerText)); // Set checked based on initial answer

                    TextView label = new TextView(Edit_Create_Public_Quiz.this);
                    label.setText(option);
                    label.setTextSize(16);
                    label.setPadding(16, 0, 0, 0);
                    label.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    tfButtons.add(rb);
                    rb.setOnClickListener(v -> {
                        for (RadioButton other : tfButtons) {
                            other.setChecked(other == rb);
                        }
                        hasChanges = true; // Mark changes when a radio button is clicked
                    });

                    row.addView(rb);
                    row.addView(label);
                    tfLayout.addView(row);
                }
                answerContainer.addView(tfLayout);
                break;

            case "Multiple Choice":
                LinearLayout multipleLayout = new LinearLayout(Edit_Create_Public_Quiz.this);
                multipleLayout.setOrientation(LinearLayout.VERTICAL);
                List<RadioButton> radioButtons = new ArrayList<>();

                for (int i = 0; i < 4; i++) { // Always show 4 choices initially
                    LinearLayout optionRow = new LinearLayout(Edit_Create_Public_Quiz.this);
                    optionRow.setOrientation(LinearLayout.HORIZONTAL);
                    optionRow.setPadding(0, 8, 0, 8);

                    RadioButton rb = new RadioButton(Edit_Create_Public_Quiz.this);
                    rb.setId(View.generateViewId()); // Generate unique ID for each RadioButton

                    EditText edit = new EditText(Edit_Create_Public_Quiz.this);
                    edit.setHint("Choice " + (i + 1));
                    edit.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                    setupChangeListeners(edit); // Attach listener to new choice field

                    if (i < choices.size()) {
                        edit.setText(choices.get(i));
                        if (choices.get(i).equals(answerText)) {
                            rb.setChecked(true);
                        }
                    }

                    rb.setOnClickListener(v -> {
                        for (RadioButton otherRb : radioButtons) {
                            otherRb.setChecked(otherRb == rb);
                        }
                        hasChanges = true; // Mark changes when a radio button is clicked
                    });

                    radioButtons.add(rb);
                    optionRow.addView(rb);
                    optionRow.addView(edit);
                    multipleLayout.addView(optionRow);
                }
                answerContainer.addView(multipleLayout);
                break;
        }
    }


    private void updateQuizInDatabase() {
        // Use FirebaseApp.getInstance() directly, or pass the instance if you have multiple.
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference quizRef = database.getReference("public_quizzes").child(quizId);

        // Update quiz title and description
        quizRef.child("title").setValue(titleField.getText().toString().trim());
        quizRef.child("description").setValue(descriptionField.getText().toString().trim());

        // Clear existing question types before adding new ones
        quizRef.child("identification").removeValue();
        quizRef.child("true_or_false").removeValue();
        quizRef.child("multiple_choice").removeValue();

        // Iterate through all question blocks in the UI and save them
        for (int i = 0; i < qaContainer.getChildCount(); i++) {
            View qaBlock = qaContainer.getChildAt(i);
            EditText questionField = qaBlock.findViewById(R.id.question_field);
            Spinner typeSpinner = qaBlock.findViewById(R.id.question_type_spinner);
            LinearLayout answerContainer = qaBlock.findViewById(R.id.answer_container);

            String question = questionField.getText().toString().trim();
            String type = typeSpinner.getSelectedItem().toString();
            String answer = "";
            List<String> choices = new ArrayList<>();

            // Use a Map to structure the question data before pushing to Firebase
            Map<String, Object> questionData = new HashMap<>();
            questionData.put("question", question);

            if (type.equals("Identification")) {
                EditText answerField = (EditText) answerContainer.getChildAt(0);
                answer = answerField.getText().toString().trim();
                questionData.put("answer", answer);
                database.getReference("public_quizzes").child(quizId).child("identification").push().setValue(questionData);
            } else if (type.equals("True or False")) {
                LinearLayout tfLayout = (LinearLayout) answerContainer.getChildAt(0);
                for (int j = 0; j < tfLayout.getChildCount(); j++) {
                    LinearLayout row = (LinearLayout) tfLayout.getChildAt(j);
                    RadioButton rb = (RadioButton) row.getChildAt(0);
                    TextView label = (TextView) row.getChildAt(1);
                    if (rb.isChecked()) {
                        answer = label.getText().toString();
                        break;
                    }
                }
                questionData.put("answer", answer);
                database.getReference("public_quizzes").child(quizId).child("true_or_false").push().setValue(questionData);
            } else if (type.equals("Multiple Choice")) {
                LinearLayout multipleLayout = (LinearLayout) answerContainer.getChildAt(0); // Assuming the first child is the LinearLayout holding choices
                for (int j = 0; j < multipleLayout.getChildCount(); j++) {
                    LinearLayout row = (LinearLayout) multipleLayout.getChildAt(j);
                    RadioButton rb = (RadioButton) row.getChildAt(0);
                    EditText choiceField = (EditText) row.getChildAt(1);
                    String choiceText = choiceField.getText().toString().trim();
                    if (!choiceText.isEmpty()) {
                        choices.add(choiceText);
                        if (rb.isChecked()) {
                            answer = choiceText;
                        }
                    }
                }
                questionData.put("answer", answer);
                questionData.put("choices", choices); // Store choices as a list
                database.getReference("public_quizzes").child(quizId).child("multiple_choice").push().setValue(questionData);
            }
        }

        hasChanges = false; // Reset the flag after successful save
        Toast.makeText(this, "Quiz updated successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void styleAnswerField(EditText field) {
        field.setTextSize(16);
        field.setPadding(16, 12, 16, 12);
        field.setTextColor(getResources().getColor(R.color.black));
        field.setBackgroundResource(R.drawable.edittext_background);
        // Set layout parameters to match parent width
        field.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private static class PublicQuizQuestion { // Changed to static to prevent memory leaks
        String type;
        String question;
        String answer;
        List<String> choices;

        PublicQuizQuestion(String type, String question, String answer, List<String> choices) {
            this.type = type;
            this.question = question;
            this.answer = answer;
            this.choices = choices;
        }
    }

    @SuppressLint("SetTextI18n") // Suppress lint warning for dynamically setting text (though not directly used here)
    private void updateDeleteButtons() {
        int childCount = qaContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View qaBlock = qaContainer.getChildAt(i);
            ImageButton deleteButton = qaBlock.findViewById(R.id.delete_qa);
            deleteButton.setEnabled(childCount > 1); // Disable if only one question
        }
    }

    @Override
    public void onBackPressed() {
        if (hasChanges) {
            showAlertDialog("Unsaved Changes", "You have unsaved changes. Do you want to discard them?",
                    (dialog, which) -> super.onBackPressed(), "Discard", "Cancel", null);
        } else {
            super.onBackPressed();
        }
    }


    private boolean validateQuiz() {
        if (titleField.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Quiz title cannot be empty", Toast.LENGTH_SHORT).show();
            titleField.requestFocus();
            return false;
        }
        if (descriptionField.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Quiz description cannot be empty", Toast.LENGTH_SHORT).show();
            descriptionField.requestFocus();
            return false;
        }

        if (qaContainer.getChildCount() == 0) {
            Toast.makeText(this, "Please add at least one question", Toast.LENGTH_SHORT).show();
            return false;
        }

        for (int i = 0; i < qaContainer.getChildCount(); i++) {
            View qaBlock = qaContainer.getChildAt(i);
            EditText questionField = qaBlock.findViewById(R.id.question_field);
            Spinner typeSpinner = qaBlock.findViewById(R.id.question_type_spinner);
            LinearLayout answerContainer = qaBlock.findViewById(R.id.answer_container);

            String question = questionField.getText().toString().trim();
            if (question.isEmpty()) {
                questionField.setError("Question cannot be empty");
                questionField.requestFocus();
                Toast.makeText(this, "Please provide a question for all question blocks", Toast.LENGTH_SHORT).show();
                return false;
            }

            String type = typeSpinner.getSelectedItem().toString();
            if (type.equals("Multiple Choice")) {
                int filledChoiceCount = 0;
                boolean hasCorrectAnswer = false;

                LinearLayout multipleLayout = (LinearLayout) answerContainer.getChildAt(0); // Get the LinearLayout holding choices
                for (int j = 0; j < multipleLayout.getChildCount(); j++) {
                    LinearLayout row = (LinearLayout) multipleLayout.getChildAt(j);
                    RadioButton rb = (RadioButton) row.getChildAt(0);
                    EditText choice = (EditText) row.getChildAt(1);

                    if (!choice.getText().toString().trim().isEmpty()) {
                        filledChoiceCount++;
                    }
                    if (rb.isChecked()) {
                        hasCorrectAnswer = true;
                    }
                }

                if (filledChoiceCount < 2) {
                    Toast.makeText(this, "Multiple Choice questions require at least 2 non-empty choices.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (!hasCorrectAnswer) {
                    Toast.makeText(this, "Please select a correct answer for all Multiple Choice questions.", Toast.LENGTH_SHORT).show();
                    return false;
                }

            } else if (type.equals("True or False")) {
                boolean selected = false;
                LinearLayout tfLayout = (LinearLayout) answerContainer.getChildAt(0);
                for (int j = 0; j < tfLayout.getChildCount(); j++) {
                    LinearLayout row = (LinearLayout) tfLayout.getChildAt(j);
                    RadioButton rb = (RadioButton) row.getChildAt(0);
                    if (rb.isChecked()) {
                        selected = true;
                        break;
                    }
                }
                if (!selected) {
                    Toast.makeText(this, "Please select True or False for all True or False questions.", Toast.LENGTH_SHORT).show();
                    return false;
                }

            } else if (type.equals("Identification")) {
                EditText answer = (EditText) answerContainer.getChildAt(0);
                if (answer.getText().toString().trim().isEmpty()) {
                    answer.setError("Answer is required.");
                    answer.requestFocus();
                    Toast.makeText(this, "Please provide an answer for all Identification questions.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        return true;
    }


    private void setupChangeListeners(View view) {
        if (view instanceof EditText) {
            ((EditText) view).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    hasChanges = true;
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        } else if (view instanceof Spinner) {
            ((Spinner) view).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    hasChanges = true;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
        // If it's a ViewGroup, iterate through its children to apply listeners
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setupChangeListeners(((ViewGroup) view).getChildAt(i));
            }
        }
    }


    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                // For older APIs
                vibrator.vibrate(50);
            }
        }
    }


    private void showAlertDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }


    private void showAlertDialog(String title, String message,
                                 android.content.DialogInterface.OnClickListener positiveClick, String positiveText,
                                 String negativeText, android.content.DialogInterface.OnClickListener negativeClick) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, positiveClick);

        if (negativeText != null && negativeClick != null) {
            builder.setNegativeButton(negativeText, negativeClick);
        }
        builder.show();
    }
}