package ntu.nguyenthithanhhuong.smartflashcard;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import ntu.nguyenthithanhhuong.smartflashcard.Model.Flashcard;

public class CardManageActivity extends BaseAppActivity {

    private RecyclerView rvManageCards;
    private SearchView searchViewCard;
    private FirebaseFirestore db;
    private String deckId;
    private CardManageAdapter adapter;
    private final List<Flashcard> allCards = new ArrayList<>(); // Danh sách gốc
    private final List<Flashcard> filteredList = new ArrayList<>(); // Danh sách sau khi tìm kiếm

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_card_manage);
        EdgeToEdgeHelper.applyScreenWithToolbar(
                findViewById(R.id.cardManageRoot),
                findViewById(R.id.toolbarManage)
        );

        deckId = getIntent().getStringExtra("DECK_ID");
        db = FirebaseFirestore.getInstance();

        initViews();
        loadCardsFromFirestore();
        setupSearchView();
    }

    private void initViews() {
        rvManageCards = findViewById(R.id.rvManageCards);
        searchViewCard = findViewById(R.id.searchViewCard);
        MaterialToolbar toolbar = findViewById(R.id.toolbarManage);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvManageCards.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CardManageAdapter(filteredList, new CardManageAdapter.OnCardActionListener() {
            @Override
            public void onDelete(Flashcard card) {
                confirmDeleteCard(card);
            }
        });
        rvManageCards.setAdapter(adapter);
    }

    private void loadCardsFromFirestore() {
        db.collection("decks").document(deckId)
                .collection("flashcards")
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    allCards.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Flashcard card = doc.toObject(Flashcard.class);
                        if (card != null) {
                            card.cardId = doc.getId(); // Gán ID tài liệu để xóa/sửa
                            allCards.add(card);
                        }
                    }
                    filterList(searchViewCard.getQuery().toString()); // Cập nhật lại danh sách hiển thị
                });
    }

    private void setupSearchView() {
        searchViewCard.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });
    }

    private void filterList(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(allCards);
        } else {
            for (Flashcard card : allCards) {
                if (card.front.toLowerCase().contains(text.toLowerCase())
                        || card.back.toLowerCase().contains(text.toLowerCase())) {
                    filteredList.add(card);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Hỏi xác nhận trước khi xóa thẻ khỏi Firebase
    private void confirmDeleteCard(Flashcard card) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa thẻ từ")
                .setMessage("Bạn có chắc chắn muốn xóa từ '" + card.front + "' không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.collection("decks").document(deckId)
                            .collection("flashcards").document(card.cardId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Cập nhật giảm trừ số lượng thẻ của bộ sưu tập
                                db.collection("decks").document(deckId)
                                        .update("cardCount", FieldValue.increment(-1));
                                Toast.makeText(this, "Đã xóa thẻ!", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}