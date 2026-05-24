package ntu.nguyenthithanhhuong.smartflashcard;

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

import ntu.nguyenthithanhhuong.smartflashcard.Model.User;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputEditText edmail, edpassword;
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

        edmail = findViewById(R.id.edemailLg);
        edpassword = findViewById(R.id.edpasswordLg);
        btnLogin = findViewById(R.id.btnLogin);
        txtSignup = findViewById(R.id.txtSignup);
        txtForgerPass = findViewById(R.id.txtForgerPass);
        mAuth = FirebaseAuth.getInstance();
        back = findViewById(R.id.back);

        Intent intent = getIntent();
        if (intent != null) {
            Bundle ex = intent.getExtras();
            if (ex != null) {
                edmail.setText(ex.getString("email"));
                edpassword.setText(ex.getString("password"));
            }
        }

        back.setOnClickListener(v -> finish());

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = edmail.getText() != null ? edmail.getText().toString().trim() : "";
                String password = edpassword.getText() != null ? edpassword.getText().toString() : "";

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
                                                            "Chào mừng " + user.fullName + " trở lại!",
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                                goToMain();
                                            }

                                            @Override
                                            public void onError(String message) {
                                                Toast.makeText(LoginActivity.this,
                                                        "Đăng nhập thành công nhưng lỗi hồ sơ: " + message,
                                                        Toast.LENGTH_SHORT).show();
                                                goToMain();
                                            }
                                        });
                                    } else {
                                        Toast.makeText(LoginActivity.this,
                                                "Lỗi phiên đăng nhập, vui lòng thử lại!",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                                    Toast.makeText(LoginActivity.this, "Sai Tài Khoản Hoặc Mật khẩu!",
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

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}