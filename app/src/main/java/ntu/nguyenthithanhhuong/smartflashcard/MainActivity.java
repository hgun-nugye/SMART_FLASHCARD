package ntu.nguyenthithanhhuong.smartflashcard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import ntu.nguyenthithanhhuong.smartflashcard.Model.Deck;
import ntu.nguyenthithanhhuong.smartflashcard.Model.User;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rvDecks;
    private FloatingActionButton fabAddDeck;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DeckAdapter adapter;
    private final List<Deck> deckList = new ArrayList<>();
    private User currentUserProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 2. Thiết lập giao diện
        initViews();

        // 3. Tải dữ liệu
//        loadDecksFromFirestore();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, ChoiceLoginActivity.class));
            finish();
        } else {
            loadUserProfile();
            loadDecksFromFirestore();
        }
    }

    private void initViews() {
        rvDecks = findViewById(R.id.rvDecks);
        fabAddDeck = findViewById(R.id.fabAddDeck);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        toolbar.setOnMenuItemClickListener(this::onToolbarMenuClick);

        rvDecks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeckAdapter(deckList, deck -> {
            Intent intent = new Intent(MainActivity.this, CardListActivity.class);
            intent.putExtra("DECK_ID", deck.deckId);
            startActivity(intent);
        });
        rvDecks.setAdapter(adapter);

        fabAddDeck.setOnClickListener(v -> {
            // Create new deck flow happens inside AddCardActivity (no dialog).
            Intent intent = new Intent(MainActivity.this, AddCardActivity.class);
            startActivity(intent);
        });
    }

    private boolean onToolbarMenuClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_manage_decks) {
            startActivity(new Intent(this, DeckManageActivity.class));
            return true;
        }
        if (id == R.id.action_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return false;
    }

    private void loadDecksFromFirestore() {
        if (mAuth.getCurrentUser() == null) return;

        String currentUserId = mAuth.getCurrentUser().getUid();

        // Sử dụng orderBy để đưa bộ thẻ mới nhất lên đầu
        db.collection("decks")
                .whereEqualTo("ownerId", currentUserId)
//                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FIRESTORE_ERROR", error.getMessage());
                        return;
                    }
                    if (value == null) return;

                    deckList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Deck deck = doc.toObject(Deck.class);
                        if (deck != null) {
                            deck.deckId = doc.getId();
                            deckList.add(deck);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // Cập nhật giao diện thanh tiêu đề hoặc thông tin cá nhân
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setSubtitle("Xin chào, " + user.fullName);
                            }
                        }
                    });
        }
    }

    // 1. Hàm này dùng để khởi tạo và "bơm" (inflate) menu vào Toolbar
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // 2. Hàm xử lý sự kiện khi người dùng click vào từng mục trong menu
    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        return onToolbarMenuClick(item) || super.onOptionsItemSelected(item);
    }
}