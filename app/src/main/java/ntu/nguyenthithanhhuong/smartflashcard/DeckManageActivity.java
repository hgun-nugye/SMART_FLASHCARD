package ntu.nguyenthithanhhuong.smartflashcard;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import ntu.nguyenthithanhhuong.smartflashcard.Model.Deck;

public class DeckManageActivity extends AppCompatActivity implements DeckManageAdapter.DeckActionListener {
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private RecyclerView rv;
    private DeckManageAdapter adapter;
    private final List<Deck> decks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_deck_manage);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.deckmanage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        FloatingActionButton fab = findViewById(R.id.fabCreateDeck);
        fab.setOnClickListener(v -> openEdit(null));

        rv = findViewById(R.id.rvManageDecks);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeckManageAdapter(decks, this);
        rv.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }
        loadDecks();
    }

    private void loadDecks() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("decks")
                .whereEqualTo("ownerId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("DECK_MANAGE", error.getMessage() == null ? "error" : error.getMessage());
                        Toast.makeText(this, "Lỗi tải bộ: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value == null) return;

                    decks.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Deck d = doc.toObject(Deck.class);
                        if (d != null) {
                            d.deckId = doc.getId();
                            decks.add(d);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void openEdit(String deckId) {
        Intent i = new Intent(this, DeckEditActivity.class);
        if (deckId != null) i.putExtra("DECK_ID", deckId);
        startActivity(i);
    }

    @Override
    public void onEdit(Deck deck) {
        openEdit(deck.deckId);
    }

    @Override
    public void onDelete(Deck deck) {
        if (deck == null || deck.deckId == null) return;
        db.collection("decks").document(deck.deckId)
                .delete()
                .addOnSuccessListener(unused -> Toast.makeText(this, "Đã xoá bộ.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi xoá: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}