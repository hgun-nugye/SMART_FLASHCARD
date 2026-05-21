package ntu.nguyenthithanhhuong.smartflashcard.Model;

import java.io.Serializable;

public class Flashcard implements Serializable {
    public String cardId;
    public String front;          // Từ vựng
    public String back;           // Nghĩa (Groq gợi ý hoặc tự nhập)
    public String ipa;            // Phiên âm
    public String example;        // Ví dụ

    // Thuộc tính cho thuật toán lặp lại ngắt quãng (Spaced Repetition)
    public long nextReview;       // Thời điểm học tiếp theo (Timestamp)
    public int interval = 1;      // Khoảng cách ngày (mặc định là 1)
    public double easeFactor = 2.5; // Độ dễ (mặc định 2.5 theo SM-2)
    public int repetitions = 0;   // Số lần đã học thành công

    // Thêm biến này để Firestore có thể lưu xuống Database thành dạng chuỗi "NEW", "REVIEW", "LEARNED"
    private String status = "NEW";

    public enum Status {
        NEW,     // Chưa học lần nào
        REVIEW,  // Đến hạn ôn tập
        LEARNED  // Đã thuộc, chưa đến hạn
    }

    public Flashcard() {
    }

    public Flashcard(String front, String back, String ipa, String example) {
        this.front = front;
        this.back = back;
        this.ipa = ipa;
        this.example = example;
        this.repetitions = 0;
        this.status = "NEW";
        this.nextReview = System.currentTimeMillis(); // Mặc định là ngay bây giờ
    }

    public Status getStatus() {
        if (repetitions == 0) {
            return Status.NEW;
        }

        long now = System.currentTimeMillis();
        if (nextReview <= now) {
            return Status.REVIEW;
        } else {
            return Status.LEARNED;
        }
    }

    public void setStatus(Status status) {
        if (status != null) {
            this.status = status.name();
        }
    }

    public String getStatusString() {
        // Luôn trả về trạng thái thực tế dựa theo thuật toán SM-2
        return getStatus().name();
    }

    public void setStatusString(String statusString) {
        this.status = statusString;
    }


     //Trả về true nếu thẻ đến hạn cần ôn tập (NEW hoặc REVIEW).
    public boolean isDue() {
        Status s = getStatus();
        return s == Status.NEW || s == Status.REVIEW;
    }
}