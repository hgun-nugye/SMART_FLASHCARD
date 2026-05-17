package ntu.nguyenthithanhhuong.smartflashcard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import ntu.nguyenthithanhhuong.smartflashcard.Model.Flashcard;

public class CardListActivity extends AppCompatActivity {

    private RecyclerView rvCards;
    private FloatingActionButton fabAddCard;
    private FirebaseFirestore db;
    private String deckId;
    private FlashcardAdapter adapter;
    private final List<Flashcard> cardList = new ArrayList<>();
    private android.speech.tts.TextToSpeech tts;
    private boolean isTtsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_card_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cardList), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        deckId = getIntent().getStringExtra("DECK_ID");
        if (deckId == null || deckId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bộ sưu tập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        // Khởi tạo tính năng phát âm TextToSpeech
        tts = new android.speech.tts.TextToSpeech(this, status -> {
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts.setLanguage(java.util.Locale.US);
                isTtsReady = true;
            }
        });

        initViews();
        loadCardsFromFirestore();
    }

    private void initViews() {
        rvCards = findViewById(R.id.rvCards);
        fabAddCard = findViewById(R.id.fabAddCard);
        MaterialToolbar toolbar = findViewById(R.id.toolbarCards);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Tạo nút quay lại mũi tên góc trái
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvCards.setLayoutManager(new LinearLayoutManager(this));

        // Gắn sự kiện click phát âm từ adapter truyền ra
        adapter = new FlashcardAdapter(cardList, word -> {
            if (isTtsReady && tts != null && word != null && !word.isEmpty()) {
                tts.speak(word, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
        rvCards.setAdapter(adapter);

        // Bấm nút + để chuyển sang AddCardActivity và truyền kèm DECK_ID hiện tại
        fabAddCard.setOnClickListener(v -> {
            Intent intent = new Intent(CardListActivity.this, AddCardActivity.class);
            intent.putExtra("DECK_ID", deckId);
            startActivity(intent);
        });
    }

    private void loadCardsFromFirestore() {
        // Truy vấn sâu vào sub-collection "flashcards" nằm trong bộ sưu tập này
        db.collection("decks").document(deckId)
                .collection("flashcards")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FIRESTORE_CARD_ERROR", error.getMessage());
                        return;
                    }
                    if (value == null) return;

                    cardList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Flashcard card = doc.toObject(Flashcard.class);
                        if (card != null) {
                            cardList.add(card);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
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