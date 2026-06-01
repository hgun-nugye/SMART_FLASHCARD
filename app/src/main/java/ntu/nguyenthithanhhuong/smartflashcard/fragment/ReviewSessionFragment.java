package ntu.nguyenthithanhhuong.smartflashcard.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ntu.nguyenthithanhhuong.smartflashcard.EdgeToEdgeHelper;
import ntu.nguyenthithanhhuong.smartflashcard.R;
import ntu.nguyenthithanhhuong.smartflashcard.model.Flashcard;

public class ReviewSessionFragment extends Fragment {

    private static final String ARG_DECK_ID = "DECK_ID";
    private String deckId;
    private FirebaseFirestore db;
    private final List<Flashcard> reviewCards = new ArrayList<>();
    private int currentIndex = 0;

    private int correctCount = 0;
    private int incorrectCount = 0;

    private TextView tvFront, tvBack, tvProgress;
    private TextView tvResultCorrect, tvResultIncorrect;
    private View divider;
    private MaterialToolbar toolbar;
    private MaterialCardView cvCard, layoutResult;
    private LinearProgressIndicator progressIndicator;

    private MaterialButton btnAction, btnCorrect, btnIncorrect, btnFinishReview;
    private LinearLayout llActionButtons;
    private TextToSpeech tts;
    private boolean isTtsReady = false;
    private ImageButton btnPlayTts;

    public static ReviewSessionFragment newInstance(String deckId) {
        ReviewSessionFragment fragment = new ReviewSessionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DECK_ID, deckId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deckId = getArguments().getString(ARG_DECK_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (deckId == null || deckId.isEmpty()) {
            Toast.makeText(requireContext(), R.string.deck_not_found, Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        db = FirebaseFirestore.getInstance();
        initViews(view);

        EdgeToEdgeHelper.applyScreenWithToolbar(view, toolbar);

        loadCards();

        tts = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(java.util.Locale.US);
                if (result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsReady = true;
                }
            }
        });

        btnPlayTts.setOnClickListener(v -> {
            String word = tvFront.getText().toString().trim();
            if (!word.isEmpty() && isTtsReady && tts != null) {
                tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });


    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbarReview);
        btnPlayTts = view.findViewById(R.id.btnPlayTts);

        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        progressIndicator = view.findViewById(R.id.progressIndicator);
        cvCard = view.findViewById(R.id.cvCard);
        tvFront = view.findViewById(R.id.tvFront);
        tvBack = view.findViewById(R.id.tvBack);
        divider = view.findViewById(R.id.divider);
        tvProgress = view.findViewById(R.id.tvProgress);

        btnAction = view.findViewById(R.id.btnAction);
        btnCorrect = view.findViewById(R.id.btnCorrect);
        btnIncorrect = view.findViewById(R.id.btnIncorrect);
        btnFinishReview = view.findViewById(R.id.btnFinishReview);

        llActionButtons = view.findViewById(R.id.llActionButtons);
        layoutResult = view.findViewById(R.id.layoutResult);
        tvResultCorrect = view.findViewById(R.id.tvResultCorrect);
        tvResultIncorrect = view.findViewById(R.id.tvResultIncorrect);

        float scale = getResources().getDisplayMetrics().density;
        cvCard.setCameraDistance(8000 * scale);

        btnAction.setOnClickListener(v -> {
            if (reviewCards.isEmpty()) return;

            btnAction.setEnabled(false);
            ObjectAnimator tiltOut = ObjectAnimator.ofFloat(cvCard, "rotationY", 0f, 12f);
            tiltOut.setDuration(180);
            tiltOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    tvBack.setVisibility(View.VISIBLE);
                    divider.setVisibility(View.VISIBLE);

                    btnAction.setVisibility(View.GONE);
                    llActionButtons.setVisibility(View.VISIBLE);

                    ObjectAnimator tiltBack = ObjectAnimator.ofFloat(cvCard, "rotationY", 12f, 0f);
                    tiltBack.setDuration(180);
                    tiltBack.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            btnAction.setEnabled(true);
                        }
                    });
                    tiltBack.start();
                }
            });
            tiltOut.start();
        });

        btnCorrect.setOnClickListener(v -> {
            correctCount++;
            if (currentIndex < reviewCards.size()) {
                Flashcard currentCard = reviewCards.get(currentIndex);
                applyReviewAndSync(currentCard, true);
            }
            moveToNextCard();
        });

        btnIncorrect.setOnClickListener(v -> {
            incorrectCount++;
            if (currentIndex < reviewCards.size()) {
                Flashcard currentCard = reviewCards.get(currentIndex);
                applyReviewAndSync(currentCard, false);
            }
            moveToNextCard();
        });

        btnFinishReview.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void moveToNextCard() {
        btnCorrect.setEnabled(false);
        btnIncorrect.setEnabled(false);

        ObjectAnimator slideOut = ObjectAnimator.ofFloat(cvCard, "translationX", 0f, -cvCard.getWidth());
        slideOut.setDuration(180);
        slideOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentIndex++;
                showCurrentCard();
                cvCard.setAlpha(0f);
                cvCard.animate().alpha(1f).setDuration(150).start();

                if (currentIndex < reviewCards.size()) {
                    cvCard.setTranslationX(cvCard.getWidth());
                    ObjectAnimator slideIn = ObjectAnimator.ofFloat(cvCard, "translationX", cvCard.getWidth(), 0f);
                    slideIn.setDuration(250);
                    slideIn.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
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
                    if (!isAdded()) return;

                    reviewCards.clear();
                    List<Flashcard> allCardsInDeck = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Flashcard card = doc.toObject(Flashcard.class);
                        if (card != null) {
                            card.cardId = doc.getId();
                            allCardsInDeck.add(card);
                            if (card.isDue()) {
                                reviewCards.add(card);
                            }
                        }
                    }

                    if (reviewCards.isEmpty() && !allCardsInDeck.isEmpty()) {
                        reviewCards.addAll(allCardsInDeck);
                        Toast.makeText(requireContext(), "Bạn đang ôn tập lại tất cả các thẻ!", Toast.LENGTH_SHORT).show();
                    }

                    Collections.shuffle(reviewCards);

                    currentIndex = 0;
                    correctCount = 0;
                    incorrectCount = 0;
                    showCurrentCard();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e("REVIEW_SESSION", "Error loading cards", e);
                    Toast.makeText(requireContext(),
                            getString(R.string.review_load_error, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showCurrentCard() {
        if (reviewCards.isEmpty()) {
            cvCard.setVisibility(View.GONE);
            btnAction.setVisibility(View.GONE);
            llActionButtons.setVisibility(View.GONE);
            tvProgress.setVisibility(View.GONE);

            layoutResult.setVisibility(View.VISIBLE);
            tvResultCorrect.setText(R.string.review_no_due);
            tvResultIncorrect.setVisibility(View.GONE);
            return;
        }

        if (currentIndex >= reviewCards.size()) {
            cvCard.setVisibility(View.GONE);
            btnAction.setVisibility(View.GONE);
            llActionButtons.setVisibility(View.GONE);
            tvProgress.setVisibility(View.GONE);
            progressIndicator.setProgress(100);

            layoutResult.setVisibility(View.VISIBLE);
            tvResultCorrect.setText(getString(R.string.review_result_correct, correctCount));
            tvResultIncorrect.setText(getString(R.string.review_result_incorrect, incorrectCount));

            ReviewFragment.saveLastSession(requireContext(), correctCount, incorrectCount);
            return;
        }

        Flashcard currentCard = reviewCards.get(currentIndex);

        cvCard.setVisibility(View.VISIBLE);
        tvFront.setVisibility(View.VISIBLE);
        tvFront.setText(currentCard.front);

        tvBack.setVisibility(View.GONE);
        tvBack.setText(currentCard.back);

        divider.setVisibility(View.GONE);

        btnAction.setVisibility(View.VISIBLE);
        llActionButtons.setVisibility(View.GONE);

        tvProgress.setVisibility(View.VISIBLE);
        tvProgress.setText(getString(R.string.review_progress, currentIndex + 1, reviewCards.size()));
        int progress = (int) (((float) currentIndex / reviewCards.size()) * 100);
        progressIndicator.setProgress(progress);

        layoutResult.setVisibility(View.GONE);
    }

    private void applyReviewAndSync(Flashcard card, boolean isCorrect) {
        card.applyReviewResult(isCorrect);

        if (card.cardId == null || card.cardId.isEmpty()) return;

        db.collection("decks").document(deckId)
                .collection("flashcards")
                .document(card.cardId)
                .update(
                        "interval", card.interval,
                        "easeFactor", card.easeFactor,
                        "repetitions", card.repetitions,
                        "nextReview", card.nextReview,
                        "statusString", card.getStatusString()
                )
                .addOnSuccessListener(aVoid -> Log.d("SM2_UPDATE", "Updated card: " + card.front))
                .addOnFailureListener(e -> Log.e("SM2_UPDATE", "Firestore update failed", e));
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
