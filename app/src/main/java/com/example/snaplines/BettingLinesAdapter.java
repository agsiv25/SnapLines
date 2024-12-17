package com.example.snaplines;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snaplines.domain.BettingLine;
import com.example.snaplines.ui.HomeFragment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class BettingLinesAdapter extends RecyclerView.Adapter<BettingLinesAdapter.BettingLineViewHolder> {

    private List<BettingLine> bettingLines;
    private final OnBettingLineClickListener listener;
    private final Fragment fragment;
    private final Context context;

    public interface OnBettingLineClickListener {
        void onBettingLineClick(String url);
    }

    public BettingLinesAdapter(Context context, Fragment fragment, List<BettingLine> bettingLines, OnBettingLineClickListener listener) {
        this.context = context;
        this.fragment = fragment;
        this.bettingLines = bettingLines;
        this.listener = listener;
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
        if (bettingLine.getDescription() == null) {
            holder.nameTextView.setVisibility(View.GONE);
        } else {
            holder.nameTextView.setVisibility(View.VISIBLE);
            holder.nameTextView.setText(bettingLine.getDescription());
        }
//        holder.nameTextView.setText(bettingLine.getDescription() != null ? bettingLine.getDescription() : bettingLine.getName());
        int price = bettingLine.getPrice();
        String formattedPrice = (price >= 0 ? "+" : "") + price;
        holder.priceTextView.setText(formattedPrice);

        double point = bettingLine.getPoint();
        String market = bettingLine.getMarket();
        if (point != 0.0) {
            holder.underOverTextView.setText(bettingLine.getName());
            holder.lineTextView.setText(String.format("%s %s", point, market));
        } else {
            holder.lineTextView.setText(market);
        }

        holder.bind(context, fragment, bettingLine, listener);
    }

    @Override
    public int getItemCount() {
        return bettingLines.size();
    }

    public static class BettingLineViewHolder extends RecyclerView.ViewHolder {

        TextView bookmakerTextView;
        TextView nameTextView;
        TextView lineTextView;
        TextView priceTextView;
        TextView underOverTextView;

        public BettingLineViewHolder(View itemView) {
            super(itemView);
            bookmakerTextView = itemView.findViewById(R.id.bookmaker);
            nameTextView = itemView.findViewById(R.id.name);
            underOverTextView = itemView.findViewById(R.id.under_over);
            lineTextView = itemView.findViewById(R.id.line);
            priceTextView = itemView.findViewById(R.id.price);
        }

        public void bind(Context context, Fragment fragment, BettingLine bettingLine, OnBettingLineClickListener listener) {
            itemView.setOnClickListener(v -> {
                if (bettingLine.getLink() != null) {
                    listener.onBettingLineClick(bettingLine.getLink());
                }

                if (fragment instanceof HomeFragment) {
                    saveBettingLineToHistory(context, bettingLine);
                }
            });
        }

        private void saveBettingLineToHistory(Context context, BettingLine bettingLine) {

            // Use SharedPreferences API to save betting lines to persistent storage
            SharedPreferences prefs = context.getSharedPreferences("BettingHistory", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            ObjectMapper objectMapper = new ObjectMapper();

            try {
                // Get existing history
                String jsonHistory = prefs.getString("history", "[]");
                List<BettingLine> historyList = objectMapper.readValue(
                        jsonHistory,
                        new TypeReference<List<BettingLine>>() {}
                );

                // Add the new line
                historyList.add(bettingLine);

                // Save updated history as JSON
                String updatedJsonHistory = objectMapper.writeValueAsString(historyList);
                editor.putString("history", updatedJsonHistory);
                editor.apply();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}

