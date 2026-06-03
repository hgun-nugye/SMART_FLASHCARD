package ntu.nguyenthithanhhuong.smartflashcard.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import ntu.nguyenthithanhhuong.smartflashcard.EdgeToEdgeHelper;
import ntu.nguyenthithanhhuong.smartflashcard.R;
import ntu.nguyenthithanhhuong.smartflashcard.deck.ReviewDeckAdapter;
import ntu.nguyenthithanhhuong.smartflashcard.model.Deck;
import ntu.nguyenthithanhhuong.smartflashcard.model.Flashcard;

public class ReviewFragment extends Fragment {
    private static final String PREFS = "review_session_prefs";
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private final List<Deck> reviewDecks = new ArrayList<>();
    private ReviewDeckAdapter adapter;
    private TextView tvReviewEmpty;
    private TextView tvLastResult;
    private MaterialCardView cardLastResult;
    private RecyclerView rvReviewDecks;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvReviewDecks = view.findViewById(R.id.rvReviewDecks);
        tvReviewEmpty = view.findViewById(R.id.tvReviewEmpty);
        tvLastResult = view.findViewById(R.id.tvLastResult);
        cardLastResult = view.findViewById(R.id.cardLastResult);

        rvReviewDecks.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ReviewDeckAdapter(reviewDecks, deck -> {
            ReviewSessionFragment sessionFragment = ReviewSessionFragment.newInstance(deck.deckId);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, sessionFragment)
                    .addToBackStack(null)
                    .commit();
        });
        rvReviewDecks.setAdapter(adapter);

        showLastSession();
    }

    @Override
    public void onStart() {
        super.onStart();
        loadDecks();
    }

    private void showLastSession() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int correct = prefs.getInt("last_correct", -1);
        int incorrect = prefs.getInt("last_incorrect", -1);
        if (correct >= 0 && incorrect >= 0) {
            cardLastResult.setVisibility(View.VISIBLE);
            tvLastResult.setText(getString(R.string.review_last_score, correct, incorrect));
        } else {
            cardLastResult.setVisibility(View.GONE);
        }
    }

    private void loadDecks() {
        if (auth.getCurrentUser() == null) {
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        db.collection("decks")
                .whereEqualTo("ownerId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded() || error != null || value == null) {
                        return;
                    }

                    reviewDecks.clear();
                    List<Deck> pending = new ArrayList<>();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Deck deck = doc.toObject(Deck.class);
                        if (deck == null) {
                            continue;
                        }
                        deck.deckId = doc.getId();
                        pending.add(deck);
                    }

                    if (pending.isEmpty()) {
                        updateEmptyState();
                        return;
                    }

                    reviewDecks.addAll(pending);
                    adapter.notifyDataSetChanged();

                    final int[] remaining = {pending.size()};
                    for (Deck deck : pending) {
                        db.collection("decks").document(deck.deckId)
                                .collection("flashcards")
                                .get()
                                .addOnSuccessListener(snap -> {
                                    if (!isAdded()) return;

                                    int due = 0;
                                    for (DocumentSnapshot cardDoc : snap.getDocuments()) {
                                        Flashcard card = cardDoc.toObject(Flashcard.class);
                                        if (card != null && card.isDue()) {
                                            due++;
                                        }
                                    }
                                    deck.dueCount = due;

                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        adapter.notifyDataSetChanged();
                                        updateEmptyState();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        adapter.notifyDataSetChanged();
                                        updateEmptyState();
                                    }
                                });
                    }
                });
    }

    private void updateEmptyState() {
        boolean empty = reviewDecks.isEmpty();
        tvReviewEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvReviewDecks.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvReviewEmpty.setText(empty ? "Bạn chưa tạo bộ thẻ nào." : "");
    }

    public static void saveLastSession(Context context, int correct, int incorrect) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt("last_correct", correct)
                .putInt("last_incorrect", incorrect)
                .apply();
    }
}