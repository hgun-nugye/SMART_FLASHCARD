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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
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
    private MaterialToolbar toolbar;
    private MaterialCardView cvCard, layoutResult;
    private LinearProgressIndicator progressIndicator;

    private MaterialButton btnAction,
            btnCorrect,
            btnIncorrect,
            btnFinishReview;

    private LinearLayout llActionButtons;

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
        progressIndicator = findViewById(R.id.progressIndicator);
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

            // Xoay nhẹ
            android.animation.ObjectAnimator tiltOut =
                    android.animation.ObjectAnimator.ofFloat(
                            cvCard,
                            "rotationY",
                            0f,
                            12f
                    );

            tiltOut.setDuration(180);

            tiltOut.addListener(new android.animation.AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(android.animation.Animator animation) {

                    tvBack.setVisibility(View.VISIBLE);
                    divider.setVisibility(View.VISIBLE);

                    btnAction.setVisibility(View.GONE);
                    llActionButtons.setVisibility(View.VISIBLE);

                    android.animation.ObjectAnimator tiltBack =
                            android.animation.ObjectAnimator.ofFloat(
                                    cvCard,
                                    "rotationY",
                                    12f,
                                    0f
                            );

                    tiltBack.setDuration(180);

                    tiltBack.addListener(new android.animation.AnimatorListenerAdapter() {

                        @Override
                        public void onAnimationEnd(android.animation.Animator animation) {

                            btnAction.setEnabled(true);
                        }
                    });

                    tiltBack.start();
                }
            });

            tiltOut.start();
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
        slideOut.setDuration(180);
        slideOut.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                currentIndex++;
                showCurrentCard();
                cvCard.setAlpha(0f);
                cvCard.animate()
                        .alpha(1f)
                        .setDuration(150)
                        .start();

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
            progressIndicator.setProgress(100);

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
        int progress = (int) (((float) currentIndex / reviewCards.size()) * 100);
        progressIndicator.setProgress(progress);

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
