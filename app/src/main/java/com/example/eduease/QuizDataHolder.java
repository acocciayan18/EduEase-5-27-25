package com.example.eduease;

import java.util.List;
import java.util.Map;

public class QuizDataHolder {
    private static List<Map<String, Object>> questionsList;
    private static List<String> userAnswers;

    public static void setQuestionsList(List<Map<String, Object>> list) {
        questionsList = list;
    }

    public static List<Map<String, Object>> getQuestionsList() {
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
