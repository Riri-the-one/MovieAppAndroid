package com.example.movies_app;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Appels centralisés à l'API Gemini (chatbot, quiz, correction).
 */
public final class GeminiHelper {

    private static final String[] GEMINI_MODELS = {
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash-lite",
            "gemini-2.5-flash"
    };

    public interface GeminiCallback {
        void onSuccess(String text);

        void onError(String message);
    }

    private GeminiHelper() {
    }

    /**
     * Envoie un message à Gemini. Le {@code systemPrompt} (goûts Firestore, etc.)
     * est injecté dans {@code system_instruction} du JSON Volley.
     */
    public static void sendMessage(Context context, String systemPrompt, String userMessage,
                                   GeminiCallback callback) {
        String apiKey = BuildConfig.GEMINI_API_KEY.trim();
        if (apiKey.isEmpty()) {
            callback.onError("Clé API Gemini manquante");
            return;
        }
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            callback.onError("Contexte utilisateur non chargé");
            return;
        }
        RequestQueue queue = Volley.newRequestQueue(context.getApplicationContext());
        callGemini(queue, apiKey, systemPrompt.trim(), userMessage, 0, callback);
    }

    private static void callGemini(RequestQueue queue, String apiKey, String systemPrompt,
                                   String userMessage, int modelIndex, GeminiCallback callback) {
        if (modelIndex >= GEMINI_MODELS.length) {
            callback.onError("Impossible de contacter Gemini");
            return;
        }

        try {
            JSONObject body = buildRequestBody(systemPrompt, userMessage);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + GEMINI_MODELS[modelIndex]
                    + ":generateContent?key=" + apiKey;

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    body,
                    response -> {
                        try {
                            String text = response
                                    .getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");
                            callback.onSuccess(text.trim());
                        } catch (JSONException e) {
                            callback.onError("Réponse Gemini invalide");
                        }
                    },
                    error -> handleError(queue, apiKey, systemPrompt, userMessage, modelIndex, error, callback)
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            request.setRetryPolicy(new DefaultRetryPolicy(30_000, 0, 1f));
            queue.add(request);
        } catch (JSONException e) {
            callback.onError("Erreur JSON");
        }
    }

    /** Corps JSON generateContent : system_instruction + message utilisateur. */
    static JSONObject buildRequestBody(String systemPrompt, String userMessage)
            throws JSONException {
        JSONObject body = new JSONObject();

        JSONObject systemInstruction = new JSONObject();
        JSONArray systemParts = new JSONArray();
        systemParts.put(new JSONObject().put("text", systemPrompt));
        systemInstruction.put("parts", systemParts);
        body.put("system_instruction", systemInstruction);

        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", userMessage));
        content.put("role", "user");
        content.put("parts", parts);
        contents.put(content);
        body.put("contents", contents);

        return body;
    }

    private static void handleError(RequestQueue queue, String apiKey, String systemPrompt,
                                    String userMessage, int modelIndex, VolleyError error,
                                    GeminiCallback callback) {
        int code = error.networkResponse != null ? error.networkResponse.statusCode : 0;
        String detail = parseError(error);
        if ((code == 404 || code == 429 || detail.toLowerCase().contains("quota"))
                && modelIndex + 1 < GEMINI_MODELS.length) {
            callGemini(queue, apiKey, systemPrompt, userMessage, modelIndex + 1, callback);
        } else {
            callback.onError(detail);
        }
    }

    private static String parseError(VolleyError error) {
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                String body = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                JSONObject json = new JSONObject(body);
                if (json.has("error")) {
                    return json.getJSONObject("error").optString("message", body);
                }
            } catch (JSONException ignored) {
            }
        }
        return "Erreur réseau Gemini";
    }
}
