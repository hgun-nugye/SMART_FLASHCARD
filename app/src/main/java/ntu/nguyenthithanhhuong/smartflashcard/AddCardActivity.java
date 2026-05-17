package ntu.nguyenthithanhhuong.smartflashcard;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
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
    private android.speech.tts.TextToSpeech tts;
    private boolean isTtsReady = false;
    private android.widget.ImageButton btnPlayTts;

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

        // 1. Khởi tạo TextToSpeech trước để sẵn sàng sử dụng
        tts = new android.speech.tts.TextToSpeech(this, status -> {
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(java.util.Locale.US);
                if (result == android.speech.tts.TextToSpeech.LANG_MISSING_DATA
                        || result == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsReady = false;
                } else {
                    isTtsReady = true;
                }
            }
        });

        // Bấm nút Play để chủ động nghe đọc
        btnPlayTts.setOnClickListener(v -> {
            String word = edtFront.getText().toString().trim();
            if (!word.isEmpty() && isTtsReady && tts != null) {
                tts.speak(word, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null);
            } else if (word.isEmpty()) {
                Toast.makeText(this, "Hãy nhập từ vựng trước khi nghe!", Toast.LENGTH_SHORT).show();
            }
        });

        // 2. Cấu hình nút gọi AI sinh nội dung
        btnAiGen.setOnClickListener(v -> {
            String word = edtFront.getText().toString().trim();
            if (word.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập từ vựng mặt trước!", Toast.LENGTH_SHORT).show();
                return;
            }

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

                    // Phát âm từ vựng vừa tra cứu nếu TTS đã sẵn sàng
                    if (isTtsReady && tts != null) {
                        tts.speak(word, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null);
                    }
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
        btnPlayTts = findViewById(R.id.btnPlayTts);
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
        String front = edtFront.getText().toString().trim();
        String back = edtBack.getText().toString().trim();
        String ipa = edtIpa.getText().toString().trim();
        String example = edtExample.getText().toString().trim();

        if (isCreateDeckMode && deckName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên bộ sưu tập!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (front.isEmpty() || back.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        Flashcard newCard = new Flashcard(front, back, ipa, example);
        newCard.nextReview = System.currentTimeMillis();

        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        if (isCreateDeckMode) {
            createDeckThenAddCard(deckName, newCard);
        } else {
            addCardToDeck(currentDeckId, newCard, false);
        }
    }

    private void createDeckThenAddCard(String deckName, Flashcard firstCard) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            btnSave.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> deckData = new HashMap<>();
        deckData.put("name", deckName);
        deckData.put("ownerId", uid);
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

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}