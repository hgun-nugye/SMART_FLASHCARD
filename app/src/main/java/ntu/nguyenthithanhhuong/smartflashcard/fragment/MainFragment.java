package ntu.nguyenthithanhhuong.smartflashcard.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import ntu.nguyenthithanhhuong.smartflashcard.card.AddCardActivity;
import ntu.nguyenthithanhhuong.smartflashcard.card.CardListActivity;
import ntu.nguyenthithanhhuong.smartflashcard.deck.DeckAdapter;
import ntu.nguyenthithanhhuong.smartflashcard.deck.DeckManageActivity;
import ntu.nguyenthithanhhuong.smartflashcard.EdgeToEdgeHelper;
import ntu.nguyenthithanhhuong.smartflashcard.R;
import ntu.nguyenthithanhhuong.smartflashcard.login.UserProfileHelper;
import ntu.nguyenthithanhhuong.smartflashcard.model.Deck;
import ntu.nguyenthithanhhuong.smartflashcard.model.Flashcard;
import ntu.nguyenthithanhhuong.smartflashcard.model.User;

public class MainFragment extends Fragment {
    private static final String TAG = "MainFragment";
    private TextView tvTotalDecks, tvDueCount, tvLearnedCount;
    private RecyclerView rvDecks;
    private FloatingActionButton fabAddDeck;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DeckAdapter adapter;

    private final List<Deck> deckList = new ArrayList<>();
    private final List<Deck> filteredList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        EdgeToEdgeHelper.applyScreenWithToolbar(view, toolbar);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            loadUserProfile();
            loadDecksFromFirestore();
        }
    }

    private void initViews(View view) {
        rvDecks = view.findViewById(R.id.rvDecks);
        tvTotalDecks = view.findViewById(R.id.tvTotalDecks);
        tvDueCount = view.findViewById(R.id.tvDueCount);
        tvLearnedCount = view.findViewById(R.id.tvLearnedCount);
        fabAddDeck = view.findViewById(R.id.fabAddDeck);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.main_menu);

            // Ép cấu hình hiển thị màu chữ trắng cho SearchView trực tiếp từ mã nguồn
            MenuItem searchItem = toolbar.getMenu().findItem(R.id.action_search);
            if (searchItem != null) {
                SearchView searchView = (SearchView) searchItem.getActionView();
                if (searchView != null) {

                    @SuppressLint("RestrictedApi") SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
                    if (searchAutoComplete != null) {
                        searchAutoComplete.setTextColor(android.graphics.Color.WHITE);
                        searchAutoComplete.setHintTextColor(android.graphics.Color.parseColor("#80FFFFFF"));
                    }

                    int closeIconId = androidx.appcompat.R.id.search_close_btn;
                    ImageView closeIcon = searchView.findViewById(closeIconId);
                    if (closeIcon != null) {
                        closeIcon.setColorFilter(android.graphics.Color.WHITE);
                    }

                    searchView.setQueryHint(getString(R.string.search_decks_hint));
                    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextSubmit(String query) {
                            filterDecks(query);
                            return true;
                        }

                        @Override
                        public boolean onQueryTextChange(String newText) {
                            filterDecks(newText);
                            return true;
                        }
                    });
                }
            }

            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_manage_decks) {
                    startActivity(new Intent(requireContext(), DeckManageActivity.class));
                    return true;
                }
                return false;
            });
        }

        rvDecks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDecks.setNestedScrollingEnabled(false);

        adapter = new DeckAdapter(filteredList, deck -> {
            Intent intent = new Intent(requireContext(), CardListActivity.class);
            intent.putExtra("DECK_ID", deck.deckId);
            intent.putExtra("DECK_NAME", deck.name);
            intent.putExtra("DECK_DESCRIPTION", deck.description);
            startActivity(intent);
        });
        rvDecks.setAdapter(adapter);

        fabAddDeck.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddCardActivity.class))
        );
    }

    private void loadDecksFromFirestore() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("decks")
                .whereEqualTo("ownerId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Firestore error: " + error.getMessage());
                        return;
                    }
                    if (value == null) return;

                    deckList.clear();
                    filteredList.clear();
                    tvTotalDecks.setText(String.valueOf(value.size()));

                    final int[] totalDue = {0};
                    final int[] totalLearned = {0};

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Deck deck = doc.toObject(Deck.class);
                        if (deck != null) {
                            deck.deckId = doc.getId();
                            deckList.add(deck);
                            filteredList.add(deck);

                            final String deckId = deck.deckId;

                            db.collection("decks")
                                    .document(deckId)
                                    .collection("flashcards")
                                    .get()
                                    .addOnSuccessListener(cardSnapshot -> {
                                        int total = 0;
                                        int newCount = 0;
                                        int dueCount = 0;
                                        int learnedCount = 0;

                                        for (DocumentSnapshot cardDoc : cardSnapshot.getDocuments()) {
                                            Flashcard card = cardDoc.toObject(Flashcard.class);
                                            if (card == null) continue;

                                            total++;
                                            switch (card.getStatus()) {
                                                case NEW:     newCount++;     break;
                                                case DUE:  dueCount++;     break;
                                                case LEARNED: learnedCount++; break;
                                            }
                                        }

                                        totalDue[0] += dueCount;
                                        totalLearned[0] += learnedCount;

                                        tvDueCount.setText(String.valueOf(totalDue[0]));
                                        tvLearnedCount.setText(String.valueOf(totalLearned[0]));

                                        for (int i = 0; i < filteredList.size(); i++) {
                                            Deck d = filteredList.get(i);
                                            if (deckId.equals(d.deckId)) {
                                                d.cardCount = total;
                                                d.newCount = newCount;
                                                d.dueCount = dueCount;
                                                d.learnedCount = learnedCount;
                                                adapter.notifyItemChanged(i);
                                                break;
                                            }
                                        }

                                        for (Deck d : deckList) {
                                            if (deckId.equals(d.deckId)) {
                                                d.cardCount = total;
                                                d.newCount = newCount;
                                                d.dueCount = dueCount;
                                                d.learnedCount = learnedCount;
                                                break;
                                            }
                                        }
                                    });
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        UserProfileHelper.ensureUserProfile(currentUser, new UserProfileHelper.Callback() {
            @Override
            public void onReady(User user) {
                if (user != null && user.fullName != null && getView() != null) {
                    MaterialToolbar toolbar = getView().findViewById(R.id.toolbar);
                    if (toolbar != null) {
                        toolbar.setTitle(getString(R.string.main_greeting, user.fullName));
                    }
                }
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Failed to load user profile: " + message);
            }
        });
    }

    private void filterDecks(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(deckList);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (Deck deck : deckList) {
                if (deck.name != null && deck.name.toLowerCase().contains(filterPattern)) {
                    filteredList.add(deck);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}