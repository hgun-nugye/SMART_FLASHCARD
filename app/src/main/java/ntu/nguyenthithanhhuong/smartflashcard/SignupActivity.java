package ntu.nguyenthithanhhuong.smartflashcard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

import ntu.nguyenthithanhhuong.smartflashcard.Model.User;

public class SignupActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText edFullName, edemail, edpassword, edrppassword;
    private MaterialButton btnsignup;
    private TextView txtLogin;
    private ProgressBar progressSignup;
    private ImageView back;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_signup);
        EdgeToEdgeHelper.applyRootInsets(findViewById(R.id.signup));

        mAuth = FirebaseAuth.getInstance();
        bindViews();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            goToMain();
        }
    }

    private void bindViews() {
        back = findViewById(R.id.back);
        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        edFullName = findViewById(R.id.edFullName);
        edemail = findViewById(R.id.edemail);
        edpassword = findViewById(R.id.edpassword);
        edrppassword = findViewById(R.id.edrppassword);
        btnsignup = findViewById(R.id.btnsignup);
        txtLogin = findViewById(R.id.txtLogin);
        progressSignup = findViewById(R.id.progressSignup);
    }

    private void setupListeners() {
        back.setOnClickListener(v -> finish());

        txtLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnsignup.setOnClickListener(v -> attemptSignup());
    }

    private void attemptSignup() {
        clearErrors();

        String fullName = textOf(edFullName);
        String email = textOf(edemail);
        String password = textOf(edpassword);
        String confirmPassword = textOf(edrppassword);

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, R.string.signup_error_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (fullName.length() < 2) {
            tilFullName.setError(getString(R.string.signup_error_name_short));
            edFullName.requestFocus();
            return;
        }

        AuthValidator.Result emailResult = AuthValidator.validateEmail(email);
        if (!emailResult.valid) {
            tilEmail.setError(getString(emailResult.messageResId));
            edemail.requestFocus();
            return;
        }
        email = AuthValidator.normalizeEmail(email);

        AuthValidator.Result passwordResult = AuthValidator.validatePassword(password);
        if (!passwordResult.valid) {
            tilPassword.setError(getString(passwordResult.messageResId));
            edpassword.requestFocus();
            return;
        }

        AuthValidator.Result matchResult = AuthValidator.validatePasswordMatch(password, confirmPassword);
        if (!matchResult.valid) {
            tilConfirmPassword.setError(getString(matchResult.messageResId));
            edrppassword.requestFocus();
            return;
        }

        setLoading(true);

        String finalEmail = email;
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        showAuthError(task.getException());
                        return;
                    }

                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser == null) {
                        setLoading(false);
                        Toast.makeText(
                                this,
                                getString(R.string.signup_error_auth, getString(R.string.signup_error_no_session)),
                                Toast.LENGTH_SHORT
                        ).show();
                        goToLogin(finalEmail);
                        return;
                    }

                    UserProfileHelper.saveUserProfile(firebaseUser, fullName, new UserProfileHelper.Callback() {
                        @Override
                        public void onReady(User user) {
                            setLoading(false);
                            String name = user != null && user.fullName != null
                                    ? user.fullName
                                    : fullName;
                            Toast.makeText(
                                    SignupActivity.this,
                                    getString(R.string.signup_success, name),
                                    Toast.LENGTH_SHORT
                            ).show();
                            goToMain();
                        }

                        @Override
                        public void onError(String message) {
                            setLoading(false);
                            Toast.makeText(
                                    SignupActivity.this,
                                    getString(R.string.signup_error_profile,
                                            UserProfileHelper.resolveErrorMessage(
                                                    SignupActivity.this, message)),
                                    Toast.LENGTH_SHORT
                            ).show();
                            goToMain();
                        }
                    });
                });
    }

    private void clearErrors() {
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void setLoading(boolean loading) {
        btnsignup.setEnabled(!loading);
        progressSignup.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            btnsignup.setText(R.string.signup_loading);
        } else {
            btnsignup.setText(R.string.signup_button);
        }
    }

    private void showAuthError(Exception exception) {
        if (exception instanceof FirebaseAuthUserCollisionException) {
            tilEmail.setError(getString(R.string.signup_error_email_used));
            edemail.requestFocus();
            return;
        }
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            tilPassword.setError(getString(R.string.signup_error_weak_password));
            edpassword.requestFocus();
            return;
        }

        String message = exception != null && exception.getMessage() != null
                ? exception.getMessage()
                : getString(R.string.signup_error_auth, "unknown");
        Toast.makeText(this, getString(R.string.signup_error_auth, message), Toast.LENGTH_LONG).show();
    }

    private static String textOf(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToLogin(String email) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
        finish();
    }
}
