package com.example.movies_app;

/**
 * Modèle d'un film issu de l'API TMDB.
 */
public class MyMovieData {

    private final int id;
    private final String title;
    private final String overview;
    private final String releaseDate;
    private final double voteAverage;
    private final String posterPath;
    private final String backdropPath;

    public MyMovieData(int id, String title, String overview, String releaseDate,
                       double voteAverage, String posterPath, String backdropPath) {
        this.id = id;
        this.title = title;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.voteAverage = voteAverage;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getOverview() {
        return overview;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public double getVoteAverage() {
        return voteAverage;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    /** Note formatée pour l'affichage (ex. 8.4). */
    public String getFormattedRating() {
        return String.format(java.util.Locale.FRANCE, "%.1f", voteAverage);
    }

    /** Année extraite de la date TMDB (yyyy-MM-dd). */
    public String getReleaseYear() {
        if (releaseDate == null || releaseDate.length() < 4) {
            return "";
        }
        return releaseDate.substring(0, 4);
    }
}
