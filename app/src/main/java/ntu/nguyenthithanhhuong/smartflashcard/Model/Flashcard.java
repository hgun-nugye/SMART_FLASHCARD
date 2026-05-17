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

    public Flashcard() {}

    public Flashcard(String front, String back, String ipa, String example) {
        this.front = front;
        this.back = back;
        this.ipa = ipa;
        this.example = example;
    }
}
