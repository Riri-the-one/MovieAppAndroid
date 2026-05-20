package com.example.movies_app;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Accès Firestore : profils, favoris, feedback et scores.
 */
public final class FirestoreHelper {

    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_FAVORITES = "users_favorites";
    public static final String COLLECTION_FEEDBACK = "users_feedback";
    /** Sous-collection users/{uid}/feedback — règles Firestore par chemin. */
    public static final String SUBCOLLECTION_USER_FEEDBACK = "feedback";
    public static final String COLLECTION_SCORES = "users_scores";

    public static final String FEEDBACK_LIKE = "like";
    public static final String FEEDBACK_DISLIKE = "dislike";

    public interface FavoriteCallback {
        void onResult(boolean isFavorite);

        void onError(String message);
    }

    public interface ScoreCallback {
        void onScore(int totalScore);

        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();

        void onError(String message);
    }

    public interface ProfileCallback {
        void onProfile(UserProfile profile);

        void onError(String message);
    }

    public interface FeedbackStateCallback {
        void onResult(String currentChoice);

        void onError(String message);
    }

    public interface FeedbackListsCallback {
        void onLists(List<String> likedTitles, List<String> dislikedTitles);

        void onError(String message);
    }

    public interface FeedbackMoviesCallback {
        void onMovies(List<FeedbackMovie> movies);

        void onError(String message);
    }

    public interface AllFeedbackCallback {
        void onResult(List<FeedbackMovie> liked, List<FeedbackMovie> disliked);

        void onError(String message);
    }

    public interface ProfileListener {
        void onProfileUpdated(UserProfile profile);

        void onError(String message);
    }

    /** Profil utilisateur stocké dans Firestore. */
    public static class UserProfile {
        private final String userId;
        private final String username;
        private final String email;
        private final String profileImageBase64;

        public UserProfile(String userId, String username, String email) {
            this(userId, username, email, null);
        }

        public UserProfile(String userId, String username, String email, String profileImageBase64) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.profileImageBase64 = profileImageBase64;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getProfileImageBase64() {
            return profileImageBase64;
        }

        public String getDisplayName() {
            if (username != null && !username.isEmpty()) {
                return username;
            }
            return email != null ? email : "Cinéphile";
        }
    }

    private static final FirebaseFirestore DB = FirebaseFirestore.getInstance();

    private FirestoreHelper() {
    }

    public static FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public static String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /** Crée le document profil seulement s'il n'existe pas encore. */
    public static void ensureUserProfile(String userId, String username, String email, SimpleCallback callback) {
        DB.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onSuccess();
                    } else {
                        saveUserProfile(userId, username, email, callback);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /** Enregistre le profil à l'inscription. */
    public static void saveUserProfile(String userId, String username, String email, SimpleCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", userId);
        data.put("userId", userId);
        data.put("username", username);
        data.put("email", email);

        DB.collection(COLLECTION_USERS)
                .document(userId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /** Met à jour le profil (nom, photo Base64) avec merge. */
    public static void updateUserProfile(String userId, String username, String email,
                                         String profileImageBase64, SimpleCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", userId);
        data.put("userId", userId);
        if (username != null) {
            data.put("username", username);
        }
        if (email != null) {
            data.put("email", email);
        }
        if (profileImageBase64 != null) {
            data.put("profileImageBase64", profileImageBase64);
        }

        DB.collection(COLLECTION_USERS)
                .document(userId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /** Charge le profil depuis Firestore (doc users/{uid}). */
    public static void getUserProfile(ProfileCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("Non connecté");
            return;
        }
        FirebaseUser authUser = getCurrentUser();
        DB.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    String username = doc.exists() ? doc.getString("username") : null;
                    String email = doc.exists() ? doc.getString("email") : null;
                    if (email == null && authUser != null) {
                        email = authUser.getEmail();
                    }
                    if (username == null || username.isEmpty()) {
                        username = authUser != null && authUser.getDisplayName() != null
                                ? authUser.getDisplayName()
                                : "Cinéphile";
                    }
                    String profileImageBase64 = doc.exists()
                            ? doc.getString("profileImageBase64") : null;
                    callback.onProfile(new UserProfile(userId, username, email, profileImageBase64));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private static boolean belongsToUser(QueryDocumentSnapshot doc, String uid) {
        String docUid = doc.getString("uid");
        if (uid.equals(docUid)) {
            return true;
        }
        String docUserId = doc.getString("userId");
        return uid.equals(docUserId);
    }

    private static String favoriteDocId(String userId, int movieId) {
        return userId + "_" + movieId;
    }

    private static String feedbackDocId(String userId, int movieId) {
        return userId + "_" + movieId;
    }

    public static void checkFavorite(int movieId, FavoriteCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("Non connecté");
            return;
        }
        DB.collection(COLLECTION_FAVORITES)
                .document(favoriteDocId(userId, movieId))
                .get()
                .addOnSuccessListener(doc -> callback.onResult(doc.exists()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public static void addFavorite(int movieId, String title, String imageUrl, SimpleCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("Non connecté");
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("movieId", movieId);
        data.put("title", title);
        data.put("image", imageUrl != null ? imageUrl : "");

        DB.collection(COLLECTION_FAVORITES)
                .document(favoriteDocId(userId, movieId))
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public static void removeFavorite(int movieId, SimpleCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("Non connecté");
            return;
        }
        DB.collection(COLLECTION_FAVORITES)
                .document(favoriteDocId(userId, movieId))
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Enregistre un like/dislike avec structure normalisée + merge (évite les doublons).
     */
    public static void saveFeedback(int movieId, String movieTitle, String imageUrl,
                                    String feedbackType, SimpleCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("Non connecté");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("userId", uid);
        data.put("movieId", movieId);
        data.put("movieTitle", movieTitle != null ? movieTitle : "");
        data.put("title", movieTitle != null ? movieTitle : "");
        data.put("feedbackType", feedbackType);
        data.put("choice", feedbackType);
        data.put("image", imageUrl != null ? imageUrl : "");
        data.put("timestamp", FieldValue.serverTimestamp());

        DB.collection(COLLECTION_FEEDBACK)
                .document(feedbackDocId(uid, movieId))
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused ->
                        DB.collection(COLLECTION_USERS)
                                .document(uid)
                                .collection(SUBCOLLECTION_USER_FEEDBACK)
                                .document(String.valueOf(movieId))
                                .set(data, SetOptions.merge())
                                .addOnSuccessListener(v -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onSuccess()))
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Erreur Firestore"));
    }

    /**
     * Charge tous les avis (likes + dislikes) sans whereEqualTo (évite PERMISSION_DENIED sur list).
     */
    public static void loadAllUserFeedback(AllFeedbackCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("Non connecté");
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Non connecté");
            return;
        }
        user.getIdToken(true).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onError("Session invalide");
                return;
            }
            loadAllFromUserFeedbackSubcollection(uid, callback);
        });
    }

    private static void loadAllFromUserFeedbackSubcollection(String uid, AllFeedbackCallback callback) {
        DB.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_USER_FEEDBACK)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        deliverSplitFeedback(snapshot, callback);
                        return;
                    }
                    loadAllFromFeedbackDocIdRange(uid, callback);
                })
                .addOnFailureListener(e -> loadAllFromFeedbackDocIdRange(uid, callback));
    }

    /** Requête par id de document {uid}_{movieId} — compatible avec les règles docId.matches. */
    private static void loadAllFromFeedbackDocIdRange(String uid, AllFeedbackCallback callback) {
        String startDocId = uid + "_";
        String endDocId = uid + "_\uf8ff";
        DB.collection(COLLECTION_FEEDBACK)
                .orderBy(FieldPath.documentId())
                .startAt(startDocId)
                .endAt(endDocId)
                .get()
                .addOnSuccessListener(snapshot -> deliverSplitFeedback(snapshot, callback))
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Erreur Firestore"));
    }

    private static void deliverSplitFeedback(
            com.google.firebase.firestore.QuerySnapshot snapshot,
            AllFeedbackCallback callback) {
        List<FeedbackMovie> liked = new ArrayList<>();
        List<FeedbackMovie> disliked = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshot) {
            String type = readFeedbackType(doc);
            if (type == null) {
                continue;
            }
            FeedbackMovie movie = FeedbackMovie.fromDocument(doc);
            if (FEEDBACK_LIKE.equalsIgnoreCase(type)) {
                liked.add(movie);
            } else if (FEEDBACK_DISLIKE.equalsIgnoreCase(type)) {
                disliked.add(movie);
            }
        }
        callback.onResult(liked, disliked);
    }

    private static String readFeedbackType(DocumentSnapshot doc) {
        String type = doc.getString("feedbackType");
        if (type == null || type.isEmpty()) {
            type = doc.getString("choice");
        }
        return type;
    }

    private static String readMovieTitle(DocumentSnapshot doc) {
        String title = doc.getString("movieTitle");
        if (title == null || title.isEmpty()) {
            title = doc.getString("title");
        }
        return title;
    }

    private static FeedbackMovie documentToFeedbackMovie(QueryDocumentSnapshot doc) {
        return FeedbackMovie.fromDocument(doc);
    }

    private static List<FeedbackMovie> filterByType(List<QueryDocumentSnapshot> docs, String feedbackType) {
        List<FeedbackMovie> movies = new ArrayList<>();
        for (QueryDocumentSnapshot doc : docs) {
            if (feedbackType.equals(readFeedbackType(doc))) {
                movies.add(documentToFeedbackMovie(doc));
            }
        }
        return movies;
    }

    /**
     * Récupère tous les avis de l'utilisateur (uid puis userId), filtre localement par type.
     */
    public static void getUserFeedbackMovies(String feedbackType, FeedbackMoviesCallback callback) {
        loadAllUserFeedback(new AllFeedbackCallback() {
            @Override
            public void onResult(List<FeedbackMovie> liked, List<FeedbackMovie> disliked) {
                if (FEEDBACK_LIKE.equals(feedbackType)) {
                    callback.onMovies(liked);
                } else {
                    callback.onMovies(disliked);
                }
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /** Récupère le choix actuel (like / dislike) ou null. */
    public static void getFeedbackForMovie(int movieId, FeedbackStateCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("Non connecté");
            return;
        }
        DB.collection(COLLECTION_FEEDBACK)
                .document(feedbackDocId(uid, movieId))
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onResult(readFeedbackType(doc));
                    } else {
                        callback.onResult(null);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /** Listes des titres likés et dislikés pour le prompt Gemini. */
    public static void getUserFeedbackLists(FeedbackListsCallback callback) {
        getUserFeedbackMovies(FEEDBACK_LIKE, new FeedbackMoviesCallback() {
            @Override
            public void onMovies(List<FeedbackMovie> likedMovies) {
                List<String> liked = titlesFromMovies(likedMovies);
                getUserFeedbackMovies(FEEDBACK_DISLIKE, new FeedbackMoviesCallback() {
                    @Override
                    public void onMovies(List<FeedbackMovie> dislikedMovies) {
                        callback.onLists(liked, titlesFromMovies(dislikedMovies));
                    }

                    @Override
                    public void onError(String message) {
                        callback.onLists(liked, new ArrayList<>());
                    }
                });
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private static List<String> titlesFromMovies(List<FeedbackMovie> movies) {
        List<String> titles = new ArrayList<>();
        for (FeedbackMovie movie : movies) {
            if (movie.getTitle() != null && !movie.getTitle().isEmpty()) {
                titles.add(movie.getTitle());
            }
        }
        return titles;
    }

    /** Écoute le profil utilisateur en temps réel (drawer). */
    public static ListenerRegistration listenToUserProfile(ProfileListener listener) {
        String uid = getCurrentUserId();
        if (uid == null) {
            listener.onError("Non connecté");
            return null;
        }
        FirebaseUser authUser = getCurrentUser();
        return DB.collection(COLLECTION_USERS)
                .document(uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        String username = snapshot.getString("username");
                        String email = snapshot.getString("email");
                        String profileImageBase64 = snapshot.getString("profileImageBase64");
                        listener.onProfileUpdated(new UserProfile(
                                uid, username, email, profileImageBase64));
                    } else if (authUser != null) {
                        listener.onProfileUpdated(new UserProfile(
                                uid,
                                authUser.getDisplayName(),
                                authUser.getEmail(),
                                null));
                    }
                });
    }

    public static void getUserScore(ScoreCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("Non connecté");
            return;
        }
        DB.collection(COLLECTION_SCORES)
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long score = doc.getLong("totalScore");
                        callback.onScore(score != null ? score.intValue() : 0);
                    } else {
                        callback.onScore(0);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public static void addPoints(int points, ScoreCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("Non connecté");
            return;
        }
        DocumentReference ref = DB.collection(COLLECTION_SCORES).document(userId);
        ref.get().addOnSuccessListener(doc -> {
            int current = 0;
            if (doc.exists() && doc.getLong("totalScore") != null) {
                current = doc.getLong("totalScore").intValue();
            }
            int newScore = current + points;
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("totalScore", newScore);
            ref.set(data)
                    .addOnSuccessListener(unused -> callback.onScore(newScore))
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Supprime toutes les données Firestore de l'utilisateur (profil, favoris, avis, score).
     */
    public static void deleteAllUserData(String userId, SimpleCallback callback) {
        deleteCollectionWhereUserId(COLLECTION_FEEDBACK, userId, () ->
                deleteCollectionWhereUserId(COLLECTION_FAVORITES, userId, () ->
                        DB.collection(COLLECTION_USERS).document(userId).delete()
                                .addOnCompleteListener(taskUsers ->
                                        DB.collection(COLLECTION_SCORES).document(userId).delete()
                                                .addOnCompleteListener(taskScores ->
                                                        callback.onSuccess())
                                )));
    }

    private static void deleteCollectionWhereUserId(String collection, String userId, Runnable next) {
        DB.collection(collection)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        next.run();
                        return;
                    }
                    WriteBatch batch = DB.batch();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit().addOnCompleteListener(unused -> next.run());
                })
                .addOnFailureListener(e -> next.run());
    }

    /** Construit le prompt système personnalisé pour Gemini. */
    public static String buildPersonalizedSystemPrompt(List<String> liked, List<String> disliked) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un expert en cinéma francophone. ");
        sb.append("Recommande des films adaptés aux goûts de l'utilisateur. ");

        if (liked != null && !liked.isEmpty()) {
            sb.append("L'utilisateur aime : ");
            sb.append(String.join(", ", liked));
            sb.append(". ");
        } else {
            sb.append("L'utilisateur n'a pas encore indiqué de films aimés. ");
        }

        if (disliked != null && !disliked.isEmpty()) {
            sb.append("Il déteste ou n'aime pas : ");
            sb.append(String.join(", ", disliked));
            sb.append(". ");
            sb.append("Ne recommande jamais ces films ni des films similaires. ");
        }

        sb.append("Réponds en français, de façon concise et enthousiaste.");
        return sb.toString();
    }
}
