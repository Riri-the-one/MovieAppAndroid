package com.example.movies_app;

/** Local round guard: one completed answer per generated question. */
final class QuizRound {
    private boolean ready;
    private boolean checking;
    private boolean answered;

    void reset() { ready = false; checking = false; answered = false; }
    void questionReady() { ready = true; }
    boolean beginAnswer() {
        if (!ready || checking || answered) return false;
        checking = true;
        return true;
    }
    void answerFailed() { checking = false; }
    void answerCompleted() { checking = false; answered = true; }
    boolean isChecking() { return checking; }
    boolean isAnswered() { return answered; }
}
