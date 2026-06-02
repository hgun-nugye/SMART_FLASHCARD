package ntu.nguyenthithanhhuong.smartflashcard.card;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ntu.nguyenthithanhhuong.smartflashcard.R;
import ntu.nguyenthithanhhuong.smartflashcard.model.WordMeaning;

public class MeaningAdapter
        extends RecyclerView.Adapter<MeaningAdapter.ViewHolder> {

    public interface OnMeaningClickListener {
        void onClick(WordMeaning meaning);
    }

    private final List<WordMeaning> meanings;
    private final OnMeaningClickListener listener;

    public MeaningAdapter(
            List<WordMeaning> meanings,
            OnMeaningClickListener listener
    ) {
        this.meanings = meanings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_meaning,
                        parent,
                        false
                );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        WordMeaning item = meanings.get(position);

        holder.txtMeaning.setText(item.vi);
        holder.txtIpa.setText(item.ipa);
        holder.txtExample.setText(item.example);

        holder.itemView.setOnClickListener(v ->
                listener.onClick(item)
        );
    }

    @Override
    public int getItemCount() {
        return meanings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtMeaning;
        TextView txtIpa;
        TextView txtExample;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtMeaning =
                    itemView.findViewById(R.id.txtMeaning);

            txtIpa =
                    itemView.findViewById(R.id.txtIpa);

            txtExample =
                    itemView.findViewById(R.id.txtExample);
        }
    }
}