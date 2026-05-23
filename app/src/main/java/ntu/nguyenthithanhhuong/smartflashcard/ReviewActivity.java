package ntu.nguyenthithanhhuong.smartflashcard;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ntu.nguyenthithanhhuong.smartflashcard.Model.Flashcard;

public class ReviewActivity extends BaseAppActivity {
    private String deckId;
    private FirebaseFirestore db;
    private List<Flashcard> reviewCards = new ArrayList<>();
    private int currentIndex = 0;

    // Đếm số lượng đúng sai
    private int correctCount = 0;
    private int incorrectCount = 0;

    private TextView tvFront, tvBack, tvProgress;
    private TextView tvResultCorrect, tvResultIncorrect;
    private View divider;
    private Button btnAction, btnCorrect, btnIncorrect, btnFinishReview;
    private LinearLayout llActionButtons, layoutResult;
    private View cvCard;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_review);
        EdgeToEdgeHelper.applyScreenWithToolbar(
                findViewById(R.id.reviewLayout),
                findViewById(R.id.toolbarReview)
        );

        deckId = getIntent().getStringExtra("DECK_ID");
        if (deckId == null || deckId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bộ sưu tập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        initViews();
        loadCards();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarReview);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        cvCard = findViewById(R.id.cvCard);
        tvFront = findViewById(R.id.tvFront);
        tvBack = findViewById(R.id.tvBack);
        divider = findViewById(R.id.divider);
        tvProgress = findViewById(R.id.tvProgress);

        btnAction = findViewById(R.id.btnAction);
        btnCorrect = findViewById(R.id.btnCorrect);
        btnIncorrect = findViewById(R.id.btnIncorrect);
        btnFinishReview = findViewById(R.id.btnFinishReview);

        llActionButtons = findViewById(R.id.llActionButtons);
        layoutResult = findViewById(R.id.layoutResult);
        tvResultCorrect = findViewById(R.id.tvResultCorrect);
        tvResultIncorrect = findViewById(R.id.tvResultIncorrect);

        // Cài đặt khoảng cách camera để hiệu ứng lật 3D trông chân thực
        float scale = getResources().getDisplayMetrics().density;
        cvCard.setCameraDistance(8000 * scale);

        // Sự kiện Hiện nghĩa
        btnAction.setOnClickListener(v -> {
            if (reviewCards.isEmpty()) return;

            btnAction.setEnabled(false);

            // Hiệu ứng lật nhẹ nhàng (tăng duration lên 400ms)
            android.animation.ObjectAnimator flipOut = android.animation.ObjectAnimator.ofFloat(cvCard, "rotationY", 0f, 90f);
            flipOut.setDuration(400);
            flipOut.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    // Khi thẻ xoay ngang, đổi mặt
                    tvBack.setVisibility(View.VISIBLE);
                    divider.setVisibility(View.VISIBLE);

                    // Ẩn nút "Hiện nghĩa", hiện 2 nút "Đúng/Sai"
                    btnAction.setVisibility(View.GONE);
                    llActionButtons.setVisibility(View.VISIBLE);

                    // Lật nốt nửa đường còn lại
                    android.animation.ObjectAnimator flipIn = android.animation.ObjectAnimator.ofFloat(cvCard, "rotationY", -90f, 0f);
                    flipIn.setDuration(400);
                    flipIn.addListener(new android.animation.AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(android.animation.Animator animation) {
                            btnAction.setEnabled(true);
                        }
                    });
                    flipIn.start();
                }
            });
            flipOut.start();
        });

        // Nút Đúng
        btnCorrect.setOnClickListener(v -> {
            correctCount++;
            if (currentIndex < reviewCards.size()) {
                Flashcard currentCard = reviewCards.get(currentIndex);
                updateCardSM2(currentCard, true); // Chạy thuật toán tính ĐÚNG
            }
            moveToNextCard();
        });

        // Nút Sai
        btnIncorrect.setOnClickListener(v -> {
            incorrectCount++;
            if (currentIndex < reviewCards.size()) {
                Flashcard currentCard = reviewCards.get(currentIndex);
                updateCardSM2(currentCard, false); // Chạy thuật toán tính SAI
            }
            moveToNextCard();
        });

        // Nút quay lại khi xong
        btnFinishReview.setOnClickListener(v -> finish());
    }

    private void moveToNextCard() {
        btnCorrect.setEnabled(false);
        btnIncorrect.setEnabled(false);

        android.animation.ObjectAnimator slideOut = android.animation.ObjectAnimator.ofFloat(cvCard, "translationX", 0f, -cvCard.getWidth());
        slideOut.setDuration(250);
        slideOut.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                currentIndex++;
                showCurrentCard();

                if (currentIndex < reviewCards.size()) {
                    cvCard.setTranslationX(cvCard.getWidth());
                    android.animation.ObjectAnimator slideIn = android.animation.ObjectAnimator.ofFloat(cvCard, "translationX", cvCard.getWidth(), 0f);
                    slideIn.setDuration(250);
                    slideIn.addListener(new android.animation.AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(android.animation.Animator animation) {
                            btnCorrect.setEnabled(true);
                            btnIncorrect.setEnabled(true);
                        }
                    });
                    slideIn.start();
                }
            }
        });
        slideOut.start();
    }

    private void loadCards() {
        db.collection("decks").document(deckId)
                .collection("flashcards")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reviewCards.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Flashcard card = doc.toObject(Flashcard.class);
                        if (card != null) {
                            card.cardId = doc.getId(); // LẤY CARD ID
                            reviewCards.add(card);
                        }
                    }

                    Collections.shuffle(reviewCards);

                    currentIndex = 0;
                    correctCount = 0;
                    incorrectCount = 0;
                    showCurrentCard();
                })
                .addOnFailureListener(e -> {
                    Log.e("REVIEW_ACTIVITY", "Error loading cards", e);
                    Toast.makeText(this, "Lỗi tải thẻ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showCurrentCard() {
        if (reviewCards.isEmpty()) {
            cvCard.setVisibility(View.GONE);
            btnAction.setVisibility(View.GONE);
            llActionButtons.setVisibility(View.GONE);
            tvProgress.setVisibility(View.GONE);

            layoutResult.setVisibility(View.VISIBLE);
            tvResultCorrect.setText("Danh sách thẻ rỗng.");
            tvResultIncorrect.setVisibility(View.GONE);
            return;
        }

        if (currentIndex >= reviewCards.size()) {
            // Hoàn thành ôn tập, hiển thị kết quả
            cvCard.setVisibility(View.GONE);
            btnAction.setVisibility(View.GONE);
            llActionButtons.setVisibility(View.GONE);
            tvProgress.setVisibility(View.GONE);

            layoutResult.setVisibility(View.VISIBLE);
            tvResultCorrect.setText("Số câu đúng: " + correctCount);
            tvResultIncorrect.setText("Số câu sai: " + incorrectCount);
            return;
        }

        Flashcard currentCard = reviewCards.get(currentIndex);

        cvCard.setVisibility(View.VISIBLE);
        tvFront.setVisibility(View.VISIBLE);
        tvFront.setText(currentCard.front);

        tvBack.setVisibility(View.GONE);
        tvBack.setText(currentCard.back);

        divider.setVisibility(View.GONE);

        // Reset button status
        btnAction.setVisibility(View.VISIBLE);
        llActionButtons.setVisibility(View.GONE);

        tvProgress.setVisibility(View.VISIBLE);
        tvProgress.setText((currentIndex + 1) + " / " + reviewCards.size());

        layoutResult.setVisibility(View.GONE);
    }

    private void updateCardSM2(Flashcard card, boolean isCorrect) {
        int q = isCorrect ? 4 : 1; // Điểm đánh giá: 4 = Đúng, 1 = Sai theo SM-2 đơn giản

        if (q >= 3) { // Trường hợp người dùng trả lời ĐÚNG
            if (card.repetitions == 0) {
                card.interval = 1; // Học lần đầu thành công -> 1 ngày sau ôn lại
            } else if (card.repetitions == 1) {
                card.interval = 6; // Học lần 2 thành công -> 6 ngày sau ôn lại
            } else {
                // Từ lần 3 trở đi: Khoảng cách = Khoảng cách cũ * Độ dễ (Ease Factor)
                card.interval = (int) Math.round(card.interval * card.easeFactor);
            }
            card.repetitions++; // Tăng số lần học thành công
        } else { // Trường hợp người dùng trả lời SAI
            card.repetitions = 0; // Reset số lần lặp về 0 (Thẻ quay lại trạng thái NEW)
            card.interval = 1;    // Ngày mai phải ôn lại ngay
        }

        // Tính toán lại độ dễ Ease Factor mới (Theo công thức chuẩn SM-2)
        card.easeFactor = card.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
        if (card.easeFactor < 1.3) {
            card.easeFactor = 1.3; // Độ dễ tối thiểu không được thấp hơn 1.3
        }

        // Tính toán mốc thời gian học tiếp theo (nextReview)
        // Thời điểm tiếp theo = Hiện tại + (Khoảng cách ngày * số mili-giây của 1 ngày)
        long oneDayMillis = 24 * 60 * 60 * 1000L;
        card.nextReview = System.currentTimeMillis() + (card.interval * oneDayMillis);

        // Đồng bộ dữ liệu mới lên Firestore
        if (card.cardId != null && !card.cardId.isEmpty()) {
            db.collection("decks").document(deckId).collection("flashcards").document(card.cardId).update("interval", card.interval, "easeFactor", card.easeFactor, "repetitions", card.repetitions, "nextReview", card.nextReview, "statusString", card.getStatusString() // Cập nhật luôn chuỗi status mới xuống DB
            ).addOnSuccessListener(aVoid -> Log.d("SM2_UPDATE", "Đã cập nhật thuật toán cho thẻ: " + card.front)).addOnFailureListener(e -> Log.e("SM2_UPDATE", "Lỗi cập nhật thẻ lên Firestore", e));
        }
    }
}
