package com.example.movies_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Écran de connexion — point d'entrée de l'application.
 */
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private ProgressBar progressBar;
    private View loginForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressBarLogin);
        loginForm = findViewById(R.id.loginForm);

        TextInputEditText editUsername = findViewById(R.id.editTextUsernameLogin);
        TextInputEditText editEmail = findViewById(R.id.editTextEmail);
        TextInputEditText editPassword = findViewById(R.id.editTextPassword);
        MaterialButton buttonLogin = findViewById(R.id.buttonLogin);
        MaterialButton buttonGoRegister = findViewById(R.id.buttonGoRegister);

        // Vérifie si une ancienne session est encore en cache (ex. compte supprimé sur Firebase)
        checkExistingSession();

        buttonLogin.setOnClickListener(v -> {
            String username = textOf(editUsername);
            String email = textOf(editEmail);
            String password = textOf(editPassword);
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.auth_fields_required, Toast.LENGTH_SHORT).show();
                return;
            }
            setLoading(true);
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            setLoading(false);
                            Toast.makeText(this,
                                    AuthErrorHelper.getMessage(this, task.getException()),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        ensureProfileThenGoMain(username, email);
                    });
        });

        buttonGoRegister.setOnClickListener(v -> {
            AuthHelper.signOut();
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    /** Session locale valide → MainActivity, sinon afficher le formulaire de connexion. */
    private void checkExistingSession() {
        setLoading(true);
        loginForm.setVisibility(View.INVISIBLE);

        AuthHelper.validateSession(new AuthHelper.SessionCallback() {
            @Override
            public void onValidSession(com.google.firebase.auth.FirebaseUser user) {
                goToMain();
            }

            @Override
            public void onNoSession() {
                setLoading(false);
                loginForm.setVisibility(View.VISIBLE);
            }
        });
    }

    /** Crée le profil Firestore s'il manque (compte Auth sans document users). */
    private void ensureProfileThenGoMain(String username, String email) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            setLoading(false);
            return;
        }
        FirestoreHelper.ensureUserProfile(user.getUid(), username, email,
                new FirestoreHelper.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        setLoading(false);
                        goToMain();
                    }

                    @Override
                    public void onError(String msg) {
                        setLoading(false);
                        goToMain();
                    }
                });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.buttonLogin).setEnabled(!loading);
        findViewById(R.id.buttonGoRegister).setEnabled(!loading);
    }

    private static String textOf(TextInputEditText editText) {
        if (editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }
}
