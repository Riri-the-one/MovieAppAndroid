package com.example.movies_app;

import com.google.firebase.firestore.DocumentSnapshot;
/**
 * Film liké ou disliké (Firestore users_feedback).
 * Champs Firestore : movieTitle, image, movieId, feedbackType, uid.
 */
public class FeedbackMovie {

    private final int movieId;
    private final String movieTitle;
    private final String image;

    public FeedbackMovie(int movieId, String movieTitle, String image) {
        this.movieId = movieId;
        this.movieTitle = movieTitle != null ? movieTitle : "Film";
        this.image = image != null ? image : "";
    }

    /** Construit l'objet depuis un document users_feedback. */
    public static FeedbackMovie fromDocument(DocumentSnapshot doc) {
        int movieId = 0;
        Object rawId = doc.get("movieId");
        if (rawId instanceof Number) {
            movieId = ((Number) rawId).intValue();
        } else if (rawId instanceof String) {
            try {
                movieId = Integer.parseInt((String) rawId);
            } catch (NumberFormatException ignored) {
            }
        }

        String movieTitle = doc.getString("movieTitle");
        if (movieTitle == null || movieTitle.isEmpty()) {
            movieTitle = doc.getString("title");
        }

        String image = doc.getString("image");

        return new FeedbackMovie(movieId, movieTitle, image);
    }

    public int getMovieId() {
        return movieId;
    }

    /** Titre affiché (champ Firestore movieTitle). */
    public String getMovieTitle() {
        return movieTitle;
    }

    /** Alias pour l'adapter. */
    public String getTitle() {
        return movieTitle;
    }

    /** URL affiche TMDB (champ Firestore image). */
    public String getImage() {
        return image;
    }

    /** Alias pour l'adapter. */
    public String getImageUrl() {
        return image;
    }
}
