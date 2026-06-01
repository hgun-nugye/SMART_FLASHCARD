package ntu.nguyenthithanhhuong.smartflashcard.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import android.text.Editable;
import android.text.TextWatcher;

import java.util.concurrent.TimeUnit;

import ntu.nguyenthithanhhuong.smartflashcard.EdgeToEdgeHelper;
import ntu.nguyenthithanhhuong.smartflashcard.MainActivity;
import ntu.nguyenthithanhhuong.smartflashcard.model.User;
import ntu.nguyenthithanhhuong.smartflashcard.R;

public class LoginOtpActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilPassword, tilConfirmPassword, tilPhone, tilOtp;
    private TextInputEditText edFullName, edPassword, edConfirmPassword, edphone, edotp;
    private MaterialButton btngetotp, btnloginotp;
    private ProgressBar progressOtp;
    private FirebaseAuth mAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String smsVerify;
    private Boolean isExistingUser = null;

    private String pendingFullName;
    private String pendingEmail;
    private String pendingPassword;
    private String pendingPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_login_otp);
        EdgeToEdgeHelper.applyRootInsets(findViewById(R.id.loginOtp));

        mAuth = FirebaseAuth.getInstance();
        bindViews();
        setupCallbacks();
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
        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(v -> finish());

        tilFullName = findViewById(R.id.tilFullName);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        tilPhone = findViewById(R.id.tilPhone);
        tilOtp = findViewById(R.id.tilOtp);

        edFullName = findViewById(R.id.edFullName);
        edPassword = findViewById(R.id.edPassword);
        edConfirmPassword = findViewById(R.id.edConfirmPassword);
        edphone = findViewById(R.id.edphone);
        edotp = findViewById(R.id.edotp);

        btngetotp = findViewById(R.id.btngetotp);
        btnloginotp = findViewById(R.id.btnloginotp);
        progressOtp = findViewById(R.id.progressOtp);

        tilFullName.setVisibility(View.GONE);
        tilPassword.setVisibility(View.GONE);
        tilConfirmPassword.setVisibility(View.GONE);

        edphone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                isExistingUser = null;
                tilFullName.setVisibility(View.GONE);
                tilPassword.setVisibility(View.GONE);
                tilConfirmPassword.setVisibility(View.GONE);
            }
        });
    }

    private void setupCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                if (credential.getSmsCode() != null) {
                    edotp.setText(credential.getSmsCode());
                }
                if (isExistingUser != null && isExistingUser) {
                    if (validateOtpField()) {
                        signInWithPhoneAuthCredential(credential);
                    }
                } else {
                    if (validateProfileFields() && validateOtpField()) {
                        cacheProfileFields();
                        signInWithPhoneAuthCredential(credential);
                    }
                }
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                setLoading(false);
                Toast.makeText(LoginOtpActivity.this, R.string.otp_error_send, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                smsVerify = verificationId;
                setLoading(false);
                Toast.makeText(LoginOtpActivity.this, R.string.otp_code_sent, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void setupListeners() {
        btngetotp.setOnClickListener(v -> {
            clearErrors();
            String phoneNumber = textOf(edphone);
            if (!phoneNumber.matches("[0-9]+")) {
                tilPhone.setError(getString(R.string.otp_error_phone));
                edphone.requestFocus();
                return;
            }

            if (isExistingUser == null) {
                setLoading(true);
                String normalizedPhone = UserProfileHelper.normalizePhone(phoneNumber);
                FirebaseFirestore.getInstance().collection("users")
                        .whereEqualTo("phone", normalizedPhone)
                        .get()
                        .addOnCompleteListener(task -> {
                            setLoading(false);
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                isExistingUser = true;
                                getOTP(phoneNumber);
                            } else {
                                isExistingUser = false;
                                tilFullName.setVisibility(View.VISIBLE);
                                tilPassword.setVisibility(View.VISIBLE);
                                tilConfirmPassword.setVisibility(View.VISIBLE);
                                Toast.makeText(this, "Số điện thoại mới. Vui lòng nhập đủ thông tin để đăng ký.", Toast.LENGTH_LONG).show();
                            }
                        });
            } else if (isExistingUser) {
                getOTP(phoneNumber);
            } else {
                if (!validateProfileFields()) {
                    return;
                }
                cacheProfileFields();
                setLoading(true);
                getOTP(phoneNumber);
            }
        });

        btnloginotp.setOnClickListener(v -> {
            clearErrors();
            if (isExistingUser == null) {
                Toast.makeText(this, R.string.otp_error_get_otp_first, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isExistingUser && !validateProfileFields()) {
                return;
            }
            if (!validateOtpField()) {
                return;
            }
            if (!isExistingUser) {
                cacheProfileFields();
            }
            verifyOtp(textOf(edotp));
        });
    }

    private void cacheProfileFields() {
        pendingFullName = textOf(edFullName);
        pendingPassword = textOf(edPassword);
        pendingPhone = textOf(edphone);
    }

    private boolean validateProfileFields() {
        String fullName = textOf(edFullName);
        String password = textOf(edPassword);
        String confirmPassword = textOf(edConfirmPassword);
        String phone = textOf(edphone);

        if (fullName.isEmpty() || password.isEmpty()
                || confirmPassword.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, R.string.otp_error_empty, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (fullName.length() < 2) {
            tilFullName.setError(getString(R.string.signup_error_name_short));
            edFullName.requestFocus();
            return false;
        }

        AuthValidator.Result passwordResult = AuthValidator.validatePassword(password);
        if (!passwordResult.valid) {
            tilPassword.setError(getString(passwordResult.messageResId));
            edPassword.requestFocus();
            return false;
        }

        AuthValidator.Result matchResult = AuthValidator.validatePasswordMatch(password, confirmPassword);
        if (!matchResult.valid) {
            tilConfirmPassword.setError(getString(matchResult.messageResId));
            edConfirmPassword.requestFocus();
            return false;
        }

        if (!phone.matches("[0-9]+")) {
            tilPhone.setError(getString(R.string.otp_error_phone));
            edphone.requestFocus();
            return false;
        }

        return true;
    }

    private boolean validateOtpField() {
        String otp = textOf(edotp);
        if (otp.isEmpty()) {
            tilOtp.setError(getString(R.string.otp_error_otp_required));
            edotp.requestFocus();
            return false;
        }
        return true;
    }

    private void getOTP(String phoneNumber) {
        if (phoneNumber.startsWith("0")) {
            phoneNumber = phoneNumber.substring(1);
        }

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber("+84" + phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyOtp(String code) {
        if (smsVerify == null) {
            Toast.makeText(this, R.string.otp_error_get_otp_first, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(smsVerify, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        tilOtp.setError(getString(R.string.otp_wrong));
                        edotp.requestFocus();
                        return;
                    }

                    FirebaseUser user = task.getResult() != null ? task.getResult().getUser() : null;
                    if (user == null) {
                        setLoading(false);
                        Toast.makeText(this, R.string.otp_session_error, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (isExistingUser != null && isExistingUser) {
                        setLoading(false);
                        Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                        goToMain();
                        return;
                    }

                    UserProfileHelper.saveOtpUserProfile(
                            user,
                            pendingFullName,
                            pendingEmail,
                            pendingPhone,
                            pendingPassword,
                            new UserProfileHelper.Callback() {
                                @Override
                                public void onReady(User profile) {
                                    setLoading(false);
                                    String name = profile != null && profile.fullName != null
                                            ? profile.fullName
                                            : pendingFullName;
                                    Toast.makeText(
                                            LoginOtpActivity.this,
                                            getString(R.string.otp_success, name),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    goToMain();
                                }

                                @Override
                                public void onError(String message) {
                                    setLoading(false);

                                    Toast.makeText(
                                            LoginOtpActivity.this,
                                            getString(R.string.otp_error_profile, message),
                                            Toast.LENGTH_LONG
                                    ).show();
                                    goToMain();
                                }
                            }
                    );
                });
    }

    private void clearErrors() {
        tilFullName.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        tilPhone.setError(null);
        tilOtp.setError(null);
    }

    private void setLoading(boolean loading) {
        btngetotp.setEnabled(!loading);
        btnloginotp.setEnabled(!loading);
        progressOtp.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            btnloginotp.setText(R.string.otp_loading);
        } else {
            btnloginotp.setText(R.string.otp_login);
        }
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
}
