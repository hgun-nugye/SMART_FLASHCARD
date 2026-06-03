package ntu.nguyenthithanhhuong.smartflashcard.deck;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import ntu.nguyenthithanhhuong.smartflashcard.R;
import ntu.nguyenthithanhhuong.smartflashcard.model.Deck;

public class DeckManageAdapter extends RecyclerView.Adapter<DeckManageAdapter.ViewHolder> {
    public interface DeckActionListener {
        void onEdit(Deck deck);
        void onDelete(Deck deck);
    }
    private final List<Deck> decks;
    private final DeckActionListener listener;

    public DeckManageAdapter(List<Deck> decks, DeckActionListener listener) {
        this.decks = decks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deck_manage, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Deck d = decks.get(position);
        h.txtName.setText(d.name);
        h.txtCount.setText(h.itemView.getContext().getString(R.string.deck_manage_card_count, d.cardCount));

        h.btnEdit.setOnClickListener(v -> listener.onEdit(d));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(d));
    }

    @Override
    public int getItemCount() {
        return decks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtCount;
        MaterialButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtDeckName);
            txtCount = itemView.findViewById(R.id.txtCardCount);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

