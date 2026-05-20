package com.example.movies_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Assistant vocal : reconnaissance vocale + requêtes Gemini via Volley.
 * Le systemPrompt n'est construit qu'après le chargement Firestore users_feedback.
 */
public class ChatbotActivity extends AppCompatActivity {

    private static final String TAG = "ChatbotActivity";
    private static final int REQUEST_RECORD_AUDIO = 100;
    private static final String COLLECTION_USERS_FEEDBACK = "users_feedback";

    /** Prompt injecté dans system_instruction de Gemini — rempli après Firestore. */
    private String systemPrompt = "";

    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ImageButton buttonMicro;
    private MessageAdapter messageAdapter;
    private SpeechRecognizer speechRecognizer;
    private boolean chatReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chatbot);
        setTitle(R.string.chatbot_title);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView recyclerViewChat = findViewById(R.id.recyclerViewChat);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonMicro = findViewById(R.id.buttonMicro);

        messageAdapter = new MessageAdapter();
        messageAdapter.attachRecyclerView(recyclerViewChat);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(messageAdapter);

        buttonSend.setEnabled(false);
        buttonMicro.setEnabled(false);
        editTextMessage.setEnabled(false);

        initSpeechRecognizer();
        fetchUserPreferencesAndInitChat();

        buttonSend.setOnClickListener(v -> sendMessage());
        buttonMicro.setOnClickListener(v -> startVoiceInput());
    }

    /**
     * Charge users_feedback puis construit systemPrompt avant d'activer le chat.
     */
    private void fetchUserPreferencesAndInitChat() {
        messageAdapter.addMessage(new Message(getString(R.string.chatbot_loading), false));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        String currentUid = user.getUid();

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_USERS_FEEDBACK)
                .whereEqualTo("uid", currentUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> likedTitles = new ArrayList<>();
                    List<String> dislikedTitles = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String feedbackType = doc.getString("feedbackType");
                        if (feedbackType == null || feedbackType.isEmpty()) {
                            feedbackType = doc.getString("choice");
                        }
                        if (feedbackType == null) {
                            continue;
                        }

                        String movieTitle = doc.getString("movieTitle");
                        if (movieTitle == null || movieTitle.isEmpty()) {
                            movieTitle = doc.getString("title");
                        }
                        if (movieTitle == null || movieTitle.isEmpty()) {
                            continue;
                        }

                        if (FirestoreHelper.FEEDBACK_LIKE.equalsIgnoreCase(feedbackType)) {
                            likedTitles.add(movieTitle);
                        } else if (FirestoreHelper.FEEDBACK_DISLIKE.equalsIgnoreCase(feedbackType)) {
                            dislikedTitles.add(movieTitle);
                        }
                    }

                    systemPrompt = FirestoreHelper.buildPersonalizedSystemPrompt(
                            likedTitles, dislikedTitles);
                    Log.d(TAG, "systemPrompt prêt — likes: " + likedTitles.size()
                            + ", dislikes: " + dislikedTitles.size());

                    loadProfileAndShowWelcome();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur Firestore users_feedback : " + e.getMessage());
                    systemPrompt = FirestoreHelper.buildPersonalizedSystemPrompt(
                            new ArrayList<>(), new ArrayList<>());
                    loadProfileAndShowWelcome();
                });
    }

    private void loadProfileAndShowWelcome() {
        FirestoreHelper.getUserProfile(new FirestoreHelper.ProfileCallback() {
            @Override
            public void onProfile(FirestoreHelper.UserProfile profile) {
                showWelcomeMessage(profile.getDisplayName());
            }

            @Override
            public void onError(String message) {
                showWelcomeMessage("Cinéphile");
            }
        });
    }

    private void showWelcomeMessage(String username) {
        if (messageAdapter.getItemCount() > 0) {
            messageAdapter.removeLastMessage();
        }
        String greeting = getString(R.string.chatbot_greeting, username);
        messageAdapter.addMessage(new Message(greeting, false));

        chatReady = true;
        buttonSend.setEnabled(true);
        buttonMicro.setEnabled(true);
        editTextMessage.setEnabled(true);
    }

    private void sendMessage() {
        if (!chatReady) {
            Toast.makeText(this, R.string.chatbot_loading, Toast.LENGTH_SHORT).show();
            return;
        }

        String userText = editTextMessage.getText().toString().trim();
        if (userText.isEmpty()) {
            return;
        }

        if (systemPrompt == null || systemPrompt.isEmpty()) {
            Toast.makeText(this, R.string.chatbot_loading, Toast.LENGTH_SHORT).show();
            return;
        }

        messageAdapter.addMessage(new Message(userText, true));
        editTextMessage.setText("");

        buttonSend.setEnabled(false);
        editTextMessage.setEnabled(false);

        GeminiHelper.sendMessage(this, systemPrompt, userText, new GeminiHelper.GeminiCallback() {
            @Override
            public void onSuccess(String text) {
                messageAdapter.addMessage(new Message(text, false));
                buttonSend.setEnabled(true);
                editTextMessage.setEnabled(true);
            }

            @Override
            public void onError(String message) {
                messageAdapter.addMessage(new Message(
                        getString(R.string.gemini_quota_exceeded).equals(message)
                                ? message
                                : "Erreur : " + message, false));
                buttonSend.setEnabled(true);
                editTextMessage.setEnabled(true);
            }
        });
    }

    private void initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { }

            @Override
            public void onError(int error) {
                Toast.makeText(ChatbotActivity.this,
                        "Erreur reconnaissance vocale : " + error,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    editTextMessage.setText(matches.get(0));
                }
            }

            @Override public void onPartialResults(Bundle partialResults) { }
            @Override public void onEvent(int eventType, Bundle params) { }
        });
    }

    private void startVoiceInput() {
        if (!chatReady) {
            Toast.makeText(this, R.string.chatbot_loading, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, R.string.speech_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO);
            return;
        }

        launchSpeechIntent();
    }

    private void launchSpeechIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRANCE);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez maintenant…");
        speechRecognizer.startListening(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchSpeechIntent();
        } else if (requestCode == REQUEST_RECORD_AUDIO) {
            Toast.makeText(this, R.string.permission_mic_denied, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
