package ntu.nguyenthithanhhuong.smartflashcard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import ntu.nguyenthithanhhuong.smartflashcard.Model.Deck;
import ntu.nguyenthithanhhuong.smartflashcard.Model.Flashcard;
import ntu.nguyenthithanhhuong.smartflashcard.Model.User;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView tvTotalDecks, tvDueCount, tvLearnedCount;
    private RecyclerView rvDecks;
    private FloatingActionButton fabAddDeck;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DeckAdapter adapter;

    private final List<Deck> deckList = new ArrayList<>();
    private final List<Deck> filteredList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
//
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, ChoiceLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            loadUserProfile();
            loadDecksFromFirestore();
        }
    }

    private void initViews() {
        rvDecks = findViewById(R.id.rvDecks);
        tvTotalDecks = findViewById(R.id.tvTotalDecks);
        tvDueCount = findViewById(R.id.tvDueCount);
        tvLearnedCount = findViewById(R.id.tvLearnedCount);
        fabAddDeck = findViewById(R.id.fabAddDeck);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Xử lý sự kiện bấm nút 3 gạch mở thanh Menu kéo (Drawer)
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        // Bắt sự kiện click các item trong Menu kéo
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                boolean handled = onToolbarMenuClick(item);
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                return handled;
            });
        }

        rvDecks.setLayoutManager(new LinearLayoutManager(this));
        rvDecks.setNestedScrollingEnabled(false);

        // 🌟 SỬA QUAN TRỌNG: Truyền filteredList vào Adapter thay vì deckList
        adapter = new DeckAdapter(filteredList, deck -> {
            Intent intent = new Intent(MainActivity.this, CardListActivity.class);
            intent.putExtra("DECK_ID", deck.deckId);
            intent.putExtra("DECK_NAME", deck.name);
            startActivity(intent);
        });
        rvDecks.setAdapter(adapter);

        fabAddDeck.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddCardActivity.class))
        );
    }

    private void loadDecksFromFirestore() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("decks")
                .whereEqualTo("ownerId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Log.e(TAG, "Firestore error: " + error.getMessage());
                        return;
                    }

                    if (value == null) return;

                    deckList.clear();
                    filteredList.clear();

                    tvTotalDecks.setText(String.valueOf(value.size()));

                    final int[] totalDue = {0};
                    final int[] totalLearned = {0};

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Deck deck = doc.toObject(Deck.class);
                        if (deck != null) {
                            deck.deckId = doc.getId();
                            deck.newCount = 0;
                            deck.dueCount = 0;
                            deck.learnedCount = 0;

                            deckList.add(deck);
                            filteredList.add(deck);

                            final String deckId = deck.deckId;

                            db.collection("decks")
                                    .document(deckId)
                                    .collection("flashcards")
                                    .get()
                                    .addOnSuccessListener(cardSnapshot -> {
                                        int total = 0;
                                        int newCount = 0;
                                        int dueCount = 0;
                                        int learnedCount = 0;

                                        for (DocumentSnapshot cardDoc : cardSnapshot.getDocuments()) {
                                            Flashcard card = cardDoc.toObject(Flashcard.class);
                                            if (card == null) continue;

                                            total++;
                                            switch (card.getStatus()) {
                                                case NEW:     newCount++;     break;
                                                case REVIEW:  dueCount++;     break;
                                                case LEARNED: learnedCount++; break;
                                            }
                                        }

                                        totalDue[0] += dueCount;
                                        totalLearned[0] += learnedCount;

                                        tvDueCount.setText(String.valueOf(totalDue[0]));
                                        tvLearnedCount.setText(String.valueOf(totalLearned[0]));

                                        // Đồng bộ chỉ số đếm thẻ lên giao diện lọc hiển thị
                                        for (int i = 0; i < filteredList.size(); i++) {
                                            Deck d = filteredList.get(i);
                                            if (deckId.equals(d.deckId)) {
                                                d.cardCount = total;
                                                d.newCount = newCount;
                                                d.dueCount = dueCount;
                                                d.learnedCount = learnedCount;
                                                adapter.notifyItemChanged(i);
                                                break;
                                            }
                                        }

                                        // Đồng bộ mảng chạy ngầm
                                        for (Deck d : deckList) {
                                            if (deckId.equals(d.deckId)) {
                                                d.cardCount = total;
                                                d.newCount = newCount;
                                                d.dueCount = dueCount;
                                                d.learnedCount = learnedCount;
                                                break;
                                            }
                                        }
                                    });
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        UserProfileHelper.ensureUserProfile(currentUser, new UserProfileHelper.Callback() {
            @Override
            public void onReady(User user) {
                if (user != null && user.fullName != null && getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Xin chào, " + user.fullName);
                }
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Lỗi load user profile: " + message);
            }
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
            Intent intent = new Intent(this, ChoiceLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {

                @SuppressLint("RestrictedApi") SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
                if (searchAutoComplete != null) {
                    searchAutoComplete.setTextColor(android.graphics.Color.WHITE); // Chữ gõ hiển thị màu trắng tinh
                    searchAutoComplete.setHintTextColor(android.graphics.Color.parseColor("#80FFFFFF")); // Chữ gợi ý "Tìm kiếm bộ sưu tập..." màu trắng mờ
                }

                int searchIconId = androidx.appcompat.R.id.search_mag_icon;
                ImageView searchIcon = searchView.findViewById(searchIconId);
                if (searchIcon != null) {
                    searchIcon.setColorFilter(android.graphics.Color.WHITE);
                }

                int closeIconId = androidx.appcompat.R.id.search_close_btn;
                ImageView closeIcon = searchView.findViewById(closeIconId);
                if (closeIcon != null) {
                    closeIcon.setColorFilter(android.graphics.Color.WHITE);
                }

                searchView.setQueryHint("Tìm kiếm bộ sưu tập...");
                searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        filterDecks(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        filterDecks(newText);
                        return true;
                    }
                });
            }
        }
        return true;
    }

    private void filterDecks(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(deckList);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (Deck deck : deckList) {
                if (deck.name != null && deck.name.toLowerCase().contains(filterPattern)) {
                    filteredList.add(deck);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        return onToolbarMenuClick(item) || super.onOptionsItemSelected(item);
    }
}