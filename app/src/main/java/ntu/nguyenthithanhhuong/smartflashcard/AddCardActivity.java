package ntu.nguyenthithanhhuong.smartflashcard;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import ntu.nguyenthithanhhuong.smartflashcard.Model.Flashcard;

public class AddCardActivity extends AppCompatActivity {
    private EditText edtDeckName, edtFront, edtBack, edtIpa, edtExample;
    private Button btnAiGen, btnSave;
    private ProgressBar progressBar;
    private GroqManager groqManager;
    private FirebaseFirestore db;
    private String currentDeckId;
    private boolean isCreateDeckMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_card);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.addCard), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        currentDeckId = getIntent().getStringExtra("DECK_ID");
        isCreateDeckMode = (currentDeckId == null || currentDeckId.trim().isEmpty());

        db = FirebaseFirestore.getInstance();
        groqManager = new GroqManager();

        initViews();
        setupModeUi();

        btnAiGen.setOnClickListener(v -> {
            String word = edtFront.getText().toString().trim();
            if (word.isEmpty()) return;

            progressBar.setVisibility(View.VISIBLE);
            btnAiGen.setEnabled(false);
            groqManager.generateCardContent(word, new GroqManager.AiCallback() {
                @Override
                public void onSuccess(String definition, String ipa, String example) {
                    progressBar.setVisibility(View.GONE);
                    btnAiGen.setEnabled(true);
                    edtBack.setText(definition);
                    edtIpa.setText(ipa);
                    edtExample.setText(example);
                }

                @Override
                public void onError(String error) {
                    progressBar.setVisibility(View.GONE);
                    btnAiGen.setEnabled(true);
                    Toast.makeText(AddCardActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnSave.setOnClickListener(v -> saveCardToFirestore());
    }

    private void initViews() {
        edtDeckName = findViewById(R.id.edtDeckName);
        edtFront = findViewById(R.id.edtFront);
        edtBack = findViewById(R.id.edtBack);
        edtIpa = findViewById(R.id.edtIpa);
        edtExample = findViewById(R.id.edtExample);
        btnAiGen = findViewById(R.id.btnAiGen);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupModeUi() {
        if (isCreateDeckMode) {
            edtDeckName.setVisibility(View.VISIBLE);
            btnSave.setText("Tạo bộ & lưu thẻ");
        } else {
            edtDeckName.setVisibility(View.GONE);
            btnSave.setText("Lưu thẻ vào Firebase");
        }
    }

    private void saveCardToFirestore() {
        String deckName = edtDeckName.getText().toString().trim();
        String front = edtFront.getText().toString();
        String back = edtBack.getText().toString();
        String ipa = edtIpa.getText().toString();
        String example = edtExample.getText().toString();

        if (isCreateDeckMode && deckName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên bộ sưu tập!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (front.isEmpty() || back.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Dùng Model Flashcard đã tạo ở bước trước
        Flashcard newCard = new Flashcard(front, back, ipa, example);
        newCard.nextReview = System.currentTimeMillis(); // Học ngay hôm nay

        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        if (isCreateDeckMode) {
            createDeckThenAddCard(deckName, newCard);
        } else {
            addCardToDeck(currentDeckId, newCard, false);
        }
    }

    private void createDeckThenAddCard(String deckName, Flashcard firstCard) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            btnSave.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> deckData = new HashMap<>();
        deckData.put("name", deckName);
        deckData.put("ownerId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        deckData.put("cardCount", 0);
        deckData.put("createdAt", System.currentTimeMillis());

        db.collection("decks")
                .add(deckData)
                .addOnSuccessListener(ref -> {
                    currentDeckId = ref.getId();
                    isCreateDeckMode = false;
                    setupModeUi();
                    addCardToDeck(currentDeckId, firstCard, true);
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tạo bộ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addCardToDeck(String deckId, Flashcard card, boolean createdDeckNow) {
        db.collection("decks").document(deckId)
                .collection("flashcards")
                .add(card)
                .addOnSuccessListener(documentReference -> {
                    db.collection("decks").document(deckId)
                            .update("cardCount", FieldValue.increment(1));

                    btnSave.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(
                            this,
                            createdDeckNow ? "Đã tạo bộ & thêm thẻ thành công!" : "Thêm thẻ thành công!",
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}