package ntu.nguyenthithanhhuong.smartflashcard.Model;

public class Deck {
    public String deckId;
    public String ownerId;
    public String name;
    public String description;
    public int cardCount;

    public int newCount = 0;
    public int dueCount = 0;
    public int learnedCount = 0;
    public Deck() {}

    public Deck(String name, String description, String ownerId) {
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
    }
}