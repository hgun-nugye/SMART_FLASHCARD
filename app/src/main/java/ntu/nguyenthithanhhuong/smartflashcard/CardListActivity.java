package ntu.nguyenthithanhhuong.smartflashcard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ntu.nguyenthithanhhuong.smartflashcard.Model.Flashcard;

public class CardListActivity extends BaseAppActivity {

    private RecyclerView rvCards;
    private FloatingActionButton fabAddCard;
    private TextView tvTotalCards, tvDueCards, tvLearnedCards;
    private FirebaseFirestore db;
    private String deckId;
    private FlashcardAdapter adapter;
    private final List<Flashcard> allCards = new ArrayList<>();
    private final List<Flashcard> filteredCards = new ArrayList<>();
    private String currentSearchQuery = "";
    private SearchView toolbarSearchView;
    private TextToSpeech tts;
    private boolean isTtsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_card_list);
        EdgeToEdgeHelper.applyCoordinatorInsets(
                findViewById(R.id.cardList),
                findViewById(R.id.appBarLayoutCards)
        );

        deckId = getIntent().getStringExtra("DECK_ID");
        if (deckId == null || deckId.isEmpty()) {
            Toast.makeText(this, R.string.deck_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                isTtsReady = true;
            }
        });

        initViews();
        loadCardsFromFirestore();
    }

    private void initViews() {
        rvCards = findViewById(R.id.rvCards);
        fabAddCard = findViewById(R.id.fabAddCard);
        tvTotalCards = findViewById(R.id.tvTotalCards);
        tvDueCards = findViewById(R.id.tvDueCards);
        tvLearnedCards = findViewById(R.id.tvLearnedCards);

        MaterialToolbar toolbar = findViewById(R.id.toolbarCards);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String deckName = getIntent().getStringExtra("DECK_NAME");
        if (deckName != null && !deckName.trim().isEmpty()) {
            toolbar.setTitle(deckName);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvCards.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FlashcardAdapter(filteredCards, new FlashcardAdapter.OnCardSpeakClickListener() {
            @Override
            public void onSpeakClick(String word) {
                if (isTtsReady && tts != null && word != null && !word.isEmpty()) {
                    tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }

            @Override
            public void onDetailClick(Flashcard card) {
                Intent intent = new Intent(CardListActivity.this, CardDetailActivity.class);
                intent.putExtra("CARD_DATA", card);
                startActivity(intent);
            }
        });

        rvCards.setAdapter(adapter);

        fabAddCard.setOnClickListener(v -> {
            Intent intent = new Intent(CardListActivity.this, AddCardActivity.class);
            intent.putExtra("DECK_ID", deckId);
            intent.putExtra("DECK_NAME", getIntent().getStringExtra("DECK_NAME"));
            intent.putExtra("DECK_DESCRIPTION", getIntent().getStringExtra("DECK_DESCRIPTION"));
            startActivity(intent);
        });
    }

    private void loadCardsFromFirestore() {
        db.collection("decks").document(deckId)
                .collection("flashcards")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FIRESTORE_CARD_ERROR", error.getMessage());
                        return;
                    }
                    if (value == null) return;

                    allCards.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Flashcard card = doc.toObject(Flashcard.class);
                        if (card != null) {
                            card.cardId = doc.getId();
                            allCards.add(card);
                        }
                    }

                    updateStats();
                    filterCards(currentSearchQuery);
                });
    }

    private void updateStats() {
        int due = 0;
        int learned = 0;
        for (Flashcard card : allCards) {
            switch (card.getStatus()) {
                case REVIEW:
                    due++;
                    break;
                case LEARNED:
                    learned++;
                    break;
                default:
                    break;
            }
        }
        tvTotalCards.setText(String.valueOf(allCards.size()));
        tvDueCards.setText(String.valueOf(due));
        tvLearnedCards.setText(String.valueOf(learned));
    }

    private void filterCards(String query) {
        currentSearchQuery = query != null ? query : "";
        filteredCards.clear();

        if (currentSearchQuery.trim().isEmpty()) {
            filteredCards.addAll(allCards);
        } else {
            String pattern = currentSearchQuery.toLowerCase(Locale.ROOT).trim();
            for (Flashcard card : allCards) {
                if (matchesCard(card, pattern)) {
                    filteredCards.add(card);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private boolean matchesCard(Flashcard card, String pattern) {
        return containsIgnoreCase(card.front, pattern)
                || containsIgnoreCase(card.back, pattern)
                || containsIgnoreCase(card.ipa, pattern)
                || containsIgnoreCase(card.example, pattern);
    }

    private boolean containsIgnoreCase(String value, String pattern) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(pattern);
    }

    private void resetSearch() {
        currentSearchQuery = "";
        if (toolbarSearchView != null) {
            toolbarSearchView.setQuery("", false);
        }
        filteredCards.clear();
        filteredCards.addAll(allCards);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.card_list_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                    resetSearch();
                    return true;
                }
            });

            SearchView searchView = (SearchView) searchItem.getActionView();
            toolbarSearchView = searchView;
            if (searchView != null) {
                @SuppressLint("RestrictedApi")
                SearchView.SearchAutoComplete searchAutoComplete =
                        searchView.findViewById(androidx.appcompat.R.id.search_src_text);
                if (searchAutoComplete != null) {
                    searchAutoComplete.setTextColor(0xFFFFFFFF);
                    searchAutoComplete.setHintTextColor(0x80FFFFFF);
                }

                ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
                if (searchIcon != null) {
                    searchIcon.setColorFilter(0xFFFFFFFF);
                }

                ImageView closeIcon = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
                if (closeIcon != null) {
                    closeIcon.setColorFilter(0xFFFFFFFF);
                }

                searchView.setQueryHint(getString(R.string.card_list_search_hint));
                searchView.setOnCloseListener(() -> {
                    resetSearch();
                    return false;
                });
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        filterCards(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        filterCards(newText);
                        return true;
                    }
                });
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_manage_cards) {
            Intent intent = new Intent(this, CardManageActivity.class);
            intent.putExtra("DECK_ID", deckId);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_review_cards) {
            Intent intent = new Intent(this, ReviewActivity.class);
            intent.putExtra("DECK_ID", deckId);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
