package com.example.movies_app;

import android.content.Context;
import android.content.Intent;

/**
 * Navigation utilitaire entre écrans d'authentification.
 */
public final class IntentHelper {

    private IntentHelper() {
    }

    public static void goToLoginClearingStack(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
