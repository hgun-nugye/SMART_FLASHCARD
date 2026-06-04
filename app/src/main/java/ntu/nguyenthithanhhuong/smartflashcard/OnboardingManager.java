package ntu.nguyenthithanhhuong.smartflashcard;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnboardingManager {

    private static final String TAG = "OnboardingManager";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

    public interface ImportCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void importSampleDecks(List<String> selectedTopics, ImportCallback callback) {
        if (currentUserId == null) {
            callback.onFailure(new Exception("Tài khoản chưa được xác thực hệ thống."));
            return;
        }

        Log.d(TAG, "👉 Khởi chạy tiến trình nạp dữ liệu vào bộ nhớ [decks] gốc: " + selectedTopics.toString());

        Map<String, Object> interestData = new HashMap<>();
        interestData.put("interests", selectedTopics);

        // 1. Lưu danh sách sở thích vào bảng users cá nhân
        db.collection("users").document(currentUserId)
                .set(interestData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {

                    // Lấy dữ liệu tĩnh cục bộ sạch để nạp trực tiếp
                    Map<String, List<Map<String, String>>> localData = getLocalSampleData();

                    final int totalDecks = selectedTopics.size();
                    final int[] processed = {0};

                    for (String topic : selectedTopics) {
                        if (!localData.containsKey(topic)) {
                            processed[0]++;
                            if (processed[0] == totalDecks) callback.onSuccess();
                            continue;
                        }

                        var userDeckRef = db.collection("decks").document();

                        List<Map<String, String>> cardsList = localData.get(topic);

                        // Tạo các trường dữ liệu cho Deck khớp 100% với ảnh chụp màn hình của bạn
                        Map<String, Object> deckMeta = new HashMap<>();
                        deckMeta.put("name", topic.replace("_", " ").toUpperCase());
                        deckMeta.put("description", "Bộ thẻ mẫu chủ đề " + topic.replace("_", " "));
                        deckMeta.put("createdAt", System.currentTimeMillis());
                        deckMeta.put("ownerId", currentUserId); // Định danh chủ sở hữu bộ thẻ
                        deckMeta.put("cardCount", cardsList.size());

                        userDeckRef.set(deckMeta).addOnSuccessListener(unused -> {
                            WriteBatch batch = db.batch();

                            for (Map<String, String> localCard : cardsList) {
                                var cardDocRef = userDeckRef.collection("flashcards").document();

                                Map<String, Object> cardData = new HashMap<>(localCard);
                                // Khởi tạo các trường bổ trợ cho thuật toán học (SM2) nếu cần thiết
                                cardData.put("interval", 1);
                                cardData.put("easeFactor", 2.5);
                                cardData.put("repetitions", 0);
                                cardData.put("nextReview", 0L);

                                batch.set(cardDocRef, cardData);
                            }

                            // Thực thi ghi đồng loạt (Batch commit) lên Cloud
                            batch.commit().addOnSuccessListener(u -> {
                                processed[0]++;
                                Log.d(TAG, "✅ Đã nhân bản thành công bộ thẻ hệ thống ngoài: " + topic);
                                if (processed[0] == totalDecks) {
                                    callback.onSuccess();
                                }
                            }).addOnFailureListener(callback::onFailure);

                        }).addOnFailureListener(callback::onFailure);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    private Map<String, List<Map<String, String>>> getLocalSampleData() {
        Map<String, List<Map<String, String>>> data = new HashMap<>();

        // 1. General English
        List<Map<String, String>> ge = new ArrayList<>();
        ge.add(createCardMap("Magnificent", "Tráng lệ, lộng lẫy", "/mæɡˈnɪfɪsnt/", "The view from the top is magnificent."));
        ge.add(createCardMap("Determine", "Quyết tâm, xác định", "/dɪˈtɜːrmɪn/", "She is determined to pass the final exam."));
        data.put("General_English", ge);

        // 2. Workplace Culture
        List<Map<String, String>> wc = new ArrayList<>();
        wc.add(createCardMap("Punctuality", "Sự đúng giờ, tác phong", "/ˌpʌŋktʃuˈæləti/", "Punctuality is highly valued in business."));
        wc.add(createCardMap("Dedication", "Sự tận tụy, cống hiến", "/ˌdedɪˈkeɪʃn/", "He showed great dedication to the project."));
        data.put("Workplace_Culture", wc);

        // 3. Entertainment & Media
        List<Map<String, String>> em = new ArrayList<>();
        em.add(createCardMap("Trendsetter", "Người tạo trào lưu", "/ˈtrendsetər/", "Idols are known as global trendsetters."));
        em.add(createCardMap("Binge-watch", "Cày phim liên tục", "/ˈbɪndʒ wɑːtʃ/", "I stayed up all night to binge-watch a series."));
        data.put("Entertainment_Media", em);

        // 4. Global Trade
        List<Map<String, String>> gt = new ArrayList<>();
        gt.add(createCardMap("E-commerce", "Thương mại điện tử", "/ˈiː kɑːmɜːrs/", "Live-streaming has revolutionized e-commerce."));
        data.put("Global_Trade", gt);

        // 5. TOEIC
        List<Map<String, String>> toeic = new ArrayList<>();
        toeic.add(createCardMap("Implement", "Thực hiện, áp dụng", "/ˈɪmplɪment/", "We will implement a new safety policy."));
        data.put("TOEIC", toeic);

        // 6. IELTS
        List<Map<String, String>> ielts = new ArrayList<>();
        ielts.add(createCardMap("Ameliorate", "Cải thiện tốt hơn", "/əˈmiːliəreɪt/", "Steps were taken to ameliorate air pollution."));
        data.put("IELTS", ielts);

        // 7. Test Strategies
        List<Map<String, String>> ts = new ArrayList<>();
        ts.add(createCardMap("Distractor", "Đáp án bẫy nhiễu", "/dɪˈstræktər/", "Be careful to avoid the distractor."));
        data.put("Test_Strategies", ts);

        // 8. Exam Management
        List<Map<String, String>> emg = new ArrayList<>();
        emg.add(createCardMap("Pace", "Kiểm soát tốc độ thời gian", "/peɪs/", "Pace yourself to finish all test questions."));
        data.put("Exam_Management", emg);

        // 9. IT
        List<Map<String, String>> it = new ArrayList<>();
        it.add(createCardMap("Database", "Cơ sở dữ liệu", "/ˈdeɪtəbeɪs/", "SQL Server is a relational database."));
        it.add(createCardMap("Algorithm", "Thuật toán", "/ˈælɡərɪðəm/", "The N-Queens problem is solved by backtracking algorithm."));
        data.put("IT", it);

        // 10. Business
        List<Map<String, String>> biz = new ArrayList<>();
        biz.add(createCardMap("Revenue", "Doanh thu", "/ˈrevənjuː/", "The company's annual revenue increased significantly."));
        data.put("Business", biz);

        // 11. Tourism
        List<Map<String, String>> tour = new ArrayList<>();
        tour.add(createCardMap("Itinerary", "Lịch trình chuyến đi", "/aɪˈtɪnəreri/", "The agent provided us with a detailed itinerary."));
        data.put("Tourism", tour);

        // 12. Medical
        List<Map<String, String>> med = new ArrayList<>();
        med.add(createCardMap("Symptom", "Triệu chứng bệnh", "/ˈsɪmptəm/", "A dry cough is a common symptom of flu."));
        data.put("Medical", med);

        // 13. Daily
        List<Map<String, String>> daily = new ArrayList<>();
        daily.add(createCardMap("Routine", "Thói quen hàng ngày", "/ruːˈtiːn/", "Reviewing flashcards is part of my routine."));
        data.put("Daily", daily);

        return data;
    }

    private Map<String, String> createCardMap(String front, String back, String ipa, String example) {
        Map<String, String> card = new HashMap<>();
        card.put("front", front);
        card.put("back", back);
        card.put("ipa", ipa);
        card.put("example", example);
        return card;
    }
}