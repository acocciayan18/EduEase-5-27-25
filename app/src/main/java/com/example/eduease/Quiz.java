package com.example.eduease;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Quiz {
    private String id;
    private String creatorId;
    private Timestamp timestamp;
    private String title;
    private String description;
    private List<Question> questions;
    private String type;
    private boolean isFlash;

    // These were public - changed to private + added getter/setter for better practice
    private String question;
    private List<String> choices;
    private String correctAnswer;

    // These are your extra fields (kept as you requested)
    private List<Quiz> quizItems = new ArrayList<>();
    private int currentIndex = 0;

    // Default constructor (required for Firestore)
    public Quiz() {}

    private String typeQuiz;

    public String getTypeQuiz() {
        return typeQuiz;
    }

    public void setTypeQuiz(String typeQuiz) {
        this.typeQuiz = typeQuiz;
    }


    // Constructor with parameters
    public Quiz(String id, String creatorId, Timestamp timestamp, String title, String description, List<Question> questions, boolean isFlash) {
        this.id = id;
        this.creatorId = creatorId;
        this.timestamp = timestamp;
        this.title = title;
        this.description = description;
        this.questions = questions;
        this.isFlash = isFlash;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public boolean isFlash() {
        return isFlash;
    }

    public void setFlash(boolean flash) {
        isFlash = flash;
    }

    public void setBonusPoints(int bonusPoints) {
        // You can implement bonus point logic here if you want later
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // Getter and Setter for question
    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    // Getter and Setter for choices
    public List<String> getChoices() {
        return choices;
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
    }

    // Getter and Setter for correctAnswer
    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    // Fix your incorrect method (should be void method, not constructor)
    public void QuizQuestion(String question, List<String> choices, String correctAnswer) {
        this.question = question;
        this.choices = choices;
        this.correctAnswer = correctAnswer;
    }

    // Getter and Setter for quizItems list (optional, based on your logic)
    public List<Quiz> QuizQuestion() {
        return quizItems;
    }

    public void QuizQuestion(List<Quiz> quizItems) {
        this.quizItems = quizItems;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }
}
