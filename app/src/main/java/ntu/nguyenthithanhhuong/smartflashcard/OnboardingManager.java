package ntu.nguyenthithanhhuong.smartflashcard;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnboardingManager {

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

        // Lưu danh sách sở thích vào bảng Users cá nhân
        db.collection("users").document(currentUserId)
                .update("interests", selectedTopics)
                .addOnSuccessListener(aVoid -> {
                    // Truy vấn dữ liệu từ bộ thẻ mẫu hệ thống theo các chủ đề được lựa chọn
                    db.collection("system_decks")
                            .whereIn("topic", selectedTopics)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (queryDocumentSnapshots.isEmpty()) {
                                    callback.onSuccess();
                                    return;
                                }

                                final int[] totalDecksToProcess = { queryDocumentSnapshots.size() };
                                final int[] processedCount = { 0 };

                                for (DocumentSnapshot deckDoc : queryDocumentSnapshots.getDocuments()) {
                                    cloneDeckToUserCustom(deckDoc, new ImportCallback() {
                                        @Override
                                        public void onSuccess() {
                                            processedCount[0]++;
                                            if (processedCount[0] == totalDecksToProcess[0]) {
                                                callback.onSuccess();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            callback.onFailure(e);
                                        }
                                    });
                                }
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    private void cloneDeckToUserCustom(DocumentSnapshot sampleDeckDoc, ImportCallback callback) {
        String sampleDeckId = sampleDeckDoc.getId();

        // Tạo liên kết lưu trữ vào đúng Sub-collection cá nhân: users -> [userId] -> decks
        var userDeckRef = db.collection("users").document(currentUserId)
                .collection("decks").document();

        // Đọc dữ liệu từ bản mẫu hệ thống
        String sampleTitle = sampleDeckDoc.getString("title");
        String sampleTopic = sampleDeckDoc.getString("topic");

        Map<String, Object> myDeckData = new HashMap<>();
        myDeckData.put("name", sampleTitle != null ? sampleTitle.toUpperCase() : "CHỦ ĐỀ MỚI");
        myDeckData.put("description", "Bộ thẻ mẫu chủ đề " + sampleTopic);
        myDeckData.put("createdAt", System.currentTimeMillis());
        myDeckData.put("ownerId", currentUserId); // Gán quyền sở hữu cho chính tài khoản mới tạo
        myDeckData.put("cardCount", 0); // Sẽ cập nhật giá trị thực tế sau khi đếm số lượng thẻ con

        userDeckRef.set(myDeckData).addOnSuccessListener(aVoid -> {

            // Lấy toàn bộ danh sách flashcards thuộc bộ mẫu này
            db.collection("system_decks").document(sampleDeckId)
                    .collection("flashcards")
                    .get()
                    .addOnSuccessListener(cardsSnapshots -> {
                        if (cardsSnapshots.isEmpty()) {
                            callback.onSuccess();
                            return;
                        }

                        int actualCardCount = cardsSnapshots.size();
                        WriteBatch batch = db.batch();

                        for (DocumentSnapshot cardDoc : cardsSnapshots.getDocuments()) {
                            var userCardRef = userDeckRef.collection("flashcards").document();

                            Map<String, Object> cardData = cardDoc.getData();
                            if (cardData != null) {
                                // Khởi tạo dữ liệu tiến trình học mặc định cho bộ nhớ thuật toán SM2
                                cardData.put("interval", 1);
                                cardData.put("easeFactor", 2.5);
                                cardData.put("repetitions", 0);
                                cardData.put("nextReview", new java.util.Date());
                                cardData.put("statusString", "NEW");

                                batch.set(userCardRef, cardData);
                            }
                        }

                        // Cập nhật lại số lượng thẻ thực tế cho trường cardCount của bộ Deck
                        batch.update(userDeckRef, "cardCount", actualCardCount);

                        // Đồng bộ ghi đồng loạt lên Firestore
                        batch.commit()
                                .addOnSuccessListener(unused -> callback.onSuccess())
                                .addOnFailureListener(callback::onFailure);
                    })
                    .addOnFailureListener(callback::onFailure);
        }).addOnFailureListener(callback::onFailure);
    }
}