package ntu.nguyenthithanhhuong.smartflashcard.login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import ntu.nguyenthithanhhuong.smartflashcard.EdgeToEdgeHelper;
import ntu.nguyenthithanhhuong.smartflashcard.InterestSelectionActivity;
import ntu.nguyenthithanhhuong.smartflashcard.MainActivity;
import ntu.nguyenthithanhhuong.smartflashcard.model.User;
import ntu.nguyenthithanhhuong.smartflashcard.R;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private TextInputEditText edMail, edPassword;
    private Button btnLogin;
    private TextView txtSignup, txtForgerPass;
    private FirebaseAuth mAuth;
    private ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_login);
        EdgeToEdgeHelper.applyRootInsets(findViewById(R.id.login));

        edMail = findViewById(R.id.edemailLg);
        edPassword = findViewById(R.id.edpasswordLg);
        btnLogin = findViewById(R.id.btnLogin);
        txtSignup = findViewById(R.id.txtSignup);
        txtForgerPass = findViewById(R.id.txtForgerPass);
        mAuth = FirebaseAuth.getInstance();
        back = findViewById(R.id.back);

        Intent intent = getIntent();
        if (intent != null) {
            Bundle ex = intent.getExtras();
            if (ex != null) {
                edMail.setText(ex.getString("email"));
                edPassword.setText(ex.getString("password"));
            }
        }

        back.setOnClickListener(v -> finish());

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = edMail.getText() != null ? edMail.getText().toString().trim() : "";
                String password = edPassword.getText() != null ? edPassword.getText().toString() : "";

                AuthValidator.Result emailResult = AuthValidator.validateEmail(email);
                if (!emailResult.valid) {
                    Toast.makeText(LoginActivity.this, emailResult.messageResId, Toast.LENGTH_SHORT).show();
                    return;
                }
                email = AuthValidator.normalizeEmail(email);

                AuthValidator.Result passwordResult = AuthValidator.validatePasswordForLogin(password);
                if (!passwordResult.valid) {
                    Toast.makeText(LoginActivity.this, passwordResult.messageResId, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Vô hiệu hóa nút để tránh người dùng nhấn liên tục khi đang xử lý
                btnLogin.setEnabled(false);

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "signInWithEmail:success");
                                    FirebaseUser firebaseUser = mAuth.getCurrentUser();

                                    if (firebaseUser != null) {
                                        UserProfileHelper.ensureUserProfile(firebaseUser, new UserProfileHelper.Callback() {
                                            @Override
                                            public void onReady(User user) {
                                                if (user != null && user.fullName != null) {
                                                    Toast.makeText(LoginActivity.this,
                                                            getString(R.string.login_welcome_back, user.fullName),
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                                checkUserInterestsAndNavigate(firebaseUser.getUid());
                                            }

                                            @Override
                                            public void onError(String message) {
                                                btnLogin.setEnabled(true);
                                                Toast.makeText(LoginActivity.this,
                                                        getString(R.string.signup_error_profile,
                                                                UserProfileHelper.resolveErrorMessage(
                                                                        LoginActivity.this, message)),
                                                        Toast.LENGTH_SHORT).show();
                                                checkUserInterestsAndNavigate(firebaseUser.getUid());
                                            }
                                        });
                                    } else {
                                        btnLogin.setEnabled(true);
                                        Exception exception = task.getException();

                                        if (exception instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                                            // Người dùng nhập Email chưa đăng ký tài khoản
                                            Toast.makeText(LoginActivity.this, "Tài khoản không tồn tại. Vui lòng đăng ký!", Toast.LENGTH_LONG).show();
                                        } else if (exception instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                                            // Sai mật khẩu (hoặc sai định dạng xác thực)
                                            Toast.makeText(LoginActivity.this, "Mật khẩu không chính xác. Vui lòng thử lại!", Toast.LENGTH_LONG).show();
                                        } else {
                                            // Các lỗi hệ thống khác (Ví dụ: mất kết nối mạng)
                                            Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                } else {
                                    btnLogin.setEnabled(true);
                                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                                    Toast.makeText(LoginActivity.this, R.string.login_failed,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        txtSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent in = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(in);
            }
        });
        txtForgerPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent in = new Intent(LoginActivity.this, ForgotPassActivity.class);
                startActivity(in);
            }
        });
    }

    private void checkUserInterestsAndNavigate(String userId) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Intent intent;
                    // Kiểm tra nếu tài khoản đã tồn tại trường "interests" và danh sách không rỗng
                    if (documentSnapshot.exists() && documentSnapshot.contains("interests")) {
                        // Đã cấu hình sở thích -> Điều hướng thẳng vào Màn hình chính
                        intent = new Intent(LoginActivity.this, MainActivity.class);
                    } else {
                        // Tài khoản mới hoặc chưa từng chọn sở thích -> Ép cấu hình tại InterestSelectionActivity
                        intent = new Intent(LoginActivity.this, InterestSelectionActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Dự phòng lỗi kết nối mạng: Cho vào thẳng MainActivity để tránh treo trải nghiệm app
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }
}