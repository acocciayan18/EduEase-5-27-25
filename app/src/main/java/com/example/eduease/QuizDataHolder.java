package com.example.eduease;

import java.util.List;
import java.util.Map;

public class QuizDataHolder {
    private static List<Map<String, String>> questionsList;
    private static List<String> userAnswers;

    public static void setQuestionsList(List<Map<String, String>> list) {
        questionsList = list;
    }

    public static List<Map<String, String>> getQuestionsList() {
        return questionsList;
    }

    public static void setUserAnswers(List<String> answers) {
        userAnswers = answers;
    }

    public static List<String> getUserAnswers() {
        return userAnswers;
    }

    public static void clear() {
        questionsList = null;
        userAnswers = null;
    }
}
