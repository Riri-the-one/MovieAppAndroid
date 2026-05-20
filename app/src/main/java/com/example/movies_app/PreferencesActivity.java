package com.example.movies_app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Préférences films : listes likés / dislikés depuis users_feedback.
 */
public class PreferencesActivity extends AppCompatActivity {

    private static final String TAG = "PreferencesActivity";
    private static final String COLLECTION_USERS_FEEDBACK = "users_feedback";
    private static final String TYPE_LIKE = "like";
    private static final String TYPE_DISLIKE = "dislike";

    private final List<FeedbackMovie> likesList = new ArrayList<>();
    private final List<FeedbackMovie> dislikesList = new ArrayList<>();

    private FeedbackMovieAdapter likedAdapter;
    private FeedbackMovieAdapter dislikedAdapter;
    private CircularProgressIndicator progressBar;
    private TextView textEmptyLiked;
    private TextView textEmptyDisliked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_preferences);

        MaterialToolbar toolbar = findViewById(R.id.toolbarPreferences);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBarPreferences);
        textEmptyLiked = findViewById(R.id.textEmptyLiked);
        textEmptyDisliked = findViewById(R.id.textEmptyDisliked);

        RecyclerView recyclerLiked = findViewById(R.id.recyclerLiked);
        RecyclerView recyclerDisliked = findViewById(R.id.recyclerDisliked);

        likedAdapter = new FeedbackMovieAdapter();
        dislikedAdapter = new FeedbackMovieAdapter();
        recyclerLiked.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerDisliked.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerLiked.setAdapter(likedAdapter);
        recyclerDisliked.setAdapter(dislikedAdapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        loadUserFeedback(user.getUid());
    }

    private void loadUserFeedback(String currentUid) {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_USERS_FEEDBACK)
                .whereEqualTo("uid", currentUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    likesList.clear();
                    dislikesList.clear();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String feedbackType = doc.getString("feedbackType");
                        if (feedbackType == null || feedbackType.isEmpty()) {
                            feedbackType = doc.getString("choice");
                        }
                        if (feedbackType == null) {
                            continue;
                        }

                        FeedbackMovie movie = FeedbackMovie.fromDocument(doc);

                        if (TYPE_LIKE.equalsIgnoreCase(feedbackType)) {
                            likesList.add(movie);
                        } else if (TYPE_DISLIKE.equalsIgnoreCase(feedbackType)) {
                            dislikesList.add(movie);
                        }
                    }

                    Log.d(TAG, "Chargés — likes: " + likesList.size()
                            + ", dislikes: " + dislikesList.size());

                    likedAdapter.setMovies(new ArrayList<>(likesList));
                    dislikedAdapter.setMovies(new ArrayList<>(dislikesList));
                    likedAdapter.notifyDataSetChanged();
                    dislikedAdapter.notifyDataSetChanged();

                    progressBar.setVisibility(View.GONE);
                    textEmptyLiked.setVisibility(likesList.isEmpty() ? View.VISIBLE : View.GONE);
                    textEmptyDisliked.setVisibility(dislikesList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Erreur chargement users_feedback : " + e.getMessage());
                    Toast.makeText(this,
                            getString(R.string.feedback_load_error, e.getMessage()),
                            Toast.LENGTH_LONG).show();

                    likesList.clear();
                    dislikesList.clear();
                    likedAdapter.setMovies(likesList);
                    dislikedAdapter.setMovies(dislikesList);
                    likedAdapter.notifyDataSetChanged();
                    dislikedAdapter.notifyDataSetChanged();
                    textEmptyLiked.setVisibility(View.VISIBLE);
                    textEmptyDisliked.setVisibility(View.VISIBLE);
                });
    }
}
