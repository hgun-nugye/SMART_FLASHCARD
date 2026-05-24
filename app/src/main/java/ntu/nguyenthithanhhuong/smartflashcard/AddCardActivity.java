package ntu.nguyenthithanhhuong.smartflashcard;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ntu.nguyenthithanhhuong.smartflashcard.Model.Flashcard;
import ntu.nguyenthithanhhuong.smartflashcard.Model.WordMeaning;

public class AddCardActivity extends BaseAppActivity {
    private EditText edtDeckName, edtFront, edtBack, edtIpa, edtExample, edtDescription;
    private Button btnAiGen, btnSave;
    private ProgressBar progressBar;
    private AIManager groqManager;
    private FirebaseFirestore db;
    private String currentDeckId;
    private boolean isCreateDeckMode;
    private android.speech.tts.TextToSpeech tts;
    private boolean isTtsReady = false;
    private ImageButton btnPlayTts;
    private List<WordMeaning> aiMeanings = new ArrayList<>();
    private TextView txtMoreMeanings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdgeHelper.enable(this);

        setContentView(R.layout.activity_add_card);

        EdgeToEdgeHelper.applyRootInsets(
                findViewById(R.id.rootView)
        );

        currentDeckId =
                getIntent().getStringExtra("DECK_ID");

        isCreateDeckMode =
                (currentDeckId == null
                        || currentDeckId.trim().isEmpty());

        db = FirebaseFirestore.getInstance();

        groqManager = new AIManager();

        initViews();

        setupModeUi();

        // TTS
        tts = new android.speech.tts.TextToSpeech(
                this,
                status -> {

                    if (status ==
                            android.speech.tts.TextToSpeech.SUCCESS) {

                        int result =
                                tts.setLanguage(
                                        java.util.Locale.US
                                );

                        if (result ==
                                android.speech.tts.TextToSpeech.LANG_MISSING_DATA
                                ||
                                result ==
                                        android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {

                            isTtsReady = false;

                        } else {

                            isTtsReady = true;
                        }
                    }
                });

        // PLAY TTS
        btnPlayTts.setOnClickListener(v -> {

            String word =
                    edtFront.getText().toString().trim();

            if (!word.isEmpty()
                    && isTtsReady
                    && tts != null) {

                tts.speak(
                        word,
                        android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                );

            } else if (word.isEmpty()) {

                Toast.makeText(
                        this,
                        "Hãy nhập từ vựng trước khi nghe!",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // AI GENERATE
        btnAiGen.setOnClickListener(v -> {

            String word =
                    edtFront.getText().toString().trim();

            if (word.isEmpty()) {

                Toast.makeText(
                        this,
                        "Vui lòng nhập từ vựng!",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            progressBar.setVisibility(View.VISIBLE);

            btnAiGen.setEnabled(false);

            groqManager.generateCardContent(
                    word,
                    new AIManager.AiCallback() {

                        @Override
                        public void onSuccess(
                                List<WordMeaning> meanings
                        ) {

                            progressBar.setVisibility(View.GONE);

                            btnAiGen.setEnabled(true);

                            aiMeanings.clear();

                            aiMeanings.addAll(meanings);

                            if (!aiMeanings.isEmpty()) {

                                WordMeaning first =
                                        aiMeanings.get(0);

                                edtBack.setText(first.vi);

                                edtIpa.setText(first.ipa);

                                edtExample.setText(first.example);
                            }

                            txtMoreMeanings.setVisibility(
                                    aiMeanings.size() > 1
                                            ? View.VISIBLE
                                            : View.GONE
                            );

                            // Auto speak
                            if (isTtsReady && tts != null) {

                                tts.speak(
                                        word,
                                        android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        null
                                );
                            }
                        }
                        @Override
                        public void onError(String error) {

                            progressBar.setVisibility(View.GONE);

                            btnAiGen.setEnabled(true);

                            Toast.makeText(
                                    AddCardActivity.this,
                                    error,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );
        });

        // SAVE
        btnSave.setOnClickListener(
                v -> saveCardToFirestore()
        );

        // SHOW MORE MEANINGS
        txtMoreMeanings.setOnClickListener(
                v -> showMeaningsDialog()
        );
    }

    private void initViews() {
        edtDeckName = findViewById(R.id.edtDeckName);
        edtDescription = findViewById(R.id.edtDescription);
        edtFront = findViewById(R.id.edtFront);
        btnPlayTts = findViewById(R.id.btnPlayTts);
        edtBack = findViewById(R.id.edtBack);
        edtIpa = findViewById(R.id.edtIpa);
        edtExample = findViewById(R.id.edtExample);
        btnAiGen = findViewById(R.id.btnAiGen);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        txtMoreMeanings = findViewById(R.id.txtMoreMeanings);
    }

    private void setupModeUi() {
        if (isCreateDeckMode) {
            // CHẾ ĐỘ 1: Tạo bộ sưu tập mới hoàn toàn
            edtDeckName.setVisibility(View.VISIBLE);
            edtDeckName.setEnabled(true);
            edtDeckName.setText(""); // Để trống cho người dùng nhập
            btnSave.setText("Tạo bộ & lưu thẻ");
        } else {
            // CHẾ ĐỘ 2: Thêm thẻ vào bộ sưu tập có sẵn
            edtDeckName.setVisibility(View.VISIBLE); // tên bộ
            edtDeckName.setEnabled(false);           // không cho sửa đổi tên bộ
            edtDeckName.setTextColor(android.graphics.Color.GRAY); // mờ chữ

            edtDescription.setVisibility(View.VISIBLE);
            edtDescription.setEnabled(false);
            edtDescription.setTextColor(android.graphics.Color.GRAY);

            // Nhận tên bộ truyền sang từ Intent
            String deckName = getIntent().getStringExtra("DECK_NAME");
            String deckDescription = getIntent().getStringExtra("DECK_DESCRIPTION");
            if (deckName != null && !deckName.trim().isEmpty()) {

                edtDeckName.setText(deckName);

                if (deckDescription != null &&
                        !deckDescription.trim().isEmpty()) {

                    edtDescription.setText(deckDescription);

                } else {

                    edtDescription.setText("Chưa có mô tả cho bộ từ này");
                }

            } else {

                edtDeckName.setText("Bộ sưu tập hiện tại");
                edtDescription.setText("Chưa có mô tả cho bộ từ này");
            }

            btnSave.setText("Lưu thẻ vào Firebase");
        }
    }

    private void saveCardToFirestore() {
        String deckName = edtDeckName.getText().toString().trim();
        String deckDescription = edtDescription.getText().toString().trim();
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
            createDeckThenAddCard(deckName, deckDescription, newCard);
        } else {
            addCardToDeck(currentDeckId, newCard, false);
        }
    }

    private void createDeckThenAddCard(String deckName, String deckDescription, Flashcard
            firstCard) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            btnSave.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> deckData = new HashMap<>();
        deckData.put("name", deckName);
        deckData.put("description", deckDescription);
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

    private void showMeaningsDialog() {

        if (aiMeanings.isEmpty()) return;

        String[] items = new String[aiMeanings.size()];

        for (int i = 0; i < aiMeanings.size(); i++) {

            WordMeaning m = aiMeanings.get(i);

            items[i] =
                    m.vi + " • " + m.ipa;
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn nghĩa")
                .setItems(items, (dialog, which) -> {

                    WordMeaning selected =
                            aiMeanings.get(which);

                    edtBack.setText(selected.vi);
                    edtIpa.setText(selected.ipa);
                    edtExample.setText(selected.example);
                })
                .show();
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