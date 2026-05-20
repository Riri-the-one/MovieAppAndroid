package com.example.movies_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Inscription : nom d'utilisateur, e-mail et mot de passe + profil Firestore.
 */
public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressBarRegister);

        TextInputEditText editUsername = findViewById(R.id.editTextUsernameRegister);
        TextInputEditText editEmail = findViewById(R.id.editTextEmailRegister);
        TextInputEditText editPassword = findViewById(R.id.editTextPasswordRegister);
        TextInputEditText editConfirm = findViewById(R.id.editTextPasswordConfirm);
        MaterialButton buttonRegister = findViewById(R.id.buttonRegister);
        MaterialButton buttonGoLogin = findViewById(R.id.buttonGoLogin);

        buttonGoLogin.setOnClickListener(v -> finish());

        buttonRegister.setOnClickListener(v -> {
            String username = textOf(editUsername);
            String email = textOf(editEmail);
            String password = textOf(editPassword);
            String confirm = textOf(editConfirm);

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.auth_fields_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(this, R.string.password_mismatch, Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, R.string.password_too_short, Toast.LENGTH_SHORT).show();
                return;
            }

            attemptRegister(username, email, password);
        });
    }

    /**
     * Crée le compte Auth, ou se connecte si l'e-mail existe déjà (1ère tentative réussie).
     */
    private void attemptRegister(String username, String email, String password) {
        setLoading(true);
        firebaseAuth.signOut();
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        finishRegistration(username, email, true);
                        return;
                    }
                    Exception ex = task.getException();
                    if (AuthErrorHelper.isEmailAlreadyInUse(ex)) {
                        tryLoginWithExistingAccount(username, email, password);
                    } else {
                        setLoading(false);
                        Toast.makeText(this,
                                AuthErrorHelper.getMessage(this, ex),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Compte déjà dans Authentication (ex. 1ère inscription réussie mais écran d'erreur). */
    private void tryLoginWithExistingAccount(String username, String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(signInTask -> {
                    if (signInTask.isSuccessful()) {
                        Toast.makeText(this,
                                R.string.account_exists_connecting,
                                Toast.LENGTH_SHORT).show();
                        finishRegistration(username, email, false);
                    } else {
                        setLoading(false);
                        showEmailAlreadyInUseDialog();
                    }
                });
    }

    /** Enregistre le profil Firestore puis ouvre l'app (même si Firestore échoue). */
    private void finishRegistration(String username, String email, boolean newAccount) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            setLoading(false);
            Toast.makeText(this, R.string.register_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        FirestoreHelper.saveUserProfile(user.getUid(), username, email,
                new FirestoreHelper.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        setLoading(false);
                        Toast.makeText(RegisterActivity.this,
                                newAccount
                                        ? R.string.register_success
                                        : R.string.register_success_existing,
                                Toast.LENGTH_SHORT).show();
                        goToMain();
                    }

                    @Override
                    public void onError(String message) {
                        setLoading(false);
                        // Auth OK : on laisse entrer dans l'app (souvent règles Firestore)
                        Toast.makeText(RegisterActivity.this,
                                R.string.register_success_profile_pending,
                                Toast.LENGTH_LONG).show();
                        goToMain();
                    }
                });
    }

    private void showEmailAlreadyInUseDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error_email_in_use_title)
                .setMessage(R.string.error_email_in_use_wrong_password)
                .setPositiveButton(R.string.action_go_login, (d, w) -> finish())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.buttonRegister).setEnabled(!loading);
    }

    private static String textOf(TextInputEditText editText) {
        if (editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }
}
