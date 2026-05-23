package ntu.nguyenthithanhhuong.smartflashcard.Model;

public class User {
    public String uid;
    public String email;
    public String fullName;
    public String phone;
    public long createdAt;

    public User() {} // Bắt buộc có cho Firebase

    public User(String uid, String email, String fullName) {
        this(uid, email, fullName, null);
    }

    public User(String uid, String email, String fullName, String phone) {
        this.uid = uid;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.createdAt = System.currentTimeMillis();
    }
}