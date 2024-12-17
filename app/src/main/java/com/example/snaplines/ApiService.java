package com.example.snaplines;

import com.example.snaplines.domain.BettingLinesResponse;
import com.example.snaplines.domain.Game;
import com.example.snaplines.domain.UploadResponse;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @Multipart
    @POST("football")
    Call<UploadResponse> uploadImage(@Part MultipartBody.Part part);

    @GET("/v4/sports/{sport}/events")
    Call<List<Game>> getGameIds(
            @Path("sport") String sport,
            @Query("apiKey") String apiKey,
            @Query("dateFormat") String id,
            @Query("commenceTimeFrom") String commenceTimeFrom,
            @Query("commenceTimeTo") String commenceTimeTo
    );

    @GET("/v4/sports/{sport}/events/{eventId}/odds")
    Call<BettingLinesResponse> getLines(
            @Path("sport") String sport,
            @Path("eventId") String eventId,
            @Query("apiKey") String apiKey,
            @Query("regions") String regions,
            @Query("markets") String markets,
            @Query("dateFormat") String dateFormat,
            @Query("oddsFormat") String oddsFormat,
            @Query("includeLinks") boolean includeLinks,
            @Query("includeSids") boolean includeSids,
            @Query("includeBetLimits") boolean includeBetLimits
    );
}
