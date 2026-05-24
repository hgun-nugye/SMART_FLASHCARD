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

public class CardManageAdapter extends RecyclerView.Adapter<CardManageAdapter.ManageViewHolder> {

    private final List<Flashcard> cardList;
    private final OnCardActionListener actionListener;

    public interface OnCardActionListener {
        void onDelete(Flashcard card);
    }

    public CardManageAdapter(List<Flashcard> cardList, OnCardActionListener actionListener) {
        this.cardList = cardList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ManageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_card, parent, false);
        return new ManageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageViewHolder holder, int position) {
        Flashcard card = cardList.get(position);
        holder.tvFront.setText(card.front);
        holder.tvBack.setText(card.back);

        holder.btnDelete.setOnClickListener(v -> actionListener.onDelete(card));
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }

    static class ManageViewHolder extends RecyclerView.ViewHolder {
        TextView tvFront, tvBack;
        ImageButton btnDelete;

        public ManageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFront = itemView.findViewById(R.id.tvManageFront);
            tvBack = itemView.findViewById(R.id.tvManageBack);
            btnDelete = itemView.findViewById(R.id.btnDeleteCard);
        }
    }
}