package com.example.fuelwise.APICalls.EVStations;

import com.example.fuelwise.models.chargningStationModels.ChargingStation;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import java.util.List;

public interface OpenChargeMapService {
    @GET("v3/poi/")
    Call<List<ChargingStation>> getChargingStations(
            @Query("key") String apiKey,
            @Query("countrycode") String countryCode,
            @Query("maxresults") int maxResults
    );
}