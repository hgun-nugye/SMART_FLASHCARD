package ntu.nguyenthithanhhuong.smartflashcard.fragment;

import static android.graphics.Bitmap.createBitmap;
import static android.graphics.Typeface.create;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ntu.nguyenthithanhhuong.smartflashcard.EdgeToEdgeHelper;
import ntu.nguyenthithanhhuong.smartflashcard.HelpActivity;
import ntu.nguyenthithanhhuong.smartflashcard.R;
import ntu.nguyenthithanhhuong.smartflashcard.login.UserProfileHelper;
import ntu.nguyenthithanhhuong.smartflashcard.model.User;
import ntu.nguyenthithanhhuong.smartflashcard.login.ChoiceLoginActivity;

public class ProfileFragment extends Fragment {
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvUid;
    private TextView tvPhone;
    private TextView tvCreatedAt;
    private MaterialButton btnLogout;
    private FirebaseAuth mAuth;
    private ShapeableImageView imgUser;
    private MaterialCardView cardHelp;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);

        tvUid = view.findViewById(R.id.tvUid);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvCreatedAt = view.findViewById(R.id.tvCreatedAt);

        btnLogout = view.findViewById(R.id.btnLogout);
        imgUser = view.findViewById(R.id.imgUser);
        cardHelp = view.findViewById(R.id.cardHelp);
        cardHelp.setOnClickListener(v -> showHelpDialog());

        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        loadUserProfile();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EdgeToEdgeHelper.applyRootInsets(view);
    }

    private void showLogoutConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext(),
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out of this account?")
                .setCancelable(true)
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(requireContext(), ChoiceLoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        tvProfileEmail.setText(currentUser.getEmail());

        UserProfileHelper.ensureUserProfile(currentUser, new UserProfileHelper.Callback() {
            @Override
            public void onReady(User user) {
                if (!isAdded() || user == null) return;

                if (user.fullName != null && !user.fullName.isEmpty()) {
                    tvProfileName.setText(user.fullName);
                    if (imgUser != null) imgUser.setImageDrawable(generateAvatarBitmap(user.fullName));
                } else {
                    tvProfileName.setText("Guest");
                    if (imgUser != null) imgUser.setImageDrawable(generateAvatarBitmap("Guest"));
                }

                if (user.email != null) tvProfileEmail.setText(user.email);
                if (user.uid != null) tvUid.setText(user.uid);

                if (user.phone != null && !user.phone.isEmpty()) {
                    tvPhone.setText(user.phone);
                } else {
                    tvPhone.setText("Not updated");
                }

                if (user.createdAt > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
                    tvCreatedAt.setText(sdf.format(new Date(user.createdAt)));
                } else {
                    tvCreatedAt.setText("Unknown");
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    tvProfileName.setText("Guest");
                    if (imgUser != null) imgUser.setImageDrawable(generateAvatarBitmap("Guest"));
                }
            }
        });
    }

    private String getSubName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "G";
        String[] words = fullName.trim().split("\\s+");
        if (words.length == 1) {
            return words[0].substring(0, Math.min(2, words[0].length())).toUpperCase();
        }
        return (words[0].substring(0, 1) + words[words.length - 1].substring(0, 1)).toUpperCase();
    }

    private GradientDrawable createTextDrawable(String text) {
        int[] colors = {
                Color.parseColor("#162E7B"), // Deep Indigo
                Color.parseColor("#5E9ED9"), // Soft Blue
                Color.parseColor("#A9DBFF"), // Light Sky Blue
                Color.parseColor("#9B7BFF"), // Muted Purple
                Color.parseColor("#00BCD4")  // Soft Cyan
        };
        int randomColor = colors[Math.abs(text.hashCode()) % colors.length];

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(randomColor);
        int size = (int) (100 * getResources().getDisplayMetrics().density);
        drawable.setSize(size, size);
        return drawable;
    }

    private void showHelpDialog() {
        Intent intent = new Intent(requireContext(), HelpActivity.class);
        startActivity(intent);
    }

    private BitmapDrawable generateAvatarBitmap(String name) {
        String subName = getSubName(name);
        GradientDrawable background = createTextDrawable(subName);

        int size = (int) (100 * getResources().getDisplayMetrics().density);
        Bitmap bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        background.setBounds(0, 0, size, size);
        background.draw(canvas);

        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(32 * getResources().getDisplayMetrics().density);
        paint.setAntiAlias(true);
        paint.setTextAlign(android.graphics.Paint.Align.CENTER);
        paint.setTypeface(create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));

        float yPos = (canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2);
        canvas.drawText(subName, canvas.getWidth() / 2, yPos, paint);

        return new BitmapDrawable(getResources(), bitmap);
    }
}