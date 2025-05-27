package com.example.eduease;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class QuizResult extends BaseActivity {

    private List<Map<String, Object>> questionsList;
    private List<String> userAnswers;
    private LinearLayout qaContainer;
    private ScrollView scrollArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_quiz_result);

        // Initialize views
        qaContainer = findViewById(R.id.qa_container);
        scrollArea = findViewById(R.id.scroll_area); // ID in updated layout
        TextView resultMessage = findViewById(R.id.result_message);

        // Set custom quiz title if provided
        String quizTitle = getIntent().getStringExtra("QUIZ_TITLE");
        if (quizTitle != null && !quizTitle.isEmpty()) {
            resultMessage.setText(quizTitle.toUpperCase());
        } else {
            resultMessage.setText("QUIZ COMPLETED!");
        }

        // Get data from holder
        questionsList = QuizDataHolder.getQuestionsList();
        userAnswers = QuizDataHolder.getUserAnswers();

        calculateScore();

        // Scroll to top
        scrollArea.post(() -> scrollArea.fullScroll(View.FOCUS_UP));
    }

    @SuppressLint("SetTextI18n")
    private void calculateScore() {
        int score = 0;
        qaContainer.removeAllViews();

        for (int i = 0; i < questionsList.size(); i++) {
            Map<String, Object> questionData = questionsList.get(i);
            String question = (String) questionData.get("question");
            String correctAnswer = ((String) questionData.get("answer")).trim();
            String userAnswer = i < userAnswers.size() ? userAnswers.get(i).trim() : "";

            if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                score++;
            }

            addQADetailToView(i + 1, question, userAnswer, correctAnswer);
        }

        int totalQuestions = questionsList.size();
        float rawPercentage = (score / (float) totalQuestions) * 100;
        float roundedPercentage = rawPercentage >= 75 ? (float) Math.floor(rawPercentage) : (float) Math.ceil(rawPercentage);

        TextView scoreMessage = findViewById(R.id.score_message);
        scoreMessage.setText("Your Score: " + score + "/" + totalQuestions + " (" + (int) roundedPercentage + "%)");

        int soundResId = roundedPercentage >= 75 ? R.raw.pass : R.raw.fail;
        MediaPlayer mediaPlayer = MediaPlayer.create(this, soundResId);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);

        findViewById(R.id.finish_button).setVisibility(View.VISIBLE);
    }

    private void addQADetailToView(int index, String question, String userAnswer, String correctAnswer) {
        TextView qaText = new TextView(this);
        qaText.setText("Q" + index + ": " + question + "\n" +
                "Your Answer: " + (userAnswer.isEmpty() ? "No Answer" : userAnswer) + "\n" +
                "Correct Answer: " + correctAnswer);
        qaText.setTextSize(14);
        qaText.setTextColor(getResources().getColor(R.color.white));
        qaText.setLineSpacing(4f, 1f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            qaText.setTypeface(getResources().getFont(R.font.poppinsregular));
        }
        qaText.setPadding(0, 0, 0, 24);

        qaContainer.addView(qaText);
    }

    public void finishQuiz(View view) {
        finish();
    }
}
