package ntu.nguyenthithanhhuong.smartflashcard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class InterestSelectionActivity extends AppCompatActivity {

    // Nhóm 1: Phong cách & Đời sống văn hóa
    private Chip chipGeneralEnglish, chipWorkplaceCulture, chipEntertainmentMedia, chipGlobalTrade;

    // Nhóm 2: Chứng chỉ & Kỹ năng phòng thi
    private Chip chipTOEIC, chipIELTS, chipTestStrategies, chipExamAnxiety;

    // Nhóm 3: Chuyên ngành & Đời sống
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

            // Nhóm 1: Languages & Cultural Vibes
            if (chipGeneralEnglish.isChecked()) selectedTopics.add("General_English");
            if (chipWorkplaceCulture.isChecked()) selectedTopics.add("Workplace_Culture");
            if (chipEntertainmentMedia.isChecked()) selectedTopics.add("Entertainment_Media");
            if (chipGlobalTrade.isChecked()) selectedTopics.add("Global_Trade");

            // Nhóm 2: Exam Preparation & Test Skills
            if (chipTOEIC.isChecked()) selectedTopics.add("TOEIC");
            if (chipIELTS.isChecked()) selectedTopics.add("IELTS");
            if (chipTestStrategies.isChecked()) selectedTopics.add("Test_Strategies");
            if (chipExamAnxiety.isChecked()) selectedTopics.add("Exam_Management");

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

    private void setupChipListeners() {
        View.OnClickListener chipClickListener = v -> {
            boolean anyChecked = chipGeneralEnglish.isChecked() || chipWorkplaceCulture.isChecked() ||
                    chipEntertainmentMedia.isChecked() || chipGlobalTrade.isChecked() ||
                    chipTOEIC.isChecked() || chipIELTS.isChecked() ||
                    chipTestStrategies.isChecked() || chipExamAnxiety.isChecked() ||
                    chipIT.isChecked() || chipBusiness.isChecked() ||
                    chipTourism.isChecked() || chipMedical.isChecked() ||
                    chipDaily.isChecked();

            if (anyChecked) {
                btnStartLearning.setText("Confirm & Initialize Decks");
            } else {
                btnStartLearning.setText("Skip for now");
            }
        };

        chipGeneralEnglish.setOnClickListener(chipClickListener);
        chipWorkplaceCulture.setOnClickListener(chipClickListener);
        chipEntertainmentMedia.setOnClickListener(chipClickListener);
        chipGlobalTrade.setOnClickListener(chipClickListener);
        chipTOEIC.setOnClickListener(chipClickListener);
        chipIELTS.setOnClickListener(chipClickListener);
        chipTestStrategies.setOnClickListener(chipClickListener);
        chipExamAnxiety.setOnClickListener(chipClickListener);
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
        chipGeneralEnglish = findViewById(R.id.chipGeneralEnglish);
        chipWorkplaceCulture = findViewById(R.id.chipWorkplaceCulture);
        chipEntertainmentMedia = findViewById(R.id.chipEntertainmentMedia);
        chipGlobalTrade = findViewById(R.id.chipGlobalTrade);

        chipTOEIC = findViewById(R.id.chipTOEIC);
        chipIELTS = findViewById(R.id.chipIELTS);
        chipTestStrategies = findViewById(R.id.chipTestStrategies);
        chipExamAnxiety = findViewById(R.id.chipExamAnxiety);

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