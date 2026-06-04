package ntu.nguyenthithanhhuong.smartflashcard.login;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import ntu.nguyenthithanhhuong.smartflashcard.EdgeToEdgeHelper;
import ntu.nguyenthithanhhuong.smartflashcard.R;

public class ForgotPassActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_forgot_pass);

        EdgeToEdgeHelper.applyRootInsets(findViewById(R.id.forgotPass));

        MaterialButton btnSendEmail = findViewById(R.id.btnsenemail);
        EditText edMail = findViewById(R.id.edmail);
        ImageView back = findViewById(R.id.back);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        back.setOnClickListener(v -> finish());

        btnSendEmail.setOnClickListener(v -> {
            String email = edMail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(ForgotPassActivity.this, R.string.forgot_email_required, Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ForgotPassActivity.this, R.string.forgot_email_sent, Toast.LENGTH_SHORT).show();
                    finish(); // Trở về màn hình cũ sau khi gửi thành công
                } else {
                    String msg = task.getException() != null ? task.getException().getMessage() : "";
                    Toast.makeText(ForgotPassActivity.this,
                            getString(R.string.forgot_error, msg), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}