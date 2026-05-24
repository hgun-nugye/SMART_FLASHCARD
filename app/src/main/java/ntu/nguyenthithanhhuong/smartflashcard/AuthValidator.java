package ntu.nguyenthithanhhuong.smartflashcard;

import androidx.annotation.StringRes;

import java.util.regex.Pattern;

public final class AuthValidator {

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;
    public static final int MAX_EMAIL_LENGTH = 254;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$"
    );

    private static final Pattern HAS_LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern HAS_UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");

    public static final class Result {
        public final boolean valid;
        @StringRes
        public final int messageResId;

        private Result(boolean valid, @StringRes int messageResId) {
            this.valid = valid;
            this.messageResId = messageResId;
        }

        public static Result ok() {
            return new Result(true, 0);
        }

        public static Result error(@StringRes int messageResId) {
            return new Result(false, messageResId);
        }
    }

    private AuthValidator() {
    }

    //    chuẩn hóa email
    public static String normalizeEmail(String rawEmail) {
        if (rawEmail == null) {
            return "";
        }
        return rawEmail.trim().toLowerCase();
    }

    //Kiểm tra email: không rỗng, không khoảng trắng, đúng định dạng, độ dài hợp lệ.
    public static Result validateEmail(String rawEmail) {
        if (rawEmail == null || rawEmail.trim().isEmpty()) {
            return Result.error(R.string.validation_email_empty);
        }

        if (rawEmail.contains(" ") || rawEmail.contains("\t")) {
            return Result.error(R.string.validation_email_invalid);
        }

        String email = rawEmail.trim();
        if (email.length() > MAX_EMAIL_LENGTH) {
            return Result.error(R.string.validation_email_too_long);
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return Result.error(R.string.validation_email_invalid);
        }

        int at = email.indexOf('@');
        if (at <= 0 || at >= email.length() - 3) {
            return Result.error(R.string.validation_email_invalid);
        }

        String domain = email.substring(at + 1);
        if (!domain.contains(".") || domain.endsWith(".") || domain.startsWith(".")) {
            return Result.error(R.string.validation_email_invalid);
        }

        String tld = domain.substring(domain.lastIndexOf('.') + 1);
        if (tld.length() < 2) {
            return Result.error(R.string.validation_email_invalid);
        }

        return Result.ok();
    }

    //Mật khẩu khi đăng ký / OTP: 8–128 ký tự, có hoa + thường + số, không khoảng trắng.
    public static Result validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return Result.error(R.string.validation_password_empty);
        }

        if (containsWhitespace(password)) {
            return Result.error(R.string.validation_password_no_space);
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            return Result.error(R.string.validation_password_short);
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            return Result.error(R.string.validation_password_long);
        }

        if (!HAS_LOWER.matcher(password).matches()) {
            return Result.error(R.string.validation_password_need_lower);
        }

        if (!HAS_UPPER.matcher(password).matches()) {
            return Result.error(R.string.validation_password_need_upper);
        }

        if (!HAS_DIGIT.matcher(password).matches()) {
            return Result.error(R.string.validation_password_need_digit);
        }

        return Result.ok();
    }

    public static Result validatePasswordForLogin(String password) {
        if (password == null || password.trim().isEmpty()) {
            return Result.error(R.string.validation_password_empty);
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return Result.error(R.string.validation_password_long);
        }
        return Result.ok();
    }

    public static Result validatePasswordMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null || !password.equals(confirmPassword)) {
            return Result.error(R.string.signup_error_password_mismatch);
        }
        return Result.ok();
    }

    private static boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
