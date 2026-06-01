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

public class ReviewDeckAdapter extends RecyclerView.Adapter<ReviewDeckAdapter.Holder> {
    public interface OnStartReviewListener {
        void onStartReview(Deck deck);
    }

    private final List<Deck> decks;
    private final OnStartReviewListener listener;

    public ReviewDeckAdapter(List<Deck> decks, OnStartReviewListener listener) {
        this.decks = decks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review_deck, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Deck deck = decks.get(position);
        holder.txtName.setText(deck.name != null ? deck.name : "");
        holder.txtDue.setText(holder.itemView.getContext().getString(
                R.string.review_due_count,
                Math.max(deck.dueCount, 0)
        ));
        holder.btnStart.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStartReview(deck);
            }
        });
        holder.itemView.setOnClickListener(v -> holder.btnStart.performClick());
    }

    @Override
    public int getItemCount() {
        return decks.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView txtName;
        final TextView txtDue;
        final MaterialButton btnStart;

        Holder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtReviewDeckName);
            txtDue = itemView.findViewById(R.id.txtReviewDue);
            btnStart = itemView.findViewById(R.id.btnStartReview);
        }
    }
}
