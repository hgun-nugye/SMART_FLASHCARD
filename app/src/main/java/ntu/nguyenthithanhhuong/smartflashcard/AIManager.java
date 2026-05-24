package ntu.nguyenthithanhhuong.smartflashcard;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ntu.nguyenthithanhhuong.smartflashcard.Model.WordMeaning;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIManager {
    private static final String TAG = "GroqManager";
    //    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String ENDPOINT =
            "https://openrouter.ai/api/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final String[] MODEL_FALLBACKS = new String[]{
            "google/gemini-2.5-flash",
            "qwen/qwen3-32b",
            "deepseek/deepseek-chat-v3-0324"
    };

    private static String extractJson(String text) {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");

        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return text;
    }

    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AiCallback {
        void onSuccess(List<WordMeaning> meanings);

        void onError(String error);
    }

    public AIManager() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public void generateCardContent(String word, AiCallback callback) {
        String apiKey = BuildConfig.OPENROUTER_API_KEY;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            sendError(callback, "Thiếu OPENROUTER_API_KEY. Hãy cấu hình trong local.properties/gradle.properties.");
            return;
        }

        String safeWord = (word == null) ? "" : word.trim();
        if (safeWord.isEmpty()) {
            sendError(callback, "Từ vựng trống.");
            return;
        }


        try {
            generateWithModelIndex(safeWord, 0, callback);
        } catch (Exception e) {
            sendError(callback, e.getMessage() == null ? "Lỗi tạo request." : e.getMessage());
        }
    }

    private void generateWithModelIndex(String safeWord, int modelIndex, AiCallback callback) throws Exception {
        String apiKey = BuildConfig.OPENROUTER_API_KEY;
        String model = MODEL_FALLBACKS[Math.min(Math.max(modelIndex, 0), MODEL_FALLBACKS.length - 1)];

        String prompt =
                "You are an English-Vietnamese dictionary AI.\n" +
                        "Analyze the English word: '" + safeWord + "'.\n\n" +

                        "Rules:\n" +
                        "- Return up to 3 common meanings.\n" +
                        "- Each meaning must contain:\n" +
                        "  - Vietnamese meaning\n" +
                        "  - correct IPA for that meaning\n" +
                        "  - simple English example\n" +
                        "- Return ONLY valid JSON.\n" +
                        "- No markdown.\n" +
                        "- No explanation.\n\n" +

                        "Format exactly:\n" +
                        "{\n" +
                        "\"meanings\":[\n" +
                        "{\n" +
                        "\"vi\":\"\",\n" +
                        "\"ipa\":\"\",\n" +
                        "\"example\":\"\"\n" +
                        "}\n" +
                        "]\n" +
                        "}";

        try {
            JSONObject payload = new JSONObject();
            payload.put("model", model);
            payload.put("temperature", 0.2);
            payload.put("messages", new JSONArray()
                    .put(new JSONObject().put("role", "user").put("content", prompt)));
            payload.put("max_tokens", 300);

            RequestBody body = RequestBody.create(payload.toString(), JSON);

            Request request = new Request.Builder()
                    .url(ENDPOINT)
                    .addHeader("Authorization", "Bearer " + apiKey.trim())
                    .addHeader("HTTP-Referer", "https://yourapp.com")
                    .addHeader("X-Title", "SmartFlashcard")
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
                    Log.d("AI_RAW", raw);
                    if (response.code() == 204 || raw.trim().isEmpty()) {

                        if (modelIndex < MODEL_FALLBACKS.length - 1) {

                            try {

                                generateWithModelIndex(
                                        safeWord,
                                        modelIndex + 1,
                                        callback
                                );

                                return;

                            } catch (Exception ignored) {
                            }
                        }

                        sendError(callback,
                                "AI không trả dữ liệu.");

                        return;
                    }

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
                        String cleaned = extractJson(
                                stripCodeFences(content)
                        ).trim();

                        if (!cleaned.startsWith("{")) {

                            sendError(callback,
                                    "AI trả dữ liệu lỗi.");

                            return;
                        }

                        JSONObject contentObj = new JSONObject(cleaned);

                        JSONArray meaningsArray =
                                contentObj.getJSONArray("meanings");

                        List<WordMeaning> meanings =
                                new ArrayList<>();

                        for (int i = 0; i < meaningsArray.length(); i++) {

                            JSONObject item =
                                    meaningsArray.getJSONObject(i);

                            String vi =
                                    item.optString("vi", "");

                            String ipa =
                                    item.optString("ipa", "");

                            String example =
                                    item.optString("example", "");

                            meanings.add(
                                    new WordMeaning(
                                            vi,
                                            ipa,
                                            example
                                    )
                            );
                        }

                        mainHandler.post(() ->
                                callback.onSuccess(meanings));
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

