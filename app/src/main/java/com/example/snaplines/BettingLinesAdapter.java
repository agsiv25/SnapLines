package com.example.snaplines;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snaplines.domain.BettingLine;

import java.util.List;

public class BettingLinesAdapter extends RecyclerView.Adapter<BettingLinesAdapter.BettingLineViewHolder> {

    private List<BettingLine> bettingLines;

    public BettingLinesAdapter(List<BettingLine> bettingLines) {
        this.bettingLines = bettingLines;
    }

    public void updateBettingLines(List<BettingLine> newLines) {
        this.bettingLines = newLines;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BettingLineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.betting_line_item, parent, false);
        return new BettingLineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BettingLineViewHolder holder, int position) {
        BettingLine bettingLine = bettingLines.get(position);
        holder.bookmakerTextView.setText(bettingLine.getBookmaker());
        holder.nameTextView.setText(bettingLine.getDescription());
        holder.priceTextView.setText(String.valueOf(bettingLine.getPrice()));
    }

    @Override
    public int getItemCount() {
        return bettingLines.size();
    }

    public static class BettingLineViewHolder extends RecyclerView.ViewHolder {

        TextView bookmakerTextView;
        TextView nameTextView;
        TextView priceTextView;

        public BettingLineViewHolder(View itemView) {
            super(itemView);
            bookmakerTextView = itemView.findViewById(R.id.bookmaker);
            nameTextView = itemView.findViewById(R.id.name);
            priceTextView = itemView.findViewById(R.id.price);
        }
    }
}

