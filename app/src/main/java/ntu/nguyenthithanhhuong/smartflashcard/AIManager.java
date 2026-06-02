package ntu.nguyenthithanhhuong.smartflashcard;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ntu.nguyenthithanhhuong.smartflashcard.model.AiWordResult;
import ntu.nguyenthithanhhuong.smartflashcard.model.WordMeaning;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIManager {
    private static final String TAG = "GroqManager";
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
    private final Context appContext;

    public interface AiCallback {
        void onSuccess(AiWordResult result);

        void onError(String error);
    }

    public AIManager(Context context) {
        this.appContext = context.getApplicationContext();
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
            sendError(callback, appContext.getString(R.string.ai_missing_key));
            return;
        }

        String safeWord = (word == null) ? "" : word.trim();
        if (safeWord.isEmpty()) {
            sendError(callback, appContext.getString(R.string.ai_empty_word));
            return;
        }


        try {
            generateWithModelIndex(safeWord, 0, callback);
        } catch (Exception e) {
            sendError(callback, e.getMessage() == null
                    ? appContext.getString(R.string.ai_request_error)
                    : e.getMessage());
        }
    }

    private void generateWithModelIndex(String safeWord, int modelIndex, AiCallback callback) throws Exception {
        String apiKey = BuildConfig.OPENROUTER_API_KEY;
        String model = MODEL_FALLBACKS[Math.min(Math.max(modelIndex, 0), MODEL_FALLBACKS.length - 1)];

        String prompt =
                "You are an English dictionary AI.\n" +
                        "Analyze this English word: '" + safeWord + "'.\n\n" +

                        "Rules:\n" +
                        "- If the word is misspelled, detect and correct it.\n" +
                        "- Return the corrected word.\n" +
                        "- If the word is already correct, correctedWord must equal original word.\n" +
                        "- Return up to 3 common meanings.\n" +
                        "- Each meaning must contain Vietnamese meaning, IPA and example.\n" +
                        "- Return ONLY valid JSON.\n\n" +

                        "Format:\n" +
                        "{\n" +
                        "\"correctedWord\":\"\",\n" +
                        "\"isCorrect\":true,\n" +
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
                    sendError(callback, e.getMessage() == null
                            ? appContext.getString(R.string.ai_network_error)
                            : e.getMessage());
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

                        sendError(callback, appContext.getString(R.string.ai_no_data));

                        return;
                    }

                    if (!response.isSuccessful()) {
                        String msg = parseApiErrorMessage(raw);
                        if (response.code() == 400 && modelIndex < MODEL_FALLBACKS.length - 1) {
                            String lower = msg == null ? "" : msg.toLowerCase();
                            if (lower.contains("model") && (lower.contains("dismiss") || lower.contains("deprecated") || lower.contains("not found"))) {
                                try {
                                    generateWithModelIndex(safeWord, modelIndex + 1, callback);
                                    return;
                                } catch (Exception ignored) {
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
                            String msg = err != null
                                    ? err.optString("message", appContext.getString(R.string.ai_api_error))
                                    : appContext.getString(R.string.ai_api_error);
                            sendError(callback, msg);
                            return;
                        }

                        JSONArray choices = obj.optJSONArray("choices");
                        if (choices == null || choices.length() == 0) {
                            sendError(callback, appContext.getString(R.string.ai_no_choices));
                            return;
                        }

                        JSONObject message = choices.optJSONObject(0) != null ? choices.optJSONObject(0).optJSONObject("message") : null;
                        String content = message != null ? message.optString("content", "") : "";
                        String cleaned = extractJson(
                                stripCodeFences(content)
                        ).trim();

                        if (!cleaned.startsWith("{")) {

                            sendError(callback, appContext.getString(R.string.ai_bad_response));

                            return;
                        }

                        JSONObject contentObj = new JSONObject(cleaned);

                        String correctedWord =
                                contentObj.optString(
                                        "correctedWord",
                                        safeWord
                                );

                        boolean isCorrect =
                                contentObj.optBoolean(
                                        "isCorrect",
                                        true
                                );

                        JSONArray meaningsArray =
                                contentObj.optJSONArray("meanings");

                        if (meaningsArray == null) {
                            meaningsArray = new JSONArray();
                        }

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

                        AiWordResult result =
                                new AiWordResult(
                                        correctedWord,
                                        isCorrect,
                                        meanings
                                );

                        mainHandler.post(() ->
                                callback.onSuccess(result)
                        );

                    } catch (Exception ex) {
                        sendError(callback, appContext.getString(R.string.ai_parse_error, ex.getMessage()));
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
        }
        return "";
    }
}

