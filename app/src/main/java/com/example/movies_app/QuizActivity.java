package com.example.movies_app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Quiz cinéma : question générée par Gemini, correction IA, score Firestore.
 */
public class QuizActivity extends AppCompatActivity {

    private static final int POINTS_PER_CORRECT = 10;

    private static final String SYSTEM_GENERATE =
            "Tu es un animateur de quiz cinéma. Génère UNE seule question en français "
                    + "sur le cinéma (film, acteur, réalisateur, année, etc.). "
                    + "Réponds UNIQUEMENT avec la question, sans réponse ni explication. "
                    + "Exemple : Quel est l'acteur principal du film Inception ?";

    private static final String SYSTEM_VALIDATE =
            "Tu corriges un quiz cinéma. L'utilisateur a répondu à une question. "
                    + "Réponds UNIQUEMENT par un seul mot : CORRECT ou INCORRECT. "
                    + "Sois tolérant sur l'orthographe et les majuscules si le sens est bon.";

    private TextView textViewQuestion;
    private TextView textViewScore;
    private TextView textViewFeedback;
    private TextInputEditText editTextAnswer;
    private ProgressBar progressBarQuiz;
    private MaterialButton buttonValidate;
    private MaterialButton buttonNewQuestion;

    private String currentQuestion = "";
    private boolean isCheckingAnswer = false;
    private boolean isGeneratingQuestion = false;
    private final QuizRound round = new QuizRound();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz);

        MaterialToolbar toolbar = findViewById(R.id.toolbarQuiz);
        toolbar.setNavigationOnClickListener(v -> finish());

        textViewQuestion = findViewById(R.id.textViewQuestion);
        textViewScore = findViewById(R.id.textViewScore);
        textViewFeedback = findViewById(R.id.textViewFeedback);
        editTextAnswer = findViewById(R.id.editTextAnswer);
        progressBarQuiz = findViewById(R.id.progressBarQuiz);
        buttonValidate = findViewById(R.id.buttonValidate);
        buttonNewQuestion = findViewById(R.id.buttonNewQuestion);

        buttonValidate.setOnClickListener(v -> validateAnswer());
        buttonNewQuestion.setOnClickListener(v -> generateNewQuestion());

        loadScore();
        if (savedInstanceState != null
                && !savedInstanceState.getString("question", "").isEmpty()) {
            currentQuestion = savedInstanceState.getString("question");
            textViewQuestion.setText(currentQuestion);
            round.questionReady();
            if (savedInstanceState.getBoolean("answered")) {
                round.answerCompleted();
                buttonValidate.setEnabled(false);
            }
        } else {
            generateNewQuestion();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("question", currentQuestion);
        outState.putBoolean("answered", round.isAnswered());
    }

    private void loadScore() {
        FirestoreHelper.getUserScore(new FirestoreHelper.ScoreCallback() {
            @Override
            public void onScore(int totalScore) {
                updateScoreDisplay(totalScore);
            }

            @Override
            public void onError(String message) {
                updateScoreDisplay(0);
            }
        });
    }

    private void updateScoreDisplay(int score) {
        textViewScore.setText(getString(R.string.quiz_score_format, score));
    }

    private void generateNewQuestion() {
        if (isGeneratingQuestion || round.isChecking()) {
            return;
        }
        isGeneratingQuestion = true;
        round.reset();
        currentQuestion = "";
        textViewFeedback.setVisibility(View.GONE);
        editTextAnswer.setText("");
        textViewQuestion.setText(R.string.quiz_loading_question);
        progressBarQuiz.setVisibility(View.VISIBLE);
        buttonValidate.setEnabled(false);
        buttonNewQuestion.setEnabled(false);

        GeminiHelper.sendMessage(this, SYSTEM_GENERATE, "Nouvelle question de quiz cinéma.",
                new GeminiHelper.GeminiCallback() {
                    @Override
                    public void onSuccess(String text) {
                        if (isDestroyed() || isFinishing()) return;
                        isGeneratingQuestion = false;
                        progressBarQuiz.setVisibility(View.GONE);
                        buttonValidate.setEnabled(true);
                        buttonNewQuestion.setEnabled(true);
                        currentQuestion = text;
                        round.questionReady();
                        textViewQuestion.setText(currentQuestion);
                    }

                    @Override
                    public void onError(String message) {
                        if (isDestroyed() || isFinishing()) return;
                        isGeneratingQuestion = false;
                        progressBarQuiz.setVisibility(View.GONE);
                        buttonValidate.setEnabled(true);
                        buttonNewQuestion.setEnabled(true);
                        textViewQuestion.setText(R.string.quiz_error_question);
                        buttonValidate.setEnabled(false);
                        Toast.makeText(QuizActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void validateAnswer() {
        if (isCheckingAnswer || currentQuestion.isEmpty()) {
            return;
        }
        String answer = editTextAnswer.getText() != null
                ? editTextAnswer.getText().toString().trim()
                : "";
        if (answer.isEmpty()) {
            Toast.makeText(this, R.string.quiz_answer_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        isCheckingAnswer = true;
        if (!round.beginAnswer()) {
            isCheckingAnswer = false;
            return;
        }
        buttonValidate.setEnabled(false);
        buttonNewQuestion.setEnabled(false);
        progressBarQuiz.setVisibility(View.VISIBLE);
        textViewFeedback.setVisibility(View.GONE);

        String userMessage = "Question : " + currentQuestion
                + "\nRéponse de l'utilisateur : " + answer;

        GeminiHelper.sendMessage(this, SYSTEM_VALIDATE, userMessage,
                new GeminiHelper.GeminiCallback() {
                    @Override
                    public void onSuccess(String text) {
                        if (isDestroyed() || isFinishing()) return;
                        isCheckingAnswer = false;
                        progressBarQuiz.setVisibility(View.GONE);
                        buttonValidate.setEnabled(false);
                        buttonNewQuestion.setEnabled(true);
                        round.answerCompleted();

                        String verdict = text.trim();
                        if (!"CORRECT".equalsIgnoreCase(verdict)
                                && !"INCORRECT".equalsIgnoreCase(verdict)) {
                            round.reset();
                            round.questionReady();
                            buttonValidate.setEnabled(true);
                            Toast.makeText(QuizActivity.this,
                                    "Correction indisponible, veuillez réessayer.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        boolean correct = "CORRECT".equalsIgnoreCase(verdict);
                        if (correct) {
                            showFeedback(getString(R.string.quiz_correct), true);
                            FirestoreHelper.addPoints(POINTS_PER_CORRECT,
                                    new FirestoreHelper.ScoreCallback() {
                                        @Override
                                        public void onScore(int totalScore) {
                                            updateScoreDisplay(totalScore);
                                        }

                                        @Override
                                        public void onError(String message) {
                                            Toast.makeText(QuizActivity.this,
                                                    message, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            showFeedback(getString(R.string.quiz_incorrect), false);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (isDestroyed() || isFinishing()) return;
                        isCheckingAnswer = false;
                        round.answerFailed();
                        buttonNewQuestion.setEnabled(true);
                        progressBarQuiz.setVisibility(View.GONE);
                        buttonValidate.setEnabled(true);
                        Toast.makeText(QuizActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showFeedback(String message, boolean success) {
        textViewFeedback.setText(message);
        textViewFeedback.setTextColor(success
                ? Color.parseColor("#4CAF50")
                : Color.parseColor("#EF5350"));
        textViewFeedback.setVisibility(View.VISIBLE);
    }
}
