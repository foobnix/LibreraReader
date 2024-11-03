package com.foobnix.pdf.info;

import android.content.Context;

public class SurveyManager {

    private static SurveyManager instance;
    private Context context;

    private SurveyManager(Context context) {
        this.context = context;
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new SurveyManager(context);
        }
    }

    public static synchronized SurveyManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SurveyManager is not initialized, call init() method first.");
        }
        return instance;
    }

    public void createSurvey(String surveyId, String question, String[] options) {
        // Code to create a new survey
    }

    public void manageSurvey(String surveyId) {
        // Code to manage an existing survey
    }
}
