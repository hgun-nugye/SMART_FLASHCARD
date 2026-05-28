package ntu.nguyenthithanhhuong.smartflashcard;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DeckEditActivity extends BaseAppActivity {
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextInputEditText edtName, edtDescription;
    private MaterialButton btnSave, btnDelete;
    private ProgressBar progress;

    private String deckId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_deck_edit);
        EdgeToEdgeHelper.applyCoordinatorInsets(
                findViewById(R.id.deckEdit),
                findViewById(R.id.appBarLayout)
        );

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        deckId = getIntent().getStringExtra("DECK_ID");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(deckId == null
                ? getString(R.string.deck_create_title)
                : getString(R.string.deck_edit_existing));

        edtName = findViewById(R.id.edtName);
        edtDescription = findViewById(R.id.edtDescription);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        progress = findViewById(R.id.progress);

        btnDelete.setVisibility(deckId == null ? View.GONE : View.VISIBLE);

        btnSave.setOnClickListener(v -> save());
        btnDelete.setOnClickListener(v -> delete());

        if (deckId != null) loadDeck();
    }

    private void loadDeck() {
        setLoading(true);
        db.collection("decks").document(deckId)
                .get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);
                    String name = doc.getString("name");
                    String desc = doc.getString("description");
                    if (name != null) edtName.setText(name);
                    if (desc != null) edtDescription.setText(desc);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            getString(R.string.deck_load_error, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void save() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, R.string.not_signed_in, Toast.LENGTH_SHORT).show();
            return;
        }

        String name = edtName.getText() != null ? edtName.getText().toString().trim() : "";
        String desc = edtDescription.getText() != null ? edtDescription.getText().toString().trim() : "";
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.deck_name_required, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        if (deckId == null) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("description", desc);
            data.put("ownerId", auth.getCurrentUser().getUid());
            data.put("cardCount", 0);
            data.put("createdAt", System.currentTimeMillis());
            db.collection("decks")
                    .add(data)
                    .addOnSuccessListener(ref -> {
                        setLoading(false);
                        Toast.makeText(this, R.string.deck_created, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this,
                                getString(R.string.deck_save_error, e.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("description", desc);
            db.collection("decks").document(deckId)
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        setLoading(false);
                        Toast.makeText(this, R.string.deck_saved, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this,
                                getString(R.string.deck_save_error, e.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void delete() {
        if (deckId == null) return;
        setLoading(true);
        db.collection("decks").document(deckId)
                .delete()
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.deck_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            getString(R.string.deck_delete_error, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        btnDelete.setEnabled(!loading);
    }
}
