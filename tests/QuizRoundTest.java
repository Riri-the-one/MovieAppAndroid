package com.example.movies_app;

/** Dependency-free regression checks; run with scripts/test-quiz.ps1. */
public final class QuizRoundTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        QuizRound round = new QuizRound();
        check(!round.beginAnswer(), "Cannot answer before a question exists");
        round.questionReady();
        check(round.beginAnswer(), "First submission accepted");
        check(round.isChecking(), "Pending answer blocks question generation");
        check(!round.beginAnswer(), "Double tap must not send another answer");
        round.answerFailed();
        check(round.beginAnswer(), "Network failure allows retry");
        round.answerCompleted();
        check(!round.beginAnswer(), "Completed answer cannot award points twice");
        round.reset();
        check(!round.beginAnswer(), "Generating another question clears readiness");
        round.questionReady();
        check(round.beginAnswer(), "New question accepts an answer");
        round.answerCompleted();
        check(!round.isChecking(), "Completed round is not pending");
        System.out.println("PASS: " + checks + " quiz round regression checks");
    }
}
