package ntu.nguyenthithanhhuong.smartflashcard;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;


public class SignupActivity extends AppCompatActivity {
    private TextInputEditText edemail, edpassword, edrppassword;
    private Button btnsignup;
    private TextView txtLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signup), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        edemail = findViewById(R.id.edemail);
        edpassword = findViewById(R.id.edpassword);
        edrppassword = findViewById(R.id.edrppassword);
        btnsignup = findViewById(R.id.btnsignup);
        txtLogin = findViewById(R.id.txtLogin);
        mAuth = FirebaseAuth.getInstance();

        btnsignup.setOnClickListener(view -> {

            String email = edemail.getText().toString().trim();
            String password = edpassword.getText().toString().trim();
            String rppassword = edrppassword.getText().toString().trim();

            // Check rỗng
            if(email.isEmpty() || password.isEmpty() || rppassword.isEmpty()){
                Toast.makeText(this, "Vui lòng nhập đầy đủ!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check email
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edemail.setError("Email không hợp lệ");
                edemail.requestFocus();
                return;
            }

            // Check password
            if (password.length() < 6) {
                edpassword.setError("Mật khẩu tối thiểu 6 ký tự");
                edpassword.requestFocus();
                return;
            }

            // Check trùng password
            if (!password.equals(rppassword)) {
                edrppassword.setError("Mật khẩu không khớp");
                edrppassword.requestFocus();
                return;
            }

            // Disable button tránh spam
            btnsignup.setEnabled(false);

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        btnsignup.setEnabled(true);

                        if (task.isSuccessful()) {

                            Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                            // Chuyển màn hình
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                            finish();

                        } else {

                            String error = task.getException().getMessage();
                            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        txtLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent in = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(in);
            }
        });
    }
    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}