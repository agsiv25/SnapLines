package com.example.snaplines.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snaplines.BettingLinesAdapter;
import com.example.snaplines.R;
import com.example.snaplines.databinding.FragmentHistoryBinding;
import com.example.snaplines.domain.BettingLine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;

    private RecyclerView recyclerView;
    private BettingLinesAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHistoryBinding.inflate(inflater, container, false);

        recyclerView = binding.getRoot().findViewById(R.id.history_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        loadBettingHistory();

        Button clearButton = binding.getRoot().findViewById(R.id.clear_all_button);
        clearButton.setOnClickListener(v -> {
            SharedPreferences prefs = requireContext().getSharedPreferences("BettingHistory", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            adapter.updateBettingLines(Collections.emptyList());
        });


        return binding.getRoot();
    }

    // Load in saved betting history from persistent storage
    private void loadBettingHistory() {
        SharedPreferences prefs = requireContext().getSharedPreferences("BettingHistory", Context.MODE_PRIVATE);
        String jsonHistory = prefs.getString("history", "[]");

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            List<BettingLine> historyList = objectMapper.readValue(jsonHistory, new TypeReference<List<BettingLine>>() {});
            adapter = new BettingLinesAdapter(requireContext(), this, historyList, url -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            });
            recyclerView.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}