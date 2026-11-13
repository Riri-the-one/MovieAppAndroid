package com.example.movies_app;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;

/**
 * Session Firebase : validation, déconnexion et suppression de compte.
 */
public final class AuthHelper {

    public interface SessionCallback {
        void onValidSession(FirebaseUser user);

        void onNoSession();
    }

    public interface DeleteAccountCallback {
        void onSuccess();

        /** needsReLogin = true si Firebase exige une reconnexion récente. */
        void onError(String message, boolean needsReLogin);
    }

    private AuthHelper() {
    }

    public static void validateSession(SessionCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onNoSession();
            return;
        }

        user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    FirebaseUser current = auth.getCurrentUser();
                    if (current != null) {
                        callback.onValidSession(current);
                    } else {
                        auth.signOut();
                        callback.onNoSession();
                    }
                })
                .addOnFailureListener(e -> {
                    auth.signOut();
                    callback.onNoSession();
                });
    }

    public static void signOut() {
        FirebaseAuth.getInstance().signOut();
    }

    /**
     * Supprime le compte Authentication + toutes les données Firestore liées.
     */
    public static void deleteAccount(DeleteAccountCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Non connecté", false);
            return;
        }

        String uid = user.getUid();
        if (user.getMetadata() == null
                || System.currentTimeMillis() - user.getMetadata().getLastSignInTimestamp() > 5 * 60_000L) {
            callback.onError("Reconnectez-vous puis réessayez de supprimer le compte.", true);
            return;
        }
        FirestoreHelper.deleteAllUserData(uid, new FirestoreHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                deleteAuthUser(user, auth, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError("Suppression des données interrompue : " + message, false);
            }
        });
    }

    private static void deleteAuthUser(FirebaseUser user, FirebaseAuth auth,
                                       DeleteAccountCallback callback) {
        user.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                auth.signOut();
                callback.onSuccess();
                return;
            }
            Exception ex = task.getException();
            boolean needsReLogin = ex instanceof FirebaseAuthRecentLoginRequiredException;
            String message = needsReLogin
                    ? "Reconnectez-vous puis réessayez de supprimer le compte."
                    : (ex != null ? ex.getMessage() : "Suppression impossible");
            callback.onError(message, needsReLogin);
        });
    }
}
