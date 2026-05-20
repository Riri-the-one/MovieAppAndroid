package com.example.movies_app;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

/**
 * Messages d'erreur Firebase Auth en français.
 */
public final class AuthErrorHelper {

    private AuthErrorHelper() {
    }

    public static boolean isEmailAlreadyInUse(Exception exception) {
        if (exception instanceof FirebaseAuthUserCollisionException) {
            return true;
        }
        if (exception != null && exception.getMessage() != null) {
            String lower = exception.getMessage().toLowerCase();
            return lower.contains("already in use") || lower.contains("déjà utilis");
        }
        return false;
    }

    public static String getMessage(Context context, Exception exception) {
        if (exception == null) {
            return context.getString(R.string.auth_error_generic);
        }
        if (exception instanceof FirebaseAuthUserCollisionException
                || isEmailAlreadyInUse(exception)) {
            return context.getString(R.string.error_email_in_use);
        }
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            return context.getString(R.string.password_too_short);
        }
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return context.getString(R.string.login_failed);
        }
        if (exception instanceof FirebaseAuthInvalidUserException) {
            return context.getString(R.string.error_user_not_found);
        }
        return context.getString(R.string.auth_error_generic);
    }
}
