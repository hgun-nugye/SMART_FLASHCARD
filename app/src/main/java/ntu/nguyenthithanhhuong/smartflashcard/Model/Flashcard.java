package ntu.nguyenthithanhhuong.smartflashcard.Model;

import java.io.Serializable;

public class Flashcard implements Serializable {

    public String cardId;
    public String front;
    public String back;
    public String ipa;
    public String example;

    public long nextReview = 0;
    public int repetitions = 0;

    public int interval = 1;
    public double easeFactor = 2.5;

    public Flashcard() {
    }

    public Flashcard(String front, String back, String ipa, String example) {
        this.front = front;
        this.back = back;
        this.ipa = ipa;
        this.example = example;

        this.repetitions = 0;
        this.nextReview = System.currentTimeMillis();
    }

    public enum Status {
        NEW,
        REVIEW,
        LEARNED
    }

    public Status getStatus() {

        if (repetitions <= 0) {
            return Status.NEW;
        }

        if (nextReview <= System.currentTimeMillis()) {
            return Status.REVIEW;
        }

        return Status.LEARNED;
    }

    public boolean isDue() {
        return getStatus() == Status.REVIEW;
    }

    public String getStatusString() {
        // Luôn trả về trạng thái thực tế dựa theo thuật toán SM-2
        return getStatus().name();
    }

}