package ntu.nguyenthithanhhuong.smartflashcard.model;

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
        DUE,
        LEARNED
    }

    private static final long ONE_DAY_MS = 24L * 60 * 60 * 1000L;
    private static final double MIN_EASE_FACTOR = 1.3;

    public Status getStatus() {
        long now = System.currentTimeMillis();
        if (nextReview <= now) {
            return Status.DUE;
        }
        if (repetitions <= 0) {
            return Status.NEW;
        }
        return Status.LEARNED;
    }

    public boolean isDue() {
        return nextReview <= System.currentTimeMillis();
    }

    public String getStatusString() {
        return getStatus().name();
    }

    public void applyReviewResult(boolean isCorrect) {
        int quality = isCorrect ? 4 : 1;

        if (quality >= 3) {
            if (repetitions == 0) {
                interval = 1;
            } else if (repetitions == 1) {
                interval = 6;
            } else {
                interval = (int) Math.round(interval * easeFactor);
                if (interval < 1) {
                    interval = 1;
                }
            }
            repetitions++;
        } else {
            repetitions = 0;
            interval = 1;
        }

        easeFactor = easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        if (easeFactor < MIN_EASE_FACTOR) {
            easeFactor = MIN_EASE_FACTOR;
        }

        long now = System.currentTimeMillis();
        if (isCorrect) {
            nextReview = now + (long) interval * ONE_DAY_MS;
        } else {
            nextReview = now;
        }
    }
}