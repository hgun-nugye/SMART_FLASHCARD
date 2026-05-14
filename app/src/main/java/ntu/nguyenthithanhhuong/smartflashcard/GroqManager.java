package ntu.nguyenthithanhhuong.smartflashcard;

import android.os.Handler;
import android.os.Looper;

import ntu.nguyenthithanhhuong.smartflashcard.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GroqManager {
    private static final String TAG = "GroqManager";
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    // Groq model IDs change over time; prefer production models.
    // See: https://console.groq.com/docs/models
    private static final String[] MODEL_FALLBACKS = new String[] {
            "llama-3.1-8b-instant",
            "llama-3.3-70b-versatile",
            "openai/gpt-oss-20b"
    };

    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AiCallback {
        void onSuccess(String definition, String ipa, String example);
        void onError(String error);
    }

    public GroqManager() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public void generateCardContent(String word, AiCallback callback) {
        String apiKey = BuildConfig
        if (apiKey == null || apiKey.trim().isEmpty()) {
            sendError(callback, "Thiếu GROQ_API_KEY. Hãy cấu hình trong local.properties/gradle.properties.");
            return;
        }

        String safeWord = (word == null) ? "" : word.trim();
        if (safeWord.isEmpty()) {
            sendError(callback, "Từ vựng trống.");
            return;
        }

        String prompt =
                "Analyze the English word '" + safeWord + "'. " +
                        "Return ONLY a JSON object with keys: " +
                        "'def' (Vietnamese meaning), 'ipa' (phonetic), 'eg' (English example). " +
                        "No intro, no markdown.";

        try {
            generateWithModelIndex(safeWord, 0, callback);
        } catch (Exception e) {
            sendError(callback, e.getMessage() == null ? "Lỗi tạo request." : e.getMessage());
        }
    }

    private void generateWithModelIndex(String safeWord, int modelIndex, AiCallback callback) throws Exception {
        String apiKey = BuildConfig.GROQ_API_KEY;
        String model = MODEL_FALLBACKS[Math.min(Math.max(modelIndex, 0), MODEL_FALLBACKS.length - 1)];

        String prompt =
                "Analyze the English word '" + safeWord + "'. " +
                        "Return ONLY a JSON object with keys: " +
                        "'def' (Vietnamese meaning), 'ipa' (phonetic), 'eg' (English example). " +
                        "No intro, no markdown.";

        try {
            JSONObject payload = new JSONObject();
            payload.put("model", model);
            payload.put("messages", new JSONArray()
                    .put(new JSONObject().put("role", "user").put("content", prompt)));
            payload.put("response_format", new JSONObject().put("type", "json_object"));

            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                    .url(ENDPOINT)
                    .addHeader("Authorization", "Bearer " + apiKey.trim())
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    sendError(callback, e.getMessage() == null ? "Lỗi mạng." : e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String raw = response.body() != null ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        String msg = parseApiErrorMessage(raw);
                        // If model is deprecated/dismissed, try next fallback.
                        if (response.code() == 400 && modelIndex < MODEL_FALLBACKS.length - 1) {
                            String lower = msg == null ? "" : msg.toLowerCase();
                            if (lower.contains("model") && (lower.contains("dismiss") || lower.contains("deprecated") || lower.contains("not found"))) {
                                try {
                                    generateWithModelIndex(safeWord, modelIndex + 1, callback);
                                    return;
                                } catch (Exception ignored) {
                                    // fall through to error
                                }
                            }
                        }
                        sendError(callback, "HTTP " + response.code() + (msg.isEmpty() ? "" : (": " + msg)));
                        return;
                    }

                    try {
                        JSONObject obj = new JSONObject(raw);
                        if (obj.has("error")) {
                            JSONObject err = obj.optJSONObject("error");
                            String msg = err != null ? err.optString("message", "Lỗi không xác định từ API") : "Lỗi không xác định từ API";
                            sendError(callback, msg);
                            return;
                        }

                        JSONArray choices = obj.optJSONArray("choices");
                        if (choices == null || choices.length() == 0) {
                            sendError(callback, "Server không trả về kết quả (choices).");
                            return;
                        }

                        JSONObject message = choices.optJSONObject(0) != null ? choices.optJSONObject(0).optJSONObject("message") : null;
                        String content = message != null ? message.optString("content", "") : "";
                        String cleaned = stripCodeFences(content).trim();

                        JSONObject contentObj = new JSONObject(cleaned);
                        String def = contentObj.optString("def", "Không có nghĩa");
                        String ipa = contentObj.optString("ipa", "/.../");
                        String eg = contentObj.optString("eg", "Không có ví dụ");

                        mainHandler.post(() -> callback.onSuccess(def, ipa, eg));
                    } catch (Exception ex) {
                        sendError(callback, "Lỗi phân tích: " + ex.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            throw e;
        }
    }

    private void sendError(AiCallback callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }

    private static String stripCodeFences(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline >= 0) t = t.substring(firstNewline + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t;
    }

    private static String parseApiErrorMessage(String raw) {
        if (raw == null) return "";
        try {
            JSONObject obj = new JSONObject(raw);
            if (obj.has("error")) {
                JSONObject err = obj.optJSONObject("error");
                if (err != null) return err.optString("message", "");
            }
        } catch (Exception ignored) {
            // ignore
        }
        return "";
    }
}

