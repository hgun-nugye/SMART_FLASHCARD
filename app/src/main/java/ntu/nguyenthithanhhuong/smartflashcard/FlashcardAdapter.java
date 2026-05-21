package ntu.nguyenthithanhhuong.smartflashcard;

import android.content.Context;
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
        void onDetailClick(Flashcard card);
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

        // Đổ dữ liệu an toàn lên giao diện
        holder.tvFront.setText(card.front != null ? card.front : "");
        holder.tvIpa.setText(card.ipa != null ? card.ipa : "");
        holder.tvBack.setText(card.back != null ? card.back : "");

        // Gọi hàm tính toán và hiển thị status
        bindStatus(holder.tvStatus, card.getStatus());

        holder.btnPlayCard.setOnClickListener(v -> {
            if (speakClickListener != null && card.front != null && !card.front.isEmpty()) {
                speakClickListener.onSpeakClick(card.front);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (speakClickListener != null) {
                speakClickListener.onDetailClick(card);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cardList != null ? cardList.size() : 0;
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView tvFront, tvIpa, tvBack, tvStatus;
        ImageButton btnPlayCard;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFront = itemView.findViewById(R.id.tvFront);
            tvIpa = itemView.findViewById(R.id.tvIpa);
            tvBack = itemView.findViewById(R.id.tvBack);
            btnPlayCard = itemView.findViewById(R.id.btnPlayCard);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }

    private void bindStatus(TextView tvStatus, Flashcard.Status status) {
        if (status == null) {
            status = Flashcard.Status.NEW;
        }
        Context ctx = tvStatus.getContext();

        switch (status) {
            case NEW:
                tvStatus.setText("NEW");
                tvStatus.setTextColor(ctx.getColor(R.color.status_new_text));
                tvStatus.setBackgroundResource(R.drawable.bg_status_new);
                break;

            case REVIEW:
                tvStatus.setText("REVIEW");
                tvStatus.setTextColor(ctx.getColor(R.color.status_review_text));
                tvStatus.setBackgroundResource(R.drawable.bg_status_review);
                break;

            case LEARNED:
                tvStatus.setText("LEARNED");
                tvStatus.setTextColor(ctx.getColor(R.color.status_learned_text));
                tvStatus.setBackgroundResource(R.drawable.bg_status_learned);
                break;

            default:
                tvStatus.setText("NEW");
                tvStatus.setTextColor(ctx.getColor(R.color.status_new_text));
                tvStatus.setBackgroundResource(R.drawable.bg_status_new);
                break;
        }
    }
}