package ntu.nguyenthithanhhuong.smartflashcard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import ntu.nguyenthithanhhuong.smartflashcard.EdgeToEdgeHelper;
import ntu.nguyenthithanhhuong.smartflashcard.MainActivity;
import ntu.nguyenthithanhhuong.smartflashcard.R;

public class InterestSelectionActivity extends AppCompatActivity {

    private Chip chipEnglish, chipJapanese, chipKorean, chipChinese;
    private Chip chipTOEIC, chipIELTS, chipJLPT, chipTOPIK;
    private Chip chipIT, chipBusiness, chipTourism, chipMedical, chipDaily;

    private MaterialButton btnStartLearning;
    private LinearLayout layoutLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_interest_selection);
        EdgeToEdgeHelper.applyRootInsets(findViewById(R.id.interestSelection));

        initViews();
        setupChipListeners();

        btnStartLearning.setOnClickListener(v -> {
            List<String> selectedTopics = new ArrayList<>();

            // Nhóm 1: Languages
            if (chipEnglish.isChecked()) selectedTopics.add("English");
            if (chipJapanese.isChecked()) selectedTopics.add("Japanese");
            if (chipKorean.isChecked()) selectedTopics.add("Korean");
            if (chipChinese.isChecked()) selectedTopics.add("Chinese");

            // Nhóm 2: Exam Preparation
            if (chipTOEIC.isChecked()) selectedTopics.add("TOEIC");
            if (chipIELTS.isChecked()) selectedTopics.add("IELTS");
            if (chipJLPT.isChecked()) selectedTopics.add("JLPT");
            if (chipTOPIK.isChecked()) selectedTopics.add("TOPIK");

            // Nhóm 3: Majors & Daily Life
            if (chipIT.isChecked()) selectedTopics.add("IT");
            if (chipBusiness.isChecked()) selectedTopics.add("Business");
            if (chipTourism.isChecked()) selectedTopics.add("Tourism");
            if (chipMedical.isChecked()) selectedTopics.add("Medical");
            if (chipDaily.isChecked()) selectedTopics.add("Daily");

            if (selectedTopics.isEmpty()) {
                navigateToHome();
                return;
            }

            layoutLoading.setVisibility(View.VISIBLE);
            btnStartLearning.setEnabled(false);

            new OnboardingManager().importSampleDecks(selectedTopics, new OnboardingManager.ImportCallback() {
                @Override
                public void onSuccess() {
                    layoutLoading.setVisibility(View.GONE);
                    btnStartLearning.setEnabled(true);
                    Toast.makeText(InterestSelectionActivity.this, "Sample decks sync successful!", Toast.LENGTH_SHORT).show();
                    navigateToHome();
                }

                @Override
                public void onFailure(Exception e) {
                    layoutLoading.setVisibility(View.GONE);
                    btnStartLearning.setEnabled(true);
                    Toast.makeText(InterestSelectionActivity.this, "Sync error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // Hàm lắng nghe thay đổi trạng thái các chip để cập nhật Text nút bấm động (UI mượt hơn)
    private void setupChipListeners() {
        View.OnClickListener chipClickListener = v -> {
            boolean anyChecked = chipEnglish.isChecked() || chipJapanese.isChecked() ||
                    chipKorean.isChecked() || chipChinese.isChecked() ||
                    chipTOEIC.isChecked() || chipIELTS.isChecked() ||
                    chipJLPT.isChecked() || chipTOPIK.isChecked() ||
                    chipIT.isChecked() || chipBusiness.isChecked() ||
                    chipTourism.isChecked() || chipMedical.isChecked() ||
                    chipDaily.isChecked();

            if (anyChecked) {
                btnStartLearning.setText("Confirm & Initialize Decks");
            } else {
                btnStartLearning.setText("Skip for now");
            }
        };

        // Gán sự kiện lắng nghe cho toàn bộ các chip
        chipEnglish.setOnClickListener(chipClickListener);
        chipJapanese.setOnClickListener(chipClickListener);
        chipKorean.setOnClickListener(chipClickListener);
        chipChinese.setOnClickListener(chipClickListener);
        chipTOEIC.setOnClickListener(chipClickListener);
        chipIELTS.setOnClickListener(chipClickListener);
        chipJLPT.setOnClickListener(chipClickListener);
        chipTOPIK.setOnClickListener(chipClickListener);
        chipIT.setOnClickListener(chipClickListener);
        chipBusiness.setOnClickListener(chipClickListener);
        chipTourism.setOnClickListener(chipClickListener);
        chipMedical.setOnClickListener(chipClickListener);
        chipDaily.setOnClickListener(chipClickListener);
    }

    private void navigateToHome() {
        Intent intent = new Intent(InterestSelectionActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void initViews() {
        chipEnglish = findViewById(R.id.chipEnglish);
        chipJapanese = findViewById(R.id.chipJapanese);
        chipKorean = findViewById(R.id.chipKorean);
        chipChinese = findViewById(R.id.chipChinese);
        chipTOEIC = findViewById(R.id.chipTOEIC);
        chipIELTS = findViewById(R.id.chipIELTS);
        chipJLPT = findViewById(R.id.chipJLPT);
        chipTOPIK = findViewById(R.id.chipTOPIK);
        chipIT = findViewById(R.id.chipIT);
        chipBusiness = findViewById(R.id.chipBusiness);
        chipTourism = findViewById(R.id.chipTourism);
        chipMedical = findViewById(R.id.chipMedical);
        chipDaily = findViewById(R.id.chipDaily);
        btnStartLearning = findViewById(R.id.btnStartLearning);
        layoutLoading = findViewById(R.id.layoutLoading);

        btnStartLearning.setText("Skip for now");
    }
}