package ntu.nguyenthithanhhuong.smartflashcard;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ChoiceLoginActivity extends AppCompatActivity {
    private Button btnemail,btnOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choice_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.choiceLogin), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnemail = findViewById(R.id.btnemail);
        btnOtp = findViewById(R.id.btnOtp);

        btnemail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent in = new Intent(ChoiceLoginActivity.this, LoginActivity.class);
                startActivity(in);
            }
        });
        btnOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent in = new Intent(ChoiceLoginActivity.this, LoginOtpActivity.class);
                startActivity(in);
            }
        });
    }
}