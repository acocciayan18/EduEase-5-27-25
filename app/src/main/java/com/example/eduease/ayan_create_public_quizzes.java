package com.example.eduease;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ayan_create_public_quizzes extends BaseActivity {

    private LinearLayout qaContainer;
    private Vibrator vibrator;
    private EditText quizTitle;
    private EditText quizDescription;
    private DatabaseReference publicDbRef;
    private FirebaseApp secondaryApp;
    private String quizId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ayan_activity_create_public_quiz);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        quizTitle = findViewById(R.id.quiz_title);
        quizDescription = findViewById(R.id.quiz_description);
        qaContainer = findViewById(R.id.qa_container);

        // Initialize secondary Firebase App
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId("1:882141634417:android:ac69b51d83d01def3460d0")
                .setApiKey("AIzaSyBlECTZf28SbEc4xHsz7JnH99YtTw6T58I")
                .setProjectId("edu-ease-ni-ayan")
                .setDatabaseUrl("https://edu-ease-ni-ayan-default-rtdb.firebaseio.com/")
                .build();

        try {
            secondaryApp = FirebaseApp.initializeApp(this, options, "secondary");
        } catch (IllegalStateException e) {
            secondaryApp = FirebaseApp.getInstance("secondary");
        }

        publicDbRef = FirebaseDatabase.getInstance(secondaryApp).getReference("public_quizzes");

        MaterialButton saveButton = findViewById(R.id.save_btn);
        saveButton.setOnClickListener(v -> saveToSecondaryDatabase());

        quizId = getIntent().getStringExtra("QUIZ_ID");
        if (quizId != null) {
            loadQuizData(quizId);
        } else {
            for (int i = 0; i < 3; i++) {
                addQuestionAnswerBlock();
            }
        }
    }

    private void addQuestionAnswerBlock() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View qaBlock = inflater.inflate(R.layout.question_and_answer, qaContainer, false);

        Spinner typeSpinner = qaBlock.findViewById(R.id.question_type_spinner);
        LinearLayout answerContainer = qaBlock.findViewById(R.id.answer_container);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.question_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                answerContainer.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(ayan_create_public_quizzes.this);

                switch (selectedType) {
                    case "Identification":
                        EditText idAnswer = new EditText(ayan_create_public_quizzes.this);
                        idAnswer.setHint("Enter answer here");
                        styleInput(idAnswer);
                        answerContainer.addView(idAnswer);
                        break;

                    case "True or False":
                        LinearLayout tfLayout = new LinearLayout(ayan_create_public_quizzes.this);
                        tfLayout.setOrientation(LinearLayout.VERTICAL);

                        List<RadioButton> tfButtons = new ArrayList<>();
                        String[] tfOptions = {"True", "False"};

                        for (int i = 0; i < 2; i++) {
                            LinearLayout row = new LinearLayout(ayan_create_public_quizzes.this);
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            row.setPadding(0, 8, 0, 8);

                            RadioButton rb = new RadioButton(ayan_create_public_quizzes.this);
                            rb.setId(View.generateViewId());
                            if (i == 0) rb.setChecked(true);

                            TextView label = new TextView(ayan_create_public_quizzes.this);
                            label.setText(tfOptions[i]);
                            label.setTextSize(16);
                            label.setPadding(16, 0, 0, 0);
                            label.setLayoutParams(new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                            tfButtons.add(rb);
                            rb.setOnClickListener(v -> {
                                for (RadioButton other : tfButtons) {
                                    other.setChecked(other == rb);
                                }
                            });

                            row.addView(rb);
                            row.addView(label);
                            tfLayout.addView(row);
                        }

                        answerContainer.addView(tfLayout);
                        break;

                    case "Multiple Choice":
                        LinearLayout multipleLayout = new LinearLayout(ayan_create_public_quizzes.this);
                        multipleLayout.setOrientation(LinearLayout.VERTICAL);

                        List<RadioButton> radioButtons = new ArrayList<>();

                        for (int i = 0; i < 4; i++) {
                            LinearLayout optionRow = new LinearLayout(ayan_create_public_quizzes.this);
                            optionRow.setOrientation(LinearLayout.HORIZONTAL);
                            optionRow.setPadding(0, 8, 0, 8);

                            RadioButton rb = new RadioButton(ayan_create_public_quizzes.this);
                            rb.setId(View.generateViewId());
                            if (i == 0) rb.setChecked(true);
                            radioButtons.add(rb);

                            EditText edit = new EditText(ayan_create_public_quizzes.this);
                            edit.setHint("Choice " + (i + 1));
                            edit.setLayoutParams(new LinearLayout.LayoutParams(
                                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                            rb.setOnClickListener(v -> {
                                for (RadioButton otherRb : radioButtons) {
                                    otherRb.setChecked(otherRb == rb);
                                }
                            });

                            optionRow.addView(rb);
                            optionRow.addView(edit);

                            multipleLayout.addView(optionRow);
                        }

                        answerContainer.addView(multipleLayout);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Buttons
        ImageButton addButton = qaBlock.findViewById(R.id.add_qa);
        addButton.setOnClickListener(v -> {
            vibrate();
            addQuestionAnswerBlock();
        });

        ImageButton deleteButton = qaBlock.findViewById(R.id.delete_qa);
        deleteButton.setOnClickListener(v -> {
            vibrate();
            qaContainer.removeView(qaBlock);
            updateDeleteButtons();
        });

        qaContainer.addView(qaBlock);
        updateDeleteButtons();
    }





//    private void monitorQAChanges(View qaBlock) {
//        EditText questionInput = qaBlock.findViewById(R.id.question_field);
//        EditText answerInput = qaBlock.findViewById(R.id.answer_field);
//
//        View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
//            if (!hasFocus) {
//                rearrangeBlocks();
//            }
//        };
//
//        questionInput.setOnFocusChangeListener(focusChangeListener);
//        answerInput.setOnFocusChangeListener(focusChangeListener);
 //   }

//    private void rearrangeBlocks() {
//        int childCount = qaContainer.getChildCount();
//        List<View> filledBlocks = new ArrayList<>();
//        List<View> emptyBlocks = new ArrayList<>();
//
//        for (int i = 0; i < childCount; i++) {
//            View block = qaContainer.getChildAt(i);
//            EditText questionInput = block.findViewById(R.id.question_field);
//            EditText answerInput = block.findViewById(R.id.answer_field);
//
//            if (questionInput.getText().toString().trim().isEmpty() || answerInput.getText().toString().trim().isEmpty()) {
//                emptyBlocks.add(block);
//            } else {
//                filledBlocks.add(block);
//            }
//        }
//
//        qaContainer.removeAllViews();
//        for (View block : emptyBlocks) {
//            qaContainer.addView(block);
//        }
//        for (View block : filledBlocks) {
//            qaContainer.addView(block);
//        }
//        updateDeleteButtons();
//    }

    @SuppressLint("SetTextI18n")
    private void updateDeleteButtons() {
        int childCount = qaContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View qaBlock = qaContainer.getChildAt(i);
            ImageButton deleteButton = qaBlock.findViewById(R.id.delete_qa);
            deleteButton.setEnabled(childCount > 1);
        }
    }

    private void styleInput(EditText editText) {
        editText.setTextColor(getResources().getColor(R.color.black));
        editText.setTextSize(17f);
        editText.setPadding(8, 8, 8, 8);
        editText.setTypeface(ResourcesCompat.getFont(this, R.font.poppinsregular));
        editText.setBackground(null);
    }

    private void saveToSecondaryDatabase() {
        String title = quizTitle.getText().toString().trim();
        String description = quizDescription.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Title and Description cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading();

        Map<String, Object> quizRoot = new HashMap<>();
        quizRoot.put("creatorId", currentUser.getUid());
        quizRoot.put("title", title);
        quizRoot.put("description", description);
        quizRoot.put("timestamp", System.currentTimeMillis());
        quizRoot.put("type", "public");

        Map<String, Object> identification = new HashMap<>();
        Map<String, Object> multipleChoice = new HashMap<>();
        Map<String, Object> trueFalse = new HashMap<>();

        int idCount = 0, mcCount = 0, tfCount = 0;
        boolean hasInvalidInput = false;

        for (int i = 0; i < qaContainer.getChildCount(); i++) {
            View qaBlock = qaContainer.getChildAt(i);

            EditText questionField = qaBlock.findViewById(R.id.question_field);
            Spinner typeSpinner = qaBlock.findViewById(R.id.question_type_spinner);
            LinearLayout answerContainer = qaBlock.findViewById(R.id.answer_container);

            String question = questionField.getText().toString().trim();
            String type = typeSpinner.getSelectedItem().toString();

            if (question.isEmpty()) {
                hasInvalidInput = true;
                break;
            }

            switch (type) {
                case "Identification":
                    if (answerContainer.getChildCount() == 0) {
                        hasInvalidInput = true;
                        break;
                    }

                    EditText answerInput = (EditText) answerContainer.getChildAt(0);
                    String idAnswer = answerInput.getText().toString().trim();
                    if (idAnswer.isEmpty()) {
                        hasInvalidInput = true;
                        break;
                    }

                    Map<String, Object> idData = new HashMap<>();
                    idData.put("question", question);
                    idData.put("answer", idAnswer);
                    identification.put("Question " + (++idCount), idData);
                    break;

                case "True or False":
                    if (answerContainer.getChildCount() == 0) {
                        hasInvalidInput = true;
                        break;
                    }

                    LinearLayout tfLayout = (LinearLayout) answerContainer.getChildAt(0);
                    String selectedTF = null;

                    for (int j = 0; j < tfLayout.getChildCount(); j++) {
                        LinearLayout tfRow = (LinearLayout) tfLayout.getChildAt(j);
                        RadioButton rb = (RadioButton) tfRow.getChildAt(0);
                        TextView label = (TextView) tfRow.getChildAt(1);
                        if (rb.isChecked()) {
                            selectedTF = label.getText().toString();
                            break;
                        }
                    }

                    if (selectedTF == null) {
                        hasInvalidInput = true;
                        break;
                    }

                    Map<String, Object> tfData = new HashMap<>();
                    tfData.put("question", question);
                    tfData.put("answer", selectedTF);
                    trueFalse.put("Question " + (++tfCount), tfData);
                    break;

                case "Multiple Choice":
                    if (answerContainer.getChildCount() == 0) {
                        hasInvalidInput = true;
                        break;
                    }

                    LinearLayout mcLayout = (LinearLayout) answerContainer.getChildAt(0);
                    List<String> choices = new ArrayList<>();
                    String correctChoice = null;

                    for (int j = 0; j < mcLayout.getChildCount(); j++) {
                        LinearLayout optionRow = (LinearLayout) mcLayout.getChildAt(j);
                        RadioButton rb = (RadioButton) optionRow.getChildAt(0);
                        EditText et = (EditText) optionRow.getChildAt(1);

                        String choice = et.getText().toString().trim();
                        if (!choice.isEmpty()) {
                            choices.add(choice);
                            if (rb.isChecked()) correctChoice = choice;
                        }
                    }

                    if (choices.size() < 2 || correctChoice == null) {
                        hasInvalidInput = true;
                        break;
                    }

                    Map<String, Object> mcData = new HashMap<>();
                    mcData.put("question", question);
                    mcData.put("choices", choices);
                    mcData.put("answer", correctChoice);
                    multipleChoice.put("Question " + (++mcCount), mcData);
                    break;
            }

            if (hasInvalidInput) break;
        }


        if (hasInvalidInput || (idCount + mcCount + tfCount == 0)) {
            hideLoading();
            Toast.makeText(this, "Each question must have a valid question and answer.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (idCount + mcCount + tfCount == 0) {

            hideLoading();
            Toast.makeText(this, "Please add at least one valid question.", Toast.LENGTH_SHORT).show();
            return;
        }




        if (!identification.isEmpty()) quizRoot.put("identification", identification);
        if (!multipleChoice.isEmpty()) quizRoot.put("multiple_choice", multipleChoice);
        if (!trueFalse.isEmpty()) quizRoot.put("true_or_false", trueFalse);

        String quizKey = publicDbRef.push().getKey();
        if (quizKey != null) {
            publicDbRef.child(quizKey)
                    .setValue(quizRoot)
                    .addOnSuccessListener(aVoid -> {
                        hideLoading();
                        Toast.makeText(this, "Public quiz saved successfully!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, Home.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        hideLoading();
                        Toast.makeText(this, "Failed to save public quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

    }



    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    private void loadQuizData(String quizId) {
        // Future method if you want to edit existing quiz
    }
}
