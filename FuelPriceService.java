package com.example.fuelwise.APICalls;


import com.example.fuelwise.models.FuelStationModels.FuelPriceResponse;
import com.example.fuelwise.models.FuelStationModels.FuelStation;

import retrofit2.Call;
import retrofit2.http.GET;

public interface FuelPriceService {
    @GET("en_gb/united-kingdom/home/fuelprices/fuel_prices_data.json")
    Call<FuelPriceResponse> getFuelPrices();
}