package com.example.eduease;

import com.google.firebase.FirebaseApp;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.FirebaseOptions;

public class TakeBonusFlash extends BaseActivity {

    private TextView quizTitleTextView;
    private TextView quizDescriptionTextView;
    private GridLayout bonusPointsContainer;

    private int totalQuestions = 0;
    private int answeredQuestions = 0;

    private int totalBonusPoints = 0; // Store the total bonus points
    private TextView totalBonusPointsTextView; // Reference to the TextView for displaying total points

    private LinearLayout qaContainer; // This seems unused in the provided snippet for TakeBonusFlash, can remove if truly not used

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ayan_activity_take_bonus_flash);

        // Initialize Firebase "bonusFlashApp" instance
        // It's crucial to initialize FirebaseApp only once per app lifecycle for a given name.
        // The try-catch block helps prevent crashes if it's already initialized.
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:882141634417:android:ac69b51d83d01def3460d0")
                    .setApiKey("AIzaSyBlECTZf28SbEc4xHsz7JnH99YtTw6T58I")
                    .setProjectId("edu-ease-ni-ayan")
                    .setDatabaseUrl("https://edu-ease-ni-ayan-default-rtdb.firebaseio.com/")
                    .build();
            FirebaseApp.initializeApp(this, options, "bonusFlashApp");
        } catch (IllegalStateException ignored) {
            // App already initialized, safely ignore.
        }

        // Initialize UI elements
        quizTitleTextView = findViewById(R.id.quiz_title);
        quizDescriptionTextView = findViewById(R.id.quiz_description);
        bonusPointsContainer = findViewById(R.id.bonus_points_container);
        // qaContainer = findViewById(R.id.qa_container); // Uncomment/remove based on its actual usage
        totalBonusPointsTextView = findViewById(R.id.total_bonus_points);

        // Get quizId from the intent
        Intent intent = getIntent();
        String quizId = intent.getStringExtra("quizId");

        // Validate quizId before attempting to load data
        if (quizId == null || quizId.isEmpty()) {
            Toast.makeText(this, "Quiz ID not provided.", Toast.LENGTH_SHORT).show();
            // It's good practice to hide loading if the activity finishes prematurely
            hideLoading();
            finish();
            return; // Exit onCreate if no valid quizId
        }

        // Load quiz details using the provided quizId
        loadQuizDetails(quizId);
    }

    /**
     * Initiates loading of quiz details from Firebase.
     * Shows a loading indicator during the data retrieval process.
     * @param quizId The ID of the quiz to load.
     */
    private void loadQuizDetails(String quizId) {
        showLoading(); // Show loading indicator before fetching data

        try {
            // Get the Firebase database instance for the "bonusFlashApp"
            FirebaseDatabase bonusDb = FirebaseDatabase.getInstance(FirebaseApp.getInstance("bonusFlashApp"));
            DatabaseReference quizRef = bonusDb.getReference("bonus_quizzes").child(quizId);

            quizRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Extract title and description
                        String title = snapshot.child("title").getValue(String.class);
                        String description = snapshot.child("description").getValue(String.class);

                        // Set title and description to TextViews, providing defaults if null
                        quizTitleTextView.setText(title != null ? title : "Title not found");
                        quizDescriptionTextView.setText(description != null ? description : "Description not found");

                        // Now load the bonus points for each question
                        // Pass the entire snapshot to loadBonusPoints to allow iterating through children
                        loadBonusPoints(snapshot);
                    } else {
                        Toast.makeText(TakeBonusFlash.this, "Quiz not found.", Toast.LENGTH_SHORT).show();
                        hideLoading(); // Hide loading if quiz does not exist
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(TakeBonusFlash.this, "Failed to load quiz: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    hideLoading(); // Hide loading on error
                    finish();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Firebase initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            hideLoading(); // Hide loading if there's a Firebase init error
            finish();
        }
    }

    /**
     * Populates the UI with bonus question cards based on the provided DataSnapshot.
     * This method is called after the main quiz details are loaded.
     * The hideLoading() call is moved here to ensure all cards are processed before hiding.
     * @param dataSnapshot The DataSnapshot containing the quiz data, including bonus questions.
     */
    @SuppressLint("SetTextI18n")
    private void loadBonusPoints(DataSnapshot dataSnapshot) {
        totalQuestions = 0; // Reset total questions
        answeredQuestions = 0; // Reset answered questions

        // Clear previous views if any (important for re-loading or orientation changes)
        bonusPointsContainer.removeAllViews();

        // Iterate through potential question nodes (BonusQA1, BonusQA2, etc.)
        // This is a common pattern if your keys are sequential (e.g., "BonusQA1", "BonusQA2")
        // If your questions are direct children without "BonusQA" prefix, you'd iterate over dataSnapshot.getChildren() directly.
        // Assuming structure is like `quizId -> BonusQA1 -> {question, answer, bonusPoints}`
        // You'll need to confirm your Firebase structure.

        // Example if your questions are direct children of the quiz node (e.g., quizId -> {question1, question2, ...})
        // for (DataSnapshot bonusQASnapshot : dataSnapshot.getChildren()) { ... }
        // but given `bonus_quizzes -> quizId -> BonusQA1`, etc. we need a different approach.

        // Let's assume your structure is: quizId -> {title, description, BonusQA1: {}, BonusQA2: {}, ...}
        // So we need to iterate over specific children keys like "BonusQA1", "BonusQA2"
        int index = 1;
        boolean hasQuestions = false; // Flag to check if any questions were processed

        while (dataSnapshot.hasChild("BonusQA" + index)) {
            DataSnapshot bonusQASnapshot = dataSnapshot.child("BonusQA" + index);
            try {
                Integer bonusPoints = bonusQASnapshot.child("bonusPoints").getValue(Integer.class);
                String question = bonusQASnapshot.child("question").getValue(String.class);
                String answer = bonusQASnapshot.child("answer").getValue(String.class);

                if (bonusPoints != null && question != null && answer != null) {
                    totalQuestions++;
                    hasQuestions = true; // Mark that we found at least one question
                    View cardView = getLayoutInflater().inflate(R.layout.item_random_question_box, bonusPointsContainer, false);

                    // Set layout parameters for GridLayout
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.width = 0; // Width will be calculated by weight
                    params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                    params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // Distribute columns evenly
                    // params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // This might not be needed if height is determined by width
                    params.setMargins(8, 8, 8, 8);
                    cardView.setLayoutParams(params);

                    // Force square dimension - this needs to be carefully handled to avoid layout issues
                    // It's often better to define a fixed size or aspect ratio in XML if possible.
                    // If you must do this programmatically, `post` ensures it runs after layout pass.
                    cardView.post(() -> {
                        int width = cardView.getWidth();
                        if (width > 0) { // Ensure width is valid
                            cardView.getLayoutParams().height = width;
                            cardView.requestLayout();
                        }
                    });

                    TextView bonusPointsTextView = cardView.findViewById(R.id.bonus_points);
                    bonusPointsTextView.setText("" + bonusPoints);

                    // Center the text and make it bold
                    bonusPointsTextView.setGravity(Gravity.CENTER);
                    bonusPointsTextView.setTypeface(null, Typeface.BOLD);

                    bonusPointsContainer.addView(cardView);

                    // Set click listener to show question dialog
                    cardView.setOnClickListener(v -> showBonusQuestionDialog(question, answer, bonusPoints, cardView));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error processing a question: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            index++;
        }

        // --- Crucial Fix Here ---
        // Hide loading ONLY AFTER all questions have been processed and added to the container.
        hideLoading();

        if (!hasQuestions) {
            Toast.makeText(this, "No bonus questions found for this quiz.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Displays an AlertDialog with the bonus question and an input field for the answer.
     * Handles user submission and updates score.
     */
    @SuppressLint("SetTextI18n")
    private void showBonusQuestionDialog(String question, String answer, Integer bonusPoints, View cardView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(TakeBonusFlash.this);
        @SuppressLint("InflateParams") // Suppress lint warning for passing null as root
        View dialogView = getLayoutInflater().inflate(R.layout.bonus_question_dialog, null);

        TextView questionTextView = dialogView.findViewById(R.id.question_text);
        EditText answerInput = dialogView.findViewById(R.id.answer_input);
        TextView bonusPointsTextView = dialogView.findViewById(R.id.bonus_points_text);
        Button submitButton = dialogView.findViewById(R.id.submit_button);

        questionTextView.setText(question);
        bonusPointsTextView.setText("Bonus Points: " + bonusPoints);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        submitButton.setOnClickListener(v -> {
            String userAnswer = answerInput.getText().toString().trim();

            if (!userAnswer.isEmpty()) {
                cardView.setEnabled(false); // Disable card so it can't be clicked again
                cardView.setAlpha(0.5f); // Visually dim it (can also change background color)

                if (userAnswer.equalsIgnoreCase(answer)) {
                    totalBonusPoints += bonusPoints;
                    // Update the TextView to reflect new total points
                    totalBonusPointsTextView.setText("Total Bonus Points: " + totalBonusPoints);
                    Toast.makeText(TakeBonusFlash.this, "Correct! You earned " + bonusPoints + " bonus points.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(TakeBonusFlash.this, "Oops! That’s incorrect. The answer was: " + answer, Toast.LENGTH_LONG).show(); // Show correct answer
                }

                answeredQuestions++;
                dialog.dismiss();

                // Check if all questions have been answered
                if (answeredQuestions == totalQuestions) {
                    showCompletionDialog(totalBonusPoints);
                }
            } else {
                Toast.makeText(TakeBonusFlash.this, "Please provide an answer to submit.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    /**
     * Navigates to the BonusFlashResult activity, passing the total bonus points earned.
     * @param totalBonusPoints The final score to pass to the result screen.
     */
    private void showCompletionDialog(int totalBonusPoints) {
        Intent intent = new Intent(this, BonusFlashResult.class);
        intent.putExtra("totalBonusPoints", totalBonusPoints);
        startActivity(intent);
        finish(); // Finish this activity so the user can't go back to the quiz via back button
    }
}