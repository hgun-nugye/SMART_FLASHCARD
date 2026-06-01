package ntu.nguyenthithanhhuong.smartflashcard.login;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

        Button btnsenemail = findViewById(R.id.btnsenemail);
        EditText edmail = findViewById(R.id.edmail);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(v -> finish());

        btnsenemail.setOnClickListener(v -> {
            String email = edmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(ForgotPassActivity.this, R.string.forgot_email_required, Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPassActivity.this, R.string.forgot_email_sent, Toast.LENGTH_SHORT).show();
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "";
                        Toast.makeText(ForgotPassActivity.this,
                                getString(R.string.forgot_error, msg), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }
}
