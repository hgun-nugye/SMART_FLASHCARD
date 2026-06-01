package ntu.nguyenthithanhhuong.smartflashcard.login;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import ntu.nguyenthithanhhuong.smartflashcard.R;
import ntu.nguyenthithanhhuong.smartflashcard.model.User;

public final class UserProfileHelper {

    public interface Callback {
        void onReady(User user);

        void onError(String message);
    }

    private UserProfileHelper() {
    }

    public static String resolveErrorMessage(Context context, String message) {
        if (message == null) {
            return context.getString(R.string.error_save_profile);
        }
        switch (message) {
            case "NOT_SIGNED_IN":
                return context.getString(R.string.error_no_auth);
            case "PROFILE_SAVE_ERROR":
                return context.getString(R.string.error_save_profile);
            case "PROFILE_CREATE_ERROR":
                return context.getString(R.string.error_create_profile);
            case "PROFILE_LOAD_ERROR":
                return context.getString(R.string.error_load_profile);
            default:
                return message;
        }
    }

    public static void saveUserProfile(FirebaseUser firebaseUser, String fullName, Callback callback) {
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError("NOT_SIGNED_IN");
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
                        callback.onError(e.getMessage() != null ? e.getMessage() : "PROFILE_SAVE_ERROR");
                    }
                });
    }

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
                callback.onError("NOT_SIGNED_IN");
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
                                callback.onError(ex.getMessage() != null ? ex.getMessage() : "PROFILE_SAVE_ERROR");
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
                        callback.onError(e.getMessage() != null ? e.getMessage() : "PROFILE_SAVE_ERROR");
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
                callback.onError("NOT_SIGNED_IN");
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
                                    callback.onError(e.getMessage() != null ? e.getMessage() : "PROFILE_CREATE_ERROR");
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e.getMessage() != null ? e.getMessage() : "PROFILE_LOAD_ERROR");
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
        return "Learner";
    }
}
