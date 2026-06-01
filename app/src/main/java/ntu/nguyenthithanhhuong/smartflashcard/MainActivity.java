package ntu.nguyenthithanhhuong.smartflashcard;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import ntu.nguyenthithanhhuong.smartflashcard.fragment.MainFragment;
import ntu.nguyenthithanhhuong.smartflashcard.fragment.ProfileFragment;
import ntu.nguyenthithanhhuong.smartflashcard.fragment.ReviewFragment;
import ntu.nguyenthithanhhuong.smartflashcard.login.ChoiceLoginActivity;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.enable(this);
        EdgeToEdgeHelper.hideStatusBar(this);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        EdgeToEdgeHelper.setupBottomNavHost(findViewById(R.id.fragmentContainer), bottomNav);

        mAuth = FirebaseAuth.getInstance();

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_main);
            showFragment(new MainFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_main) {
                showFragment(new MainFragment());
                return true;
            }
            if (id == R.id.nav_review) {
                showFragment(new ReviewFragment());
                return true;
            }
            if (id == R.id.nav_profile) {
                showFragment(new ProfileFragment());
                return true;
            }
            return false;
        });
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            EdgeToEdgeHelper.hideStatusBar(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, ChoiceLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}
