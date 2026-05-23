package ntu.nguyenthithanhhuong.smartflashcard;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import ntu.nguyenthithanhhuong.smartflashcard.Model.User;

//  Quản lý document users/{uid} trên Firestore và đồng bộ display name Auth.
public final class UserProfileHelper {

    public interface Callback {
        void onReady(User user);

        void onError(String message);
    }

    private UserProfileHelper() {
    }

    //    Lưu hồ sơ khi đăng ký - dùng họ tên người dùng nhập, ghi đè document nếu cần.
    public static void saveUserProfile(FirebaseUser firebaseUser, String fullName, Callback callback) {
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError("Không có thông tin đăng nhập");
            }
            return;
        }

        String trimmedName = fullName != null ? fullName.trim() : "";
        if (trimmedName.isEmpty()) {
            trimmedName = defaultDisplayName(firebaseUser);
        }

        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
        User user = new User(uid, email, trimmedName);

        UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                .setDisplayName(trimmedName)
                .build();

        Tasks.whenAllComplete(
                        firebaseUser.updateProfile(profileUpdate),
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .set(user)
                )
                .addOnSuccessListener(tasks -> {
                    if (callback != null) {
                        callback.onReady(user);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Lỗi lưu hồ sơ");
                    }
                });
    }

    //Lưu hồ sơ đầy đủ sau đăng nhập OTP: họ tên, email, SĐT; liên kết email/mật khẩu với tài khoản phone./
    public static void saveOtpUserProfile(
            FirebaseUser firebaseUser,
            String fullName,
            String email,
            String phone,
            String password,
            Callback callback
    ) {
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError("Không có thông tin đăng nhập");
            }
            return;
        }

        String trimmedName = fullName != null ? fullName.trim() : "";
        String trimmedEmail = email != null ? email.trim() : "";
        String normalizedPhone = normalizePhone(phone);

        if (trimmedName.isEmpty()) {
            trimmedName = defaultDisplayName(firebaseUser);
        }

        String uid = firebaseUser.getUid();
        User user = new User(uid, trimmedEmail, trimmedName, normalizedPhone);

        UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                .setDisplayName(trimmedName)
                .build();

        Task<?> linkTask = buildEmailLinkTask(firebaseUser, trimmedEmail, password);

        Tasks.whenAllComplete(
                        firebaseUser.updateProfile(profileUpdate),
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .set(user),
                        linkTask
                )
                .addOnSuccessListener(tasks -> {
                    for (Task<?> t : tasks) {
                        if (!t.isSuccessful() && t.getException() != null) {
                            Exception ex = t.getException();
                            if (ex instanceof FirebaseAuthUserCollisionException) {
                                if (callback != null) {
                                    callback.onError("EMAIL_COLLISION");
                                }
                                return;
                            }
                            if (callback != null) {
                                callback.onError(ex.getMessage() != null ? ex.getMessage() : "Lỗi lưu hồ sơ");
                            }
                            return;
                        }
                    }
                    if (callback != null) {
                        callback.onReady(user);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Lỗi lưu hồ sơ");
                    }
                });
    }

    private static Task<?> buildEmailLinkTask(FirebaseUser firebaseUser, String email, String password) {
        if (email.isEmpty()
                || password == null
                || !AuthValidator.validatePassword(password).valid) {
            return Tasks.forResult(null);
        }

        for (UserInfo provider : firebaseUser.getProviderData()) {
            if (EmailAuthProvider.PROVIDER_ID.equals(provider.getProviderId())) {
                return Tasks.forResult(null);
            }
        }

        AuthCredential emailCredential = EmailAuthProvider.getCredential(email, password);
        return firebaseUser.linkWithCredential(emailCredential);
    }

    static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (digits.isEmpty()) {
            return "";
        }
        return "+84" + digits;
    }


    public static void ensureUserProfile(FirebaseUser firebaseUser, Callback callback) {
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError("Không có thông tin đăng nhập");
            }
            return;
        }

        String uid = firebaseUser.getUid();
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User existing = doc.toObject(User.class);
                        if (callback != null) {
                            callback.onReady(existing);
                        }
                        return;
                    }

                    User newUser = new User(
                            uid,
                            firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "",
                            defaultDisplayName(firebaseUser)
                    );

                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .set(newUser)
                            .addOnSuccessListener(unused -> {
                                if (callback != null) {
                                    callback.onReady(newUser);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) {
                                    callback.onError(e.getMessage() != null ? e.getMessage() : "Lỗi tạo hồ sơ");
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Lỗi tải hồ sơ");
                    }
                });
    }

    static String defaultDisplayName(FirebaseUser user) {
        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName().trim();
        }
        String email = user.getEmail();
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        String phone = user.getPhoneNumber();
        if (phone != null && !phone.isEmpty()) {
            return phone;
        }
        return "Người dùng";
    }
}
