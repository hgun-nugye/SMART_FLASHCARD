package ntu.nguyenthithanhhuong.smartflashcard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import ntu.nguyenthithanhhuong.smartflashcard.Model.Flashcard;

public class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.CardViewHolder> {

    private final List<Flashcard> cardList;
    private final OnCardSpeakClickListener speakClickListener;

    public interface OnCardSpeakClickListener {
        void onSpeakClick(String word);
    }

    public FlashcardAdapter(List<Flashcard> cardList, OnCardSpeakClickListener speakClickListener) {
        this.cardList = cardList;
        this.speakClickListener = speakClickListener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flashcard, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Flashcard card = cardList.get(position);

        holder.tvFront.setText(card.front);
        holder.tvIpa.setText(card.ipa != null ? card.ipa : "");
        holder.tvBack.setText(card.back);

        // Sự kiện bấm nút loa phát âm
        holder.btnPlayCard.setOnClickListener(v -> {
            if (speakClickListener != null) {
                speakClickListener.onSpeakClick(card.front);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView tvFront, tvIpa, tvBack;
        ImageButton btnPlayCard;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFront = itemView.findViewById(R.id.tvFront);
            tvIpa = itemView.findViewById(R.id.tvIpa);
            tvBack = itemView.findViewById(R.id.tvBack);
            btnPlayCard = itemView.findViewById(R.id.btnPlayCard);
        }
    }
}