package ntu.nguyenthithanhhuong.smartflashcard.model;

import java.util.List;

public class AiWordResult {

    public String correctedWord;
    public boolean isCorrect;
    public List<WordMeaning> meanings;

    public AiWordResult(
            String correctedWord,
            boolean isCorrect,
            List<WordMeaning> meanings
    ) {
        this.correctedWord = correctedWord;
        this.isCorrect = isCorrect;
        this.meanings = meanings;
    }
}