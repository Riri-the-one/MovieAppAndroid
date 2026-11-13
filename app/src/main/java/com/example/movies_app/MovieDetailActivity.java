package com.example.movies_app;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Random;

/**
 * Écran détails : backdrop séparé, affiche, trailer YouTube, cinéma et billet QR.
 */
public class MovieDetailActivity extends AppCompatActivity {

    private static final String TAG = "MovieDetailActivity";
    private static final int REQUEST_LOCATION_PERMISSION = 201;

    public static final String EXTRA_MOVIE_ID = "EXTRA_MOVIE_ID";
    public static final String EXTRA_MOVIE_NAME = "EXTRA_MOVIE_NAME";
    public static final String EXTRA_MOVIE_DATE = "EXTRA_MOVIE_DATE";
    public static final String EXTRA_MOVIE_OVERVIEW = "EXTRA_MOVIE_OVERVIEW";
    public static final String EXTRA_MOVIE_RATING = "EXTRA_MOVIE_RATING";
    public static final String EXTRA_POSTER_PATH = "EXTRA_POSTER_PATH";
    public static final String EXTRA_BACKDROP_PATH = "EXTRA_BACKDROP_PATH";

    private String movieTitle;
    private int movieId;
    private String posterPath;
    private RequestQueue requestQueue;
    private ImageButton buttonFavorite;
    private MaterialButton buttonLike;
    private MaterialButton buttonDislike;
    private boolean isFavorite;
    private String currentFeedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_movie_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        requestQueue = Volley.newRequestQueue(this);

        movieId = getIntent().getIntExtra(EXTRA_MOVIE_ID, -1);
        movieTitle = getIntent().getStringExtra(EXTRA_MOVIE_NAME);
        String releaseDate = getIntent().getStringExtra(EXTRA_MOVIE_DATE);
        String overview = getIntent().getStringExtra(EXTRA_MOVIE_OVERVIEW);
        float rating = getIntent().getFloatExtra(EXTRA_MOVIE_RATING, 0f);
        posterPath = getIntent().getStringExtra(EXTRA_POSTER_PATH);
        String backdropPath = getIntent().getStringExtra(EXTRA_BACKDROP_PATH);

        ImageView imageViewBackdrop = findViewById(R.id.imageViewBackdrop);
        ImageView imageViewPoster = findViewById(R.id.imageViewDetailPoster);
        TextView textViewTitle = findViewById(R.id.textViewDetailTitle);
        TextView textViewYear = findViewById(R.id.textViewDetailYear);
        TextView textViewRating = findViewById(R.id.textViewDetailRating);
        TextView textViewOverview = findViewById(R.id.textViewDetailOverview);
        MaterialButton buttonFindCinema = findViewById(R.id.buttonFindCinema);
        MaterialButton buttonReserve = findViewById(R.id.buttonReserve);
        MaterialButton buttonTrailer = findViewById(R.id.buttonTrailer);
        MaterialToolbar toolbarDetail = findViewById(R.id.toolbarDetail);
        buttonFavorite = findViewById(R.id.buttonFavorite);
        buttonLike = findViewById(R.id.buttonLike);
        buttonDislike = findViewById(R.id.buttonDislike);

        toolbarDetail.setNavigationOnClickListener(v -> finish());
        setupFavoriteButton();
        setupFeedbackButtons();

        textViewTitle.setText(movieTitle);
        textViewYear.setText(formatReleaseDate(releaseDate));
        textViewRating.setText(String.format(Locale.FRANCE, "%.1f / 10", rating));

        if (overview == null || overview.trim().isEmpty()) {
            textViewOverview.setText(R.string.no_overview);
        } else {
            textViewOverview.setText(overview);
        }

        Glide.with(this)
                .load(TmdbApi.getBackdropUrl(backdropPath))
                .placeholder(R.color.md_surface_variant)
                .error(R.color.md_surface_variant)
                .centerCrop()
                .into(imageViewBackdrop);

        Glide.with(this)
                .load(TmdbApi.getPosterUrl(posterPath))
                .placeholder(R.color.md_surface_variant)
                .error(R.color.md_surface_variant)
                .centerCrop()
                .into(imageViewPoster);

        buttonTrailer.setOnClickListener(v -> fetchAndPlayTrailer());
        buttonFindCinema.setOnClickListener(v -> openCinemaMap());
        buttonReserve.setOnClickListener(v -> showElectronicTicketDialog());
    }

    /** Vérifie l'état favori Firestore et bascule au clic sur le cœur. */
    private void setupFavoriteButton() {
        if (movieId < 0) {
            buttonFavorite.setVisibility(View.GONE);
            return;
        }
        refreshFavoriteIcon(false);
        FirestoreHelper.checkFavorite(movieId, new FirestoreHelper.FavoriteCallback() {
            @Override
            public void onResult(boolean favorite) {
                isFavorite = favorite;
                refreshFavoriteIcon(favorite);
            }

            @Override
            public void onError(String message) {
                // Silencieux si non connecté sur cet écran (normalement connecté)
            }
        });

        buttonFavorite.setOnClickListener(v -> {
            buttonFavorite.setEnabled(false);
            if (isFavorite) {
                FirestoreHelper.removeFavorite(movieId, favoriteCallback(false));
            } else {
                String imageUrl = TmdbApi.getPosterUrl(posterPath);
                FirestoreHelper.addFavorite(movieId, movieTitle, imageUrl, favoriteCallback(true));
            }
        });
    }

    private FirestoreHelper.SimpleCallback favoriteCallback(boolean adding) {
        return new FirestoreHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                isFavorite = adding;
                refreshFavoriteIcon(isFavorite);
                buttonFavorite.setEnabled(true);
                Toast.makeText(MovieDetailActivity.this,
                        adding ? R.string.favorite_added : R.string.favorite_removed,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                buttonFavorite.setEnabled(true);
                Toast.makeText(MovieDetailActivity.this,
                        R.string.favorite_error, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void refreshFavoriteIcon(boolean filled) {
        buttonFavorite.setImageResource(filled
                ? R.drawable.ic_favorite_filled
                : R.drawable.ic_favorite_border);
    }

    /** Like / dislike enregistrés dans users_feedback pour les recommandations IA. */
    private void setupFeedbackButtons() {
        if (movieId < 0) {
            buttonLike.setVisibility(View.GONE);
            buttonDislike.setVisibility(View.GONE);
            return;
        }
        refreshFeedbackUi(null);
        FirestoreHelper.getFeedbackForMovie(movieId, new FirestoreHelper.FeedbackStateCallback() {
            @Override
            public void onResult(String choice) {
                currentFeedback = choice;
                refreshFeedbackUi(choice);
            }

            @Override
            public void onError(String message) {
                // Ignoré si hors ligne
            }
        });

        buttonLike.setOnClickListener(v -> saveFeedback(FirestoreHelper.FEEDBACK_LIKE));
        buttonDislike.setOnClickListener(v -> saveFeedback(FirestoreHelper.FEEDBACK_DISLIKE));
    }

    private void saveFeedback(String feedbackType) {
        String uid = FirestoreHelper.getCurrentUserId();
        if (uid == null) {
            Toast.makeText(this, R.string.feedback_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        buttonLike.setEnabled(false);
        buttonDislike.setEnabled(false);
        String imageUrl = TmdbApi.getPosterUrl(posterPath);

        FirestoreHelper.saveFeedback(movieId, movieTitle, imageUrl, feedbackType,
                new FirestoreHelper.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        currentFeedback = feedbackType;
                        refreshFeedbackUi(feedbackType);
                        buttonLike.setEnabled(true);
                        buttonDislike.setEnabled(true);
                        Toast.makeText(MovieDetailActivity.this,
                                FirestoreHelper.FEEDBACK_LIKE.equals(feedbackType)
                                        ? R.string.feedback_like_saved
                                        : R.string.feedback_dislike_saved,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Firestore feedback error: " + message);
                        buttonLike.setEnabled(true);
                        buttonDislike.setEnabled(true);
                        Toast.makeText(MovieDetailActivity.this,
                                getString(R.string.feedback_error_detail, message),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void refreshFeedbackUi(String choice) {
        boolean liked = FirestoreHelper.FEEDBACK_LIKE.equals(choice);
        boolean disliked = FirestoreHelper.FEEDBACK_DISLIKE.equals(choice);
        buttonLike.setAlpha(liked ? 1f : 0.6f);
        buttonDislike.setAlpha(disliked ? 1f : 0.6f);
    }

    /**
     * Récupère la bande-annonce TMDB (fr-FR puis en-US si vide)
     * et l'ouvre sur YouTube.
     */
    private void fetchAndPlayTrailer() {
        if (movieId < 0) {
            showTrailerNotFound();
            return;
        }

        String apiKey = BuildConfig.TMDB_API_KEY.trim();
        if (apiKey.isEmpty()) {
            Toast.makeText(this, R.string.tmdb_key_missing, Toast.LENGTH_SHORT).show();
            return;
        }

        buttonTrailerEnabled(false);
        // Beaucoup de films n'ont pas de vidéos en français sur TMDB → repli en anglais
        fetchTrailerForLanguage("fr-FR", apiKey, () ->
                fetchTrailerForLanguage("en-US", apiKey, this::showTrailerNotFound));
    }

    private void fetchTrailerForLanguage(String language, String apiKey, Runnable onFailure) {
        String url = TmdbApi.getMovieVideosUrl(movieId, language) + "&api_key=" + apiKey;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    String youtubeKey = extractTrailerKey(response);
                    if (youtubeKey != null) {
                        buttonTrailerEnabled(true);
                        openYoutubeTrailer(youtubeKey);
                    } else if (onFailure != null) {
                        onFailure.run();
                    } else {
                        showTrailerNotFound();
                    }
                },
                error -> {
                    if (onFailure != null) {
                        onFailure.run();
                    } else {
                        showTrailerNotFound();
                    }
                }
        );
        requestQueue.add(request);
    }

    private void showTrailerNotFound() {
        buttonTrailerEnabled(true);
        Toast.makeText(this, R.string.trailer_not_found, Toast.LENGTH_LONG).show();
    }

    /**
     * Extrait la meilleure clé YouTube : Trailer > Teaser > Clip > autre.
     */
    private String extractTrailerKey(JSONObject response) {
        try {
            JSONArray results = response.getJSONArray("results");
            if (results.length() == 0) {
                return null;
            }

            String[] typesPriority = {"Trailer", "Teaser", "Clip", "Featurette"};

            for (String type : typesPriority) {
                String official = findYoutubeByType(results, type, true);
                if (official != null) {
                    return official;
                }
            }
            for (String type : typesPriority) {
                String any = findYoutubeByType(results, type, false);
                if (any != null) {
                    return any;
                }
            }

            for (int i = 0; i < results.length(); i++) {
                JSONObject video = results.getJSONObject(i);
                if (isYoutube(video)) {
                    String key = readVideoKey(video);
                    if (key != null) {
                        return key;
                    }
                }
            }
        } catch (JSONException ignored) {
        }
        return null;
    }

    private String findYoutubeByType(JSONArray results, String type, boolean officialOnly)
            throws JSONException {
        for (int i = 0; i < results.length(); i++) {
            JSONObject video = results.getJSONObject(i);
            if (!isYoutube(video)) {
                continue;
            }
            if (!type.equalsIgnoreCase(video.optString("type"))) {
                continue;
            }
            if (officialOnly && !video.optBoolean("official", false)) {
                continue;
            }
            String key = readVideoKey(video);
            if (key != null) {
                return key;
            }
        }
        return null;
    }

    private boolean isYoutube(JSONObject video) {
        return "YouTube".equalsIgnoreCase(video.optString("site"));
    }

    private String readVideoKey(JSONObject video) {
        String key = video.optString("key", "").trim();
        return key.isEmpty() ? null : key;
    }

    private void openYoutubeTrailer(String videoKey) {
        if (videoKey == null || videoKey.isEmpty()) {
            showTrailerNotFound();
            return;
        }

        Uri uri = Uri.parse("https://www.youtube.com/watch?v=" + videoKey);

        // Tenter l'application YouTube
        Intent youtubeApp = new Intent(Intent.ACTION_VIEW, uri);
        youtubeApp.setPackage("com.google.android.youtube");
        try {
            startActivity(youtubeApp);
            return;
        } catch (ActivityNotFoundException ignored) {
        }

        // Navigateur ou autre lecteur vidéo
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            showTrailerNotFound();
        }
    }

    private void buttonTrailerEnabled(boolean enabled) {
        MaterialButton buttonTrailer = findViewById(R.id.buttonTrailer);
        buttonTrailer.setEnabled(enabled);
        buttonTrailer.setText(enabled
                ? R.string.watch_trailer
                : R.string.loading_trailer);
    }

    private String formatReleaseDate(String releaseDate) {
        if (releaseDate == null || releaseDate.isEmpty()) {
            return getString(R.string.release_unknown);
        }
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE);
            SimpleDateFormat output = new SimpleDateFormat("d MMMM yyyy", Locale.FRANCE);
            return "Sortie : " + output.format(input.parse(releaseDate));
        } catch (ParseException e) {
            return "Sortie : " + releaseDate;
        }
    }

    private void openCinemaMap() {
        if (hasLocationPermission()) {
            startActivity(new Intent(this, CinemaMapActivity.class));
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (hasLocationPermission()) {
                startActivity(new Intent(this, CinemaMapActivity.class));
            } else {
                Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showElectronicTicketDialog() {
        try {
            String seat = generateRandomSeat();
            String qrContent = "BILLET|" + movieTitle + "|" + seat + "|" + System.currentTimeMillis();

            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap qrBitmap = encoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 512, 512);

            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_e_ticket, null);
            TextView textMovie = dialogView.findViewById(R.id.textViewTicketMovie);
            TextView textSeat = dialogView.findViewById(R.id.textViewTicketSeat);
            ImageView imageQr = dialogView.findViewById(R.id.imageViewQrTicket);

            textMovie.setText(movieTitle);
            textSeat.setText(getString(R.string.e_ticket_seat, seat));
            imageQr.setImageBitmap(qrBitmap);

            new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        } catch (WriterException e) {
            Toast.makeText(this, "Impossible de générer le billet.", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateRandomSeat() {
        Random random = new Random();
        char row = (char) ('A' + random.nextInt(8));
        int number = 1 + random.nextInt(12);
        return row + String.valueOf(number);
    }
}
