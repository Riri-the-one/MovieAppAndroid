package com.example.movies_app;

/**
 * Constantes et utilitaires pour l'API The Movie Database (TMDB).
 */
public final class TmdbApi {

    private static final String BASE_URL = "https://api.themoviedb.org/3/";
    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p/";

    private TmdbApi() {
    }

    /** URL des films populaires pour une page donnée (20 films par page). */
    public static String getPopularMoviesUrl(int page) {
        return BASE_URL + "movie/popular?language=fr-FR&page=" + page;
    }

    /** URL des vidéos (bandes-annonces) d'un film pour une langue donnée. */
    public static String getMovieVideosUrl(int movieId, String language) {
        return BASE_URL + "movie/" + movieId + "/videos?language=" + language;
    }

    public static String getPosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isEmpty()) {
            return null;
        }
        return IMAGE_BASE + "w500" + posterPath;
    }

    public static String getBackdropUrl(String backdropPath) {
        if (backdropPath == null || backdropPath.isEmpty()) {
            return null;
        }
        return IMAGE_BASE + "w780" + backdropPath;
    }
}
