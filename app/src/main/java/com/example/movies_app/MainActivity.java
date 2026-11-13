package com.example.movies_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Écran principal : films TMDB, drawer profil et assistant IA.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int GRID_SPAN_COUNT = 2;
    private static final int LOAD_MORE_THRESHOLD = 4;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ShapeableImageView imageViewDrawerAvatar;
    private TextView textViewProfileName;
    private TextView textViewProfileEmail;

    private CircularProgressIndicator progressBar;
    private CircularProgressIndicator progressBarLoadMore;
    private View shimmerLoading;
    private TextView textViewError;
    private ListenerRegistration profileListener;
    private MyMovieAdapter movieAdapter;
    private RequestQueue requestQueue;
    private GridLayoutManager gridLayoutManager;

    private final List<MyMovieData> allMovies = new ArrayList<>();

    private int currentPage = 1;
    private int totalPages = 1;
    private boolean isLoading = false;
    private String activeSearchQuery = "";

    @Override
    protected void onStart() {
        super.onStart();
        AuthHelper.validateSession(new AuthHelper.SessionCallback() {
            @Override
            public void onValidSession(FirebaseUser user) {
                startProfileRealtimeSync();
            }

            @Override
            public void onNoSession() {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        View header = navigationView.getHeaderView(0);
        imageViewDrawerAvatar = header.findViewById(R.id.imageViewDrawerAvatar);
        textViewProfileName = header.findViewById(R.id.textViewProfileName);
        textViewProfileEmail = header.findViewById(R.id.textViewProfileEmail);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        progressBarLoadMore = findViewById(R.id.progressBarLoadMore);
        shimmerLoading = findViewById(R.id.shimmerLoading);
        textViewError = findViewById(R.id.textViewError);
        SearchView searchView = findViewById(R.id.searchView);
        FloatingActionButton fabChatbot = findViewById(R.id.fabChatbot);

        gridLayoutManager = new GridLayoutManager(this, GRID_SPAN_COUNT);
        recyclerView.setLayoutManager(gridLayoutManager);
        movieAdapter = new MyMovieAdapter(this);
        recyclerView.setAdapter(movieAdapter);

        setupSearchView(searchView);
        setupInfiniteScroll(recyclerView);

        fabChatbot.setOnClickListener(v -> openChatbot());
        startProfileRealtimeSync();

        requestQueue = Volley.newRequestQueue(this);
        loadFirstPage();
    }

    @Override
    protected void onDestroy() {
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
        super.onDestroy();
    }

    /** Synchronise nom et e-mail du drawer en temps réel depuis Firestore users/{uid}. */
    private void startProfileRealtimeSync() {
        if (profileListener != null) {
            profileListener.remove();
        }
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) {
            return;
        }
        textViewProfileName.setText(R.string.loading_profile);
        textViewProfileEmail.setText(authUser.getEmail() != null ? authUser.getEmail() : "");

        profileListener = FirestoreHelper.listenToUserProfile(
                new FirestoreHelper.ProfileListener() {
                    @Override
                    public void onProfileUpdated(FirestoreHelper.UserProfile profile) {
                        textViewProfileName.setText(profile.getDisplayName());
                        String email = profile.getEmail();
                        if (email == null || email.isEmpty()) {
                            email = authUser.getEmail();
                        }
                        textViewProfileEmail.setText(email != null ? email : "");
                        ProfileImageHelper.displayInImageView(
                                imageViewDrawerAvatar,
                                profile.getProfileImageBase64());
                    }

                    @Override
                    public void onError(String message) {
                        textViewProfileName.setText(
                                authUser.getEmail() != null ? authUser.getEmail() : "Cinéphile");
                        textViewProfileEmail.setText(authUser.getEmail());
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_profile) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_manage_profile) {
            startActivity(new Intent(this, ManageProfileActivity.class));
        } else if (id == R.id.nav_preferences) {
            startActivity(new Intent(this, PreferencesActivity.class));
        } else if (id == R.id.nav_quiz) {
            startActivity(new Intent(this, QuizActivity.class));
        } else if (id == R.id.nav_logout) {
            logout();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void openChatbot() {
        startActivity(new Intent(this, ChatbotActivity.class));
    }

    private void logout() {
        AuthHelper.signOut();
        IntentHelper.goToLoginClearingStack(this);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startProfileRealtimeSync();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void setupInfiniteScroll(RecyclerView recyclerView) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || isLoading || !activeSearchQuery.isEmpty()) {
                    return;
                }
                int total = gridLayoutManager.getItemCount();
                int lastVisible = gridLayoutManager.findLastVisibleItemPosition();
                if (lastVisible >= total - LOAD_MORE_THRESHOLD && currentPage < totalPages) {
                    fetchPopularMovies(currentPage + 1, false);
                }
            }
        });
    }

    private void setupSearchView(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterMovies(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterMovies(newText);
                return true;
            }
        });
    }

    private void filterMovies(String query) {
        if (query == null || query.trim().isEmpty()) {
            activeSearchQuery = "";
            movieAdapter.setMovies(new ArrayList<>(allMovies));
            return;
        }
        activeSearchQuery = query.trim().toLowerCase(Locale.FRANCE);
        List<MyMovieData> filtered = new ArrayList<>();
        for (MyMovieData movie : allMovies) {
            if (movie.getTitle().toLowerCase(Locale.FRANCE).contains(activeSearchQuery)
                    || (movie.getOverview() != null
                    && movie.getOverview().toLowerCase(Locale.FRANCE).contains(activeSearchQuery))) {
                filtered.add(movie);
            }
        }
        movieAdapter.setMovies(filtered);
    }

    private void loadFirstPage() {
        currentPage = 1;
        totalPages = 1;
        allMovies.clear();
        fetchPopularMovies(1, true);
    }

    private void fetchPopularMovies(int page, boolean resetList) {
        String apiKey = BuildConfig.TMDB_API_KEY.trim();
        if (apiKey.isEmpty()) {
            showError(getString(R.string.tmdb_key_missing));
            return;
        }
        if (isLoading) {
            return;
        }

        isLoading = true;
        if (resetList) {
            showLoading(true);
        } else {
            progressBarLoadMore.setVisibility(View.VISIBLE);
        }

        String url = TmdbApi.getPopularMoviesUrl(page) + "&api_key=" + apiKey;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> parseMoviesResponse(response, page, resetList),
                error -> {
                    isLoading = false;
                    showLoading(false);
                    progressBarLoadMore.setVisibility(View.GONE);
                    if (resetList) {
                        showError(getString(R.string.error_loading_movies) + "\n" + parseVolleyError(error));
                    } else {
                        Toast.makeText(this, parseVolleyError(error), Toast.LENGTH_SHORT).show();
                    }
                }
        );
        requestQueue.add(request);
    }

    private void parseMoviesResponse(JSONObject response, int page, boolean resetList) {
        isLoading = false;
        showLoading(false);
        progressBarLoadMore.setVisibility(View.GONE);

        try {
            totalPages = response.optInt("total_pages", 1);
            currentPage = page;

            JSONArray results = response.getJSONArray("results");
            List<MyMovieData> pageMovies = new ArrayList<>();

            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.getJSONObject(i);
                pageMovies.add(new MyMovieData(
                        item.getInt("id"),
                        item.optString("title", "Sans titre"),
                        item.optString("overview", ""),
                        item.optString("release_date", ""),
                        item.optDouble("vote_average", 0.0),
                        item.optString("poster_path", null),
                        item.optString("backdrop_path", null)
                ));
            }

            if (resetList) {
                allMovies.clear();
                allMovies.addAll(pageMovies);
                if (allMovies.isEmpty()) {
                    showError(getString(R.string.error_loading_movies));
                } else {
                    textViewError.setVisibility(View.GONE);
                    if (activeSearchQuery.isEmpty()) {
                        movieAdapter.setMovies(new ArrayList<>(allMovies));
                    } else {
                        filterMovies(activeSearchQuery);
                    }
                }
            } else {
                allMovies.addAll(pageMovies);
                if (activeSearchQuery.isEmpty()) {
                    movieAdapter.appendMovies(pageMovies);
                } else {
                    filterMovies(activeSearchQuery);
                }
            }
        } catch (JSONException e) {
            if (resetList) {
                showError(getString(R.string.error_loading_movies));
            }
        }
    }

    private void showLoading(boolean loading) {
        if (loading) {
            shimmerLoading.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            textViewError.setVisibility(View.GONE);
            Animation pulse = AnimationUtils.loadAnimation(this, R.anim.shimmer_pulse);
            shimmerLoading.startAnimation(pulse);
        } else {
            shimmerLoading.clearAnimation();
            shimmerLoading.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        textViewError.setText(message);
        textViewError.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String parseVolleyError(com.android.volley.VolleyError error) {
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                String body = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                JSONObject json = new JSONObject(body);
                return json.optString("status_message", body);
            } catch (Exception e) {
                return "HTTP " + error.networkResponse.statusCode;
            }
        }
        if (error.getMessage() != null) {
            return error.getMessage();
        }
        return "Connexion impossible";
    }
}
