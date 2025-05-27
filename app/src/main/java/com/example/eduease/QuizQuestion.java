package com.example.eduease;

import java.util.Map;

public class QuizQuestion {

    private String question;
    private Map<String, String> choices;
    private String answer;

    public QuizQuestion() {
        // Default constructor required for Firebase
    }

    public QuizQuestion(String question, Map<String, String> choices, String answer) {
        this.question = question;
        this.choices = choices;
        this.answer = answer;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Map<String, String> getChoices() {
        return choices;
    }

    public void setChoices(Map<String, String> choices) {
        this.choices = choices;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
