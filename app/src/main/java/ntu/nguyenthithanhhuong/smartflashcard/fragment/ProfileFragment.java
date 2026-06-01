package ntu.nguyenthithanhhuong.smartflashcard.fragment;

import static android.graphics.Bitmap.createBitmap;
import static android.graphics.Typeface.create;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ntu.nguyenthithanhhuong.smartflashcard.EdgeToEdgeHelper;
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
        imgUser= view.findViewById(R.id.imgUser);

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(requireContext(), ChoiceLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        loadUserProfile();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EdgeToEdgeHelper.applyRootInsets(view);
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        tvProfileEmail.setText(currentUser.getEmail());

        UserProfileHelper.ensureUserProfile(currentUser, new UserProfileHelper.Callback() {
            @Override
            public void onReady(User user) {
                if (!isAdded() || user == null) {
                    return;
                }

                // 1. Xử lý Tên và Tự động tạo ảnh đại diện theo tên
                if (user.fullName != null && !user.fullName.isEmpty()) {
                    tvProfileName.setText(user.fullName);

                    if (imgUser != null) {
                        imgUser.setImageDrawable(generateAvatarBitmap(user.fullName));
                    }
                } else {
                    tvProfileName.setText("Guest");
                    if (imgUser != null) {
                        imgUser.setImageDrawable(generateAvatarBitmap("Guest"));
                    }
                }

                // 2. Email
                if (user.email != null) {
                    tvProfileEmail.setText(user.email);
                }

                // 3. UID
                if (user.uid != null) {
                    tvUid.setText(user.uid);
                }

                // 4. Phone
                if (user.phone != null && !user.phone.isEmpty()) {
                    tvPhone.setText(user.phone);
                } else {
                    tvPhone.setText("Not updated");
                }

                // 5. Created At
                if (user.createdAt > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    String date = sdf.format(new Date(user.createdAt));
                    tvCreatedAt.setText(date);
                } else {
                    tvCreatedAt.setText("Unknown");
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    tvProfileName.setText("Guest");
                    if (imgUser != null) {
                        imgUser.setImageDrawable(generateAvatarBitmap("Guest"));
                    }
                }
            }
        });
    }

    // Lấy 2 chữ cái đầu của Tên
    private String getSubName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "G";
        String[] words = fullName.trim().split("\\s+");
        if (words.length == 1) {
            return words[0].substring(0, Math.min(2, words[0].length())).toUpperCase();
        }
        return (words[0].substring(0, 1) + words[words.length - 1].substring(0, 1)).toUpperCase();
    }

    // Vẽ nền hình tròn màu sắc pastel ngẫu nhiên theo mã băm của tên
    private GradientDrawable createTextDrawable(String text) {
        int[] colors = {
                android.graphics.Color.parseColor("#9B7BFF"), // Tím
                android.graphics.Color.parseColor("#FF6B81"), // Hồng
                android.graphics.Color.parseColor("#4ED164"), // Xanh lá
                android.graphics.Color.parseColor("#FF9F43"), // Cam
                android.graphics.Color.parseColor("#54a0ff")  // Xanh dương
        };
        int randomColor = colors[Math.abs(text.hashCode()) % colors.length];

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(randomColor);
        int size = (int) (100 * getResources().getDisplayMetrics().density);
        drawable.setSize(size, size);
        return drawable;
    }

    // Kết hợp chữ viết trắng đè lên nền hình tròn
    private BitmapDrawable generateAvatarBitmap(String name) {
        String subName = getSubName(name);
       GradientDrawable background = createTextDrawable(subName);

        int size = (int) (100 * getResources().getDisplayMetrics().density);
        android.graphics.Bitmap bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        background.setBounds(0, 0, size, size);
        background.draw(canvas);

        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(android.graphics.Color.WHITE);
        paint.setTextSize(32 * getResources().getDisplayMetrics().density);
        paint.setAntiAlias(true);
        paint.setTextAlign(android.graphics.Paint.Align.CENTER);
        paint.setTypeface(create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));

        float yPos = (canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2);
        canvas.drawText(subName, canvas.getWidth() / 2, yPos, paint);

        return new BitmapDrawable(getResources(), bitmap);
    }
}