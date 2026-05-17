package ntu.nguyenthithanhhuong.smartflashcard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import ntu.nguyenthithanhhuong.smartflashcard.Model.Deck;

public class DeckManageAdapter extends RecyclerView.Adapter<DeckManageAdapter.VH> {

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
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deck_manage, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Deck d = decks.get(position);
        h.txtName.setText(d.name);
        h.txtCount.setText(d.cardCount + " thẻ");

        h.btnEdit.setOnClickListener(v -> listener.onEdit(d));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(d));
    }

    @Override
    public int getItemCount() {
        return decks.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtName, txtCount;
        MaterialButton btnEdit, btnDelete;

        public VH(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtDeckName);
            txtCount = itemView.findViewById(R.id.txtCardCount);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

