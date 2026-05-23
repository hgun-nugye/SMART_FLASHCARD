package ntu.nguyenthithanhhuong.smartflashcard;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import java.util.Locale;
import ntu.nguyenthithanhhuong.smartflashcard.Model.Flashcard;

public class CardDetailActivity extends BaseAppActivity {

    private TextView tvFront, tvIpa, tvBack, tvExample, tvStatus;
    private MaterialButton btnPlay;
    private TextToSpeech tts;
    private boolean isTtsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_card_detail);
        EdgeToEdgeHelper.applyScreenWithToolbar(
                findViewById(R.id.cardDetail),
                findViewById(R.id.toolbarDetail)
        );

        // Khởi tạo TextToSpeech cho màn hình chi tiết
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                isTtsReady = true;
            }
        });

        initViews();
        displayCardData();
    }

    private void initViews() {
        tvFront = findViewById(R.id.tvDetailFront);
        tvIpa = findViewById(R.id.tvDetailIpa);
        tvBack = findViewById(R.id.tvDetailBack);
        tvExample = findViewById(R.id.tvDetailExample);
        tvStatus = findViewById(R.id.tvDetailStatus);
        btnPlay = findViewById(R.id.btnDetailPlay);
        MaterialToolbar toolbar = findViewById(R.id.toolbarDetail);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void displayCardData() {
        // Nhận đối tượng Flashcard truyền từ màn hình danh sách sang
        Flashcard card = (Flashcard) getIntent().getSerializableExtra("CARD_DATA");

        if (card != null) {
            tvFront.setText(card.front);
            tvIpa.setText(card.ipa != null ? card.ipa : "");
            tvBack.setText(card.back);
            tvStatus.setText(card.getStatus() != null ? card.getStatus().toString() : "Mới");

            if (card.example != null && !card.example.isEmpty()) {
                tvExample.setText(card.example);
            } else {
                tvExample.setText("Không có ví dụ cho từ này.");
            }

            btnPlay.setOnClickListener(v -> {
                if (isTtsReady && tts != null) {
                    tts.speak(card.front, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            });
        }
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