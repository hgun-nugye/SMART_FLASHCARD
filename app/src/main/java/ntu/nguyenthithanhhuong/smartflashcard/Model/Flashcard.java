package ntu.nguyenthithanhhuong.smartflashcard.Model;

public class Flashcard {
    public String cardId;
    public String front;          // Từ vựng (Người dùng nhập)
    public String back;           // Nghĩa (Groq gợi ý hoặc tự nhập)
    public String ipa;            // Phiên âm (Gemini gợi ý)
    public String example;        // Ví dụ (Gemini gợi ý)

    // Thuộc tính cho thuật toán lặp lại ngắt quãng (Spaced Repetition)
    public long nextReview;       // Thời điểm học tiếp theo (Timestamp)
    public int interval = 1;      // Khoảng cách ngày (mặc định là 1)
    public double easeFactor = 2.5; // Độ dễ (mặc định 2.5 theo SM-2)
    public int repetitions = 0;   // Số lần đã học thành công

    public enum Status {
        NEW,     // Chưa học lần nào
        REVIEW,  // Đến hạn ôn tập
        LEARNED  // Đã thuộc, chưa đến hạn
    }

    /**
     * Tính trạng thái thẻ dựa trên dữ liệu SM-2.
     * - NEW    : repetitions == 0
     * - REVIEW : repetitions > 0 và nextReview <= thời điểm hiện tại
     * - LEARNED: repetitions > 0 và nextReview > thời điểm hiện tại
     */

    public Status getStatus() {
        if (repetitions == 0) return Status.NEW;
        long now = System.currentTimeMillis();
        return (nextReview <= now) ? Status.REVIEW : Status.LEARNED;
    }

    /**
     * Trả về true nếu thẻ đến hạn cần ôn tập (NEW hoặc REVIEW).
     */
    public boolean isDue() {
        Status s = getStatus();
        return s == Status.NEW || s == Status.REVIEW;
    }

    public Flashcard() {
    }

    public Flashcard(String front, String back, String ipa, String example) {
        this.front = front;
        this.back = back;
        this.ipa = ipa;
        this.example = example;
    }
}
