package ntu.nguyenthithanhhuong.smartflashcard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ntu.nguyenthithanhhuong.smartflashcard.Model.Deck;

public class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.DeckViewHolder> {
    private List<Deck> deckList;
    private OnDeckClickListener listener;

    public interface OnDeckClickListener {
        void onDeckClick(Deck deck);
    }

    public DeckAdapter(List<Deck> deckList, OnDeckClickListener listener) {
        this.deckList = deckList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeckViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deck, parent, false);
        return new DeckViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeckViewHolder holder, int position) {
        Deck deck = deckList.get(position);
        holder.txtDeckName.setText(deck.name);
        holder.txtCardCount.setText(deck.cardCount + " thẻ");

        holder.itemView.setOnClickListener(v -> listener.onDeckClick(deck));
    }

    @Override
    public int getItemCount() {
        return deckList.size();
    }

    public static class DeckViewHolder extends RecyclerView.ViewHolder {
        TextView txtDeckName, txtCardCount;

        public DeckViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDeckName = itemView.findViewById(R.id.txtDeckName);
            txtCardCount = itemView.findViewById(R.id.txtCardCount);
        }
    }
}