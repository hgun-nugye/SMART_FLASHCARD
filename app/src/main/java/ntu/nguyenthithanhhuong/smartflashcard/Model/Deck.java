package ntu.nguyenthithanhhuong.smartflashcard.Model;

public class Deck {
    public String deckId;
    public String ownerId; // Chính là UID của User
    public String name;
    public String description;
    public int cardCount;

    public Deck() {}

    public Deck(String name, String description, String ownerId) {
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
    }
}