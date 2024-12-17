package com.example.snaplines.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snaplines.ApiService;
import com.example.snaplines.BettingLinesAdapter;
import com.example.snaplines.BuildConfig;
import com.example.snaplines.R;
import com.example.snaplines.databinding.FragmentHomeBinding;
import com.example.snaplines.domain.BettingLine;
import com.example.snaplines.domain.BettingLinesResponse;
import com.example.snaplines.domain.Bookmaker;
import com.example.snaplines.domain.Game;
import com.example.snaplines.domain.Market;
import com.example.snaplines.domain.Outcome;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private ObjectMapper objectMapper;
    private Retrofit retrofit;
    private ApiService apiService;

    String apiKey = BuildConfig.API_KEY;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        // Sports betting api
        retrofit = new Retrofit.Builder()
                .baseUrl("https://api.the-odds-api.com/")
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build();

        apiService = retrofit.create(ApiService.class);

        // Retrieve arguments passed during navigation
        Bundle arguments = getArguments();
        if (arguments != null) {
            String team1 = arguments.getString("team1");
            String team2 = arguments.getString("team2");

            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime weekLater = now.plusWeeks(1);

            // Use ISO 8601 format with only 2 units for seconds
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");
            String commenceTimeFrom = now.format(formatter);
            String commenceTimeTo = weekLater.format(formatter);

            apiService.getGameIds("americanfootball_nfl", apiKey, "iso", commenceTimeFrom, commenceTimeTo).enqueue(new Callback<List<Game>>() {
                @Override
                public void onResponse(@NonNull Call<List<Game>> call, @NonNull Response<List<Game>> response) {
                    List<Game> games = response.body();

                    if (response.isSuccessful() && games != null) {
                        String gameId = null;

                        // Iterate over all on-going games to find the one that involves the given teams
                        for (Game game : games) {

                            String homeTeam = game.getHomeTeam().trim();
                            String awayTeam = game.getAwayTeam().trim();

                            boolean isTeam1Involved = team1.equals(homeTeam) || team1.equals(awayTeam);
                            boolean isTeam2Involved = team2.equals(homeTeam) || team2.equals(awayTeam);

                            if (isTeam1Involved && isTeam2Involved) {
                                gameId = game.getId();
                                break;
                            }
                        }

                        // If a gameId was found, then handle it
                        if (gameId != null) {
                            processGameId(gameId);
                        } else {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    Toast.makeText(requireContext(), "No games found for the given teams.", Toast.LENGTH_LONG).show()
                            );
                        }
                    } else {
                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(requireContext(), "Failed to get betting lines. Please try again.", Toast.LENGTH_LONG).show()
                        );
                    }
                }

                @Override
                public void onFailure(Call<List<Game>> call, Throwable t) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(requireContext(), "Failed to get betting lines. Please try again.", Toast.LENGTH_LONG).show()
                    );
                }
            });

        }

        return binding.getRoot();
    }

    private void processGameId(String gameId) {

        List<String> marketsList = Arrays.asList(
                // Team lines
                "h2h", "spreads", "totals",

                // PLayer lines
                "player_assists", "player_field_goals", "player_kicking_points",
                "player_pass_attempts", "player_pass_completions", "player_pass_interceptions",
                "player_pass_longest_completion", "player_pass_rush_reception_tds",
                "player_pass_rush_reception_yds", "player_pass_tds", "player_pass_yds",
                "player_pats", "player_receptions", "player_reception_yds", "player_rush_attempts",
                "player_rush_reception_tds", "player_rush_reception_yds", "player_rush_yds",
                "player_sacks", "player_solo_tackles", "player_tackles_assists",
                "player_tds_over", "player_anytime_td"
        );

        String markets = String.join(",", marketsList);

        apiService.getLines("americanfootball_nfl", gameId, apiKey, "us", markets, "iso", "american", true, true, true).enqueue(new Callback<BettingLinesResponse>() {
            @Override
            public void onResponse(@NonNull Call<BettingLinesResponse> call, @NonNull Response<BettingLinesResponse> response) {
                BettingLinesResponse bettingLinesResponse = response.body();

                if (response.isSuccessful() && bettingLinesResponse != null) {
                    // Handle the response
                    processBettingLinesResponse(bettingLinesResponse);
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(requireContext(), "Failed to get betting lines. Please try again.", Toast.LENGTH_LONG).show()
                    );
                }
            }

            @Override
            public void onFailure(Call<BettingLinesResponse> call, Throwable t) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(requireContext(), "Failed to get betting lines. Please try again.", Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void processBettingLinesResponse(BettingLinesResponse bettingLinesResponse) {

        if (bettingLinesResponse != null) {
            Map<String, List<BettingLine>> bettingLinesByMarket = new HashMap<>();

            for (Bookmaker bookmaker : bettingLinesResponse.getBookmakers()) {
                for (Market market : bookmaker.getMarkets()) {
                    // Market keys are used for category titles, so make it human readable
                    String marketKey = Arrays.stream(market.getKey().split("_"))
                            .filter(word -> !"player".equalsIgnoreCase(word))
                            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                            .collect(Collectors.joining(" "));

                    bettingLinesByMarket.putIfAbsent(marketKey, new ArrayList<>());

                    // Set all betting lines
                    for (Outcome outcome : market.getOutcomes()) {
                        BettingLine bettingLine = new BettingLine();

                        // Don't add if there isn't a link to access it
                        if (outcome.getLink() == null) continue;

                        bettingLine.setBookmaker(bookmaker.getTitle());
                        bettingLine.setMarket(marketKey);
                        bettingLine.setName(outcome.getName());
                        bettingLine.setDescription(outcome.getDescription());
                        bettingLine.setPoint(outcome.getPoint());
                        bettingLine.setPrice(outcome.getPrice());

                        // If link requires a state, use Arizona for now because all bets are
                        // allowed there, so the user won't go to a non existent Wisconsin link
                        // If its not actually available in the user's location, let the sportsbook
                        // app handle that scenario.
                        bettingLine.setLink(outcome.getLink().replace("{state}", "az"));

                        bettingLinesByMarket.get(marketKey).add(bettingLine);
                    }

                    // Sort betting lines by payout, so that the best paying lines appear at the top
                    // Suggested during ECE Capstone event
                    bettingLinesByMarket.get(marketKey).sort((o1, o2) ->
                            Double.compare(Math.abs(o2.getPrice()), Math.abs(o1.getPrice())));
                }
            }

            List<String> marketKeys = new ArrayList<>(bettingLinesByMarket.keySet());

            // Set up the Spinner
            Spinner spinner = binding.getRoot().findViewById(R.id.market_filter_spinner);
            spinner.setVisibility(View.VISIBLE);
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, marketKeys);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(spinnerAdapter);
            spinner.setSelection(marketKeys.indexOf("Spreads")); // Set Spreads as default market

            // Set up the RecyclerView
            RecyclerView recyclerView = binding.getRoot().findViewById(R.id.betting_lines_recycler_view);
            BettingLinesAdapter adapter = new BettingLinesAdapter(requireContext(), this, new ArrayList<>(), url -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            });
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2)); // 2 tiles wide
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}