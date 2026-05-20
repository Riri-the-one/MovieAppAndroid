package com.example.movies_app;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * CRUD profil : photo Base64 dans Firestore, nom (Auth + Firestore).
 */
public class ManageProfileActivity extends AppCompatActivity {

    private ShapeableImageView imageViewProfileEdit;
    private TextInputEditText editTextProfileUsername;
    private TextView textViewProfileEmailReadonly;
    private ProgressBar progressBar;

    private Uri pendingPhotoUri;
    private String currentProfileImageBase64;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pendingPhotoUri = uri;
                    Glide.with(this).load(uri).circleCrop().into(imageViewProfileEdit);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_profile);

        MaterialToolbar toolbar = findViewById(R.id.toolbarManageProfile);
        toolbar.setNavigationOnClickListener(v -> finish());

        imageViewProfileEdit = findViewById(R.id.imageViewProfileEdit);
        editTextProfileUsername = findViewById(R.id.editTextProfileUsername);
        textViewProfileEmailReadonly = findViewById(R.id.textViewProfileEmailReadonly);
        progressBar = findViewById(R.id.progressBarManageProfile);
        MaterialButton buttonSave = findViewById(R.id.buttonSaveProfile);
        MaterialButton buttonDelete = findViewById(R.id.buttonDeleteAccountManage);

        imageViewProfileEdit.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        buttonSave.setOnClickListener(v -> saveProfile());
        buttonDelete.setOnClickListener(v -> confirmDeleteAccount());

        loadProfile();
    }

    private void loadProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        textViewProfileEmailReadonly.setText(user.getEmail());

        FirestoreHelper.getUserProfile(new FirestoreHelper.ProfileCallback() {
            @Override
            public void onProfile(FirestoreHelper.UserProfile profile) {
                editTextProfileUsername.setText(profile.getUsername());
                currentProfileImageBase64 = profile.getProfileImageBase64();
                ProfileImageHelper.displayInImageView(imageViewProfileEdit, currentProfileImageBase64);
            }

            @Override
            public void onError(String message) {
                editTextProfileUsername.setText(user.getDisplayName());
                ProfileImageHelper.displayInImageView(imageViewProfileEdit, null);
            }
        });
    }

    private void saveProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        String username = editTextProfileUsername.getText() != null
                ? editTextProfileUsername.getText().toString().trim()
                : "";
        if (username.isEmpty()) {
            Toast.makeText(this, R.string.auth_fields_required, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        String base64ToSave = currentProfileImageBase64;
        if (pendingPhotoUri != null) {
            try {
                base64ToSave = ProfileImageHelper.uriToBase64(this, pendingPhotoUri);
            } catch (Exception e) {
                setLoading(false);
                Toast.makeText(this, R.string.profile_image_error, Toast.LENGTH_LONG).show();
                return;
            }
        }

        final String finalBase64 = base64ToSave;
        user.updateProfile(new UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build())
                .addOnCompleteListener(authTask ->
                        FirestoreHelper.updateUserProfile(
                                user.getUid(),
                                username,
                                user.getEmail(),
                                finalBase64,
                                new FirestoreHelper.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        setLoading(false);
                                        pendingPhotoUri = null;
                                        currentProfileImageBase64 = finalBase64;
                                        Toast.makeText(ManageProfileActivity.this,
                                                R.string.profile_saved, Toast.LENGTH_SHORT).show();
                                        finish();
                                    }

                                    @Override
                                    public void onError(String message) {
                                        setLoading(false);
                                        Toast.makeText(ManageProfileActivity.this,
                                                message, Toast.LENGTH_LONG).show();
                                    }
                                }));
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account_title)
                .setMessage(R.string.delete_account_message)
                .setPositiveButton(R.string.delete_account_confirm, (d, w) -> performDelete())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void performDelete() {
        setLoading(true);
        AuthHelper.deleteAccount(new AuthHelper.DeleteAccountCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(ManageProfileActivity.this,
                        R.string.delete_account_success, Toast.LENGTH_LONG).show();
                IntentHelper.goToLoginClearingStack(ManageProfileActivity.this);
                finish();
            }

            @Override
            public void onError(String message, boolean needsReLogin) {
                setLoading(false);
                Toast.makeText(ManageProfileActivity.this, message, Toast.LENGTH_LONG).show();
                if (needsReLogin) {
                    IntentHelper.goToLoginClearingStack(ManageProfileActivity.this);
                    finish();
                }
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.buttonSaveProfile).setEnabled(!loading);
        findViewById(R.id.buttonDeleteAccountManage).setEnabled(!loading);
    }
}
