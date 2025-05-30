package com.example.eduease;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull; // Added for DataSnapshot and DatabaseError annotations
import androidx.appcompat.app.AlertDialog; // Used for confirmation dialogs
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError; // Added for onCancelled
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener; // Added for real-time listener if needed, but not used here

import java.util.HashMap;
import java.util.Map;

public class Edit_Create_Local_Bonus_Flash extends BaseActivity {

    private EditText bonusQuizTitle, bonusQuizDescription;
    private LinearLayout bonusQaContainer;
    private Vibrator vibrator;
    private DatabaseReference bonusQuizzesDbRef; // Renamed secondaryDb for clarity
    private String quizId;
    private boolean hasChanges = false; // Flag to track unsaved changes

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_create_local_bonus_flash);

        // Initialize UI elements
        bonusQuizTitle = findViewById(R.id.flashquiz_input_title);
        bonusQuizDescription = findViewById(R.id.flashquiz_input_description);
        bonusQaContainer = findViewById(R.id.flashquiz_qa_block_container);
        MaterialButton saveBtn = findViewById(R.id.flashquiz_button_submit);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // Get quizId from the intent
        quizId = getIntent().getStringExtra("QUIZ_ID");

        // Initialize Firebase "secondary" app instance
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:882141634417:android:ac69b51d83d01def3460d0")
                    .setApiKey("AIzaSyBlECTZf28SbEc4xHsz7JnH99YtTw6T58I")
                    .setProjectId("edu-ease-ni-ayan")
                    .setDatabaseUrl("https://edu-ease-ni-ayan-default-rtdb.firebaseio.com/")
                    .build();

            FirebaseApp secondaryApp = FirebaseApp.initializeApp(getApplicationContext(), options, "secondary");
            bonusQuizzesDbRef = FirebaseDatabase.getInstance(secondaryApp).getReference("bonus_quizzes");
        } catch (IllegalStateException e) {
            Log.e("EditBonusFlash", "Firebase 'secondary' app already initialized, getting existing instance.");
            bonusQuizzesDbRef = FirebaseDatabase.getInstance(FirebaseApp.getInstance("secondary")).getReference("bonus_quizzes");
        }

        // Apply TextWatchers to track changes
        setupChangeListeners(bonusQuizTitle);
        setupChangeListeners(bonusQuizDescription);

        // Load quiz data if quizId is provided, otherwise prepare for new quiz creation
        if (quizId != null && !quizId.isEmpty()) {
            loadQuizData(quizId);
        } else {
            Toast.makeText(this, "No Quiz ID provided. Starting new quiz creation.", Toast.LENGTH_SHORT).show();
            // For a new quiz, add an initial empty question block
            addPreFilledBonusQABlock("", "", 0);
            updateDeleteButtons(); // Ensure delete button is handled for the single block
            hasChanges = true; // A new quiz implies changes are being made
        }

        // Set save button click listener
        saveBtn.setOnClickListener(v -> saveEditedQuiz());
    }

    /**
     * Overrides the back button behavior to prompt about unsaved changes.
     */
    @Override
    public void onBackPressed() {
        if (hasChanges) {
            new AlertDialog.Builder(this)
                    .setTitle("Unsaved Changes")
                    .setMessage("You have unsaved changes. Do you want to discard them and exit?")
                    .setPositiveButton("Discard", (dialog, which) -> super.onBackPressed())
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Loads existing quiz data from Firebase based on the provided quiz ID.
     * Shows loading indicator during data retrieval.
     */
    private void loadQuizData(String quizId) {
        showLoading(); // Show loading when starting data fetch
        bonusQuizzesDbRef.child(quizId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                bonusQuizTitle.setText(snapshot.child("title").getValue(String.class));
                bonusQuizDescription.setText(snapshot.child("description").getValue(String.class));
                bonusQaContainer.removeAllViews(); // Clear existing views before populating

                int index = 1;
                // Loop through and add all existing bonus QA blocks
                while (snapshot.hasChild("BonusQA" + index)) {
                    DataSnapshot qaSnap = snapshot.child("BonusQA" + index);
                    String q = qaSnap.child("question").getValue(String.class);
                    String a = qaSnap.child("answer").getValue(String.class);
                    Long bp = qaSnap.child("bonusPoints").getValue(Long.class);

                    // Add pre-filled block, ensuring non-null values
                    addPreFilledBonusQABlock(
                            q != null ? q : "",
                            a != null ? a : "",
                            bp != null ? bp.intValue() : 0
                    );
                    index++;
                }
                hasChanges = false; // Reset changes flag after loading data
            } else {
                Toast.makeText(this, "Quiz not found in database. Please check the ID or create a new one.", Toast.LENGTH_SHORT).show();
                // If quiz not found, but quizId was given, it means it's an invalid ID
                // In this case, we might still want to allow creating a new one or just finish.
                finish();
            }
            hideLoading(); // Hide loading after data is successfully loaded or not found
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            hideLoading(); // Hide loading if data fetch fails
            finish();
        });
    }

    /**
     * Validates the entire quiz content before saving.
     * Checks title, description, and each question block for completeness and valid values.
     * Displays a Toast message and returns false if any validation fails.
     *
     * @return true if all content is valid, false otherwise.
     */
    private boolean validateQuizContent() {
        String title = bonusQuizTitle.getText().toString().trim();
        String desc = bonusQuizDescription.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Quiz Title cannot be empty.", Toast.LENGTH_SHORT).show();
            bonusQuizTitle.setError("Required");
            bonusQuizTitle.requestFocus();
            return false;
        }
        if (desc.isEmpty()) {
            Toast.makeText(this, "Quiz Description cannot be empty.", Toast.LENGTH_SHORT).show();
            bonusQuizDescription.setError("Required");
            bonusQuizDescription.requestFocus();
            return false;
        }

        if (bonusQaContainer.getChildCount() < 3) {
            Toast.makeText(this, "Please add at least 3 question sets.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate each individual QA block
        for (int i = 0; i < bonusQaContainer.getChildCount(); i++) {
            View qaView = bonusQaContainer.getChildAt(i);
            EditText questionField = qaView.findViewById(R.id.bonus_question_field);
            EditText answerField = qaView.findViewById(R.id.bonus_answer_field);
            EditText bonusField = qaView.findViewById(R.id.bonus_points_field);

            String question = questionField.getText().toString().trim();
            String answer = answerField.getText().toString().trim();
            String pointsStr = bonusField.getText().toString().trim();

            if (question.isEmpty()) {
                Toast.makeText(this, "Question #" + (i + 1) + ": Question field cannot be empty.", Toast.LENGTH_SHORT).show();
                questionField.setError("Required");
                questionField.requestFocus();
                return false;
            }
            if (answer.isEmpty()) {
                Toast.makeText(this, "Question #" + (i + 1) + ": Answer field cannot be empty.", Toast.LENGTH_SHORT).show();
                answerField.setError("Required");
                answerField.requestFocus();
                return false;
            }
            if (pointsStr.isEmpty()) {
                Toast.makeText(this, "Question #" + (i + 1) + ": Bonus Points field cannot be empty.", Toast.LENGTH_SHORT).show();
                bonusField.setError("Required");
                bonusField.requestFocus();
                return false;
            }

            int points;
            try {
                points = Integer.parseInt(pointsStr);
                if (points < 0 || points > 99) {
                    Toast.makeText(this, "Question #" + (i + 1) + ": Bonus points must be between 0 and 99.", Toast.LENGTH_SHORT).show();
                    bonusField.setError("Invalid range (0-99)");
                    bonusField.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Question #" + (i + 1) + ": Invalid bonus point value. Please enter a number.", Toast.LENGTH_SHORT).show();
                bonusField.setError("Not a number");
                bonusField.requestFocus();
                return false;
            }
        }
        return true; // All validation passed
    }

    /**
     * Saves the edited/created quiz data to Firebase.
     * Shows loading indicator during the save operation.
     */
    private void saveEditedQuiz() {
        if (!hasChanges) {
            Toast.makeText(this, "No changes to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Perform comprehensive validation before saving
        if (!validateQuizContent()) {
            return; // If validation fails, stop the save process (Toast messages are already shown)
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in. Please log in to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(); // Show loading before starting the save process

        Map<String, Object> quizData = new HashMap<>();
        quizData.put("title", bonusQuizTitle.getText().toString().trim());
        quizData.put("description", bonusQuizDescription.getText().toString().trim());
        quizData.put("creatorId", user.getUid());
        quizData.put("flash", true);
        quizData.put("type", "local");

        // Iterate through all QA blocks in the container to prepare data for Firebase
        for (int i = 0; i < bonusQaContainer.getChildCount(); i++) {
            View qaView = bonusQaContainer.getChildAt(i);
            EditText questionField = qaView.findViewById(R.id.bonus_question_field);
            EditText answerField = qaView.findViewById(R.id.bonus_answer_field);
            EditText bonusField = qaView.findViewById(R.id.bonus_points_field);

            String question = questionField.getText().toString().trim();
            String answer = answerField.getText().toString().trim();
            int points = Integer.parseInt(bonusField.getText().toString().trim()); // Already validated by validateQuizContent()

            Map<String, Object> qa = new HashMap<>();
            qa.put("question", question);
            qa.put("answer", answer);
            qa.put("bonusPoints", points);
            quizData.put("BonusQA" + (i + 1), qa); // Use (i + 1) for sequential naming
        }

        // Determine the final quiz ID: use existing if editing, generate new if creating
        String finalQuizId = (quizId != null && !quizId.isEmpty()) ? quizId : bonusQuizzesDbRef.push().getKey();
        if (finalQuizId == null) {
            hideLoading();
            Toast.makeText(this, "Failed to generate quiz ID. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        bonusQuizzesDbRef.child(finalQuizId).setValue(quizData)
                .addOnSuccessListener(aVoid -> {
                    hideLoading(); // Hide loading on success
                    Toast.makeText(this, "Quiz updated successfully!", Toast.LENGTH_SHORT).show();
                    hasChanges = false; // Reset changes flag after successful save
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(this, Home.class));
                        finish(); // Finish current activity
                    }, 800);
                })
                .addOnFailureListener(e -> {
                    hideLoading(); // Hide loading on failure
                    Toast.makeText(this, "Error saving quiz: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Adds a pre-filled bonus question-answer block to the UI.
     * This is used for loading existing data or adding new empty blocks.
     * @param question The question text.
     * @param answer The answer text.
     * @param bonusPoints The bonus points value.
     */
    private void addPreFilledBonusQABlock(String question, String answer, int bonusPoints) {
        // Inflate the layout for a single QA block
        @SuppressLint("InflateParams")
        View qaBlock = getLayoutInflater().inflate(R.layout.bonus_qa_block, bonusQaContainer, false);

        // Find views within the inflated block
        EditText questionField = qaBlock.findViewById(R.id.bonus_question_field);
        EditText answerField = qaBlock.findViewById(R.id.bonus_answer_field);
        EditText bonusField = qaBlock.findViewById(R.id.bonus_points_field);

        // Set the text for the fields
        questionField.setText(question);
        answerField.setText(answer);
        bonusField.setText(String.valueOf(bonusPoints));

        // Apply TextWatchers to track changes for dynamically added fields
        setupChangeListeners(questionField);
        setupChangeListeners(answerField);
        setupChangeListeners(bonusField);

        // Set up click listeners for add and delete buttons within this block
        ImageButton addBtn = qaBlock.findViewById(R.id.add_button);
        ImageButton deleteBtn = qaBlock.findViewById(R.id.delete_button);

        addBtn.setOnClickListener(v -> {
            vibrate();
            addPreFilledBonusQABlock("", "", 0); // Add a new empty block
            updateDeleteButtons(); // Update delete button states after adding
            hasChanges = true; // Adding a block means a change has occurred
        });

        deleteBtn.setOnClickListener(v -> {
            vibrate();
            bonusQaContainer.removeView(qaBlock); // Remove this specific block
            updateDeleteButtons(); // Update delete button states after removing
            hasChanges = true; // Deleting a block means a change has occurred
        });

        // Add the new block to the container
        bonusQaContainer.addView(qaBlock);
        updateDeleteButtons(); // Initial update for the newly added block
    }

    /**
     * Updates the enabled/disabled state of delete buttons for all QA blocks.
     * Delete buttons are disabled if there are 3 or fewer questions.
     */
    private void updateDeleteButtons() {
        int count = bonusQaContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View qaView = bonusQaContainer.getChildAt(i);
            ImageButton deleteBtn = qaView.findViewById(R.id.delete_button);
            // Delete button is enabled if there are more than 3 questions
            deleteBtn.setEnabled(count > 3);
        }
    }

    /**
     * Sets up TextWatchers for EditTexts to track changes.
     * @param editText The EditText to attach the TextWatcher to.
     */
    private void setupChangeListeners(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanges = true; // Mark changes whenever text is modified
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Triggers a short vibration feedback.
     */
    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50); // For older API levels
            }
        }
    }
}