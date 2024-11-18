package com.example.snaplines.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snaplines.BettingLinesAdapter;
import com.example.snaplines.R;
import com.example.snaplines.databinding.FragmentHomeBinding;
import com.example.snaplines.domain.BettingLine;
import com.example.snaplines.domain.BettingLinesResponse;
import com.example.snaplines.domain.Bookmaker;
import com.example.snaplines.domain.Market;
import com.example.snaplines.domain.Outcome;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // Retrieve arguments passed during navigation
        Bundle arguments = getArguments();
        if (arguments != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            String bettingLines = arguments.getString("bettingLines");

            BettingLinesResponse bettingLinesResponse;
            try {
                bettingLinesResponse = objectMapper.readValue(bettingLines, BettingLinesResponse.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            if (bettingLinesResponse != null) {
//                List<BettingLine> bettingLineList = new ArrayList<>();
                Map<String, List<BettingLine>> bettingLinesByMarket = new HashMap<>();

                for (Bookmaker bookmaker : bettingLinesResponse.getBookmakers()) {
                    for (Market market : bookmaker.getMarkets()) {
                        String marketKey = Arrays.stream(market.getKey().split("_"))
                                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                                .collect(Collectors.joining(" "));
                        bettingLinesByMarket.putIfAbsent(marketKey, new ArrayList<>());

                        for (Outcome outcome : market.getOutcomes()) {
                            BettingLine bettingLine = new BettingLine();
                            bettingLine.setBookmaker(bookmaker.getTitle());
                            bettingLine.setMarket(market.getKey());
                            bettingLine.setName(outcome.getName());
                            bettingLine.setDescription(outcome.getDescription());
                            bettingLine.setPrice(outcome.getPrice());
                            bettingLine.setLink(outcome.getLink());

                            bettingLinesByMarket.get(marketKey).add(bettingLine);
                        }
                    }
                }

                List<String> marketKeys = new ArrayList<>(bettingLinesByMarket.keySet());

                // Set up the Spinner
                Spinner spinner = binding.getRoot().findViewById(R.id.market_filter_spinner);
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, marketKeys);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(spinnerAdapter);
                spinner.setSelection(marketKeys.indexOf("Spreads"));

                // Set up the RecyclerView
                RecyclerView recyclerView = binding.getRoot().findViewById(R.id.betting_lines_recycler_view);
                BettingLinesAdapter adapter = new BettingLinesAdapter(new ArrayList<>());
                recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
                recyclerView.setAdapter(adapter);

                // Handle Spinner selection
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String selectedMarket = marketKeys.get(position);
                        List<BettingLine> selectedLines = bettingLinesByMarket.getOrDefault(selectedMarket, new ArrayList<>());
                        adapter.updateBettingLines(selectedLines); // Update adapter with filtered lines
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Do nothing
                    }
                });
            }
        }

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}