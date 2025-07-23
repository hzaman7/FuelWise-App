package com.example.fuelwise;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fuelwise.customComponents.CustomSnackBar;
import com.example.fuelwise.firebaseCalls.FireStoreDB;
import com.example.fuelwise.models.FuelFirebaseModel;
import com.example.fuelwise.models.MarkerTag;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.fuelwise.APICalls.EVStations.OpenChargeMapService;
import com.example.fuelwise.APICalls.EVStations.RetroEVClient;
import com.example.fuelwise.APICalls.FuelPriceService;
import com.example.fuelwise.APICalls.RetrofitClient;

import com.example.fuelwise.models.FuelStationModels.FuelPriceResponse;
import com.example.fuelwise.models.FuelStationModels.FuelStation;
import com.example.fuelwise.models.chargningStationModels.ChargingStation;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity  implements OnMapReadyCallback {


    private GoogleMap mMap;

    ImageView filterIVBtn,graphIVBtn,logoutIVBtn;
    ProgressBar loading_pb;
    FireStoreDB fireStoreDB;
    LatLng userLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    Retrofit retrofit,retroEvClientfit;

    OpenChargeMapService servEVCharging;

    // API key
    String apiKey = "e3d9fbfd-254e-44c6-bda1-f04a4c478828";
    String countryCode = "GB";
    int maxResults = 50;

    private List<Marker> allMarkers = new ArrayList<>();
    FuelPriceService service;


    private void filterMarkers(int checkedId, float minPrice, int maxDistance) {
        for (Marker marker : allMarkers) {
            MarkerTag markerTag = (MarkerTag) marker.getTag();
            if (markerTag != null) {
                boolean matchesType = false;
                boolean matchesPrice = (minPrice == 0);
                boolean matchesDistance = (maxDistance == 0);


                if (checkedId == R.id.radio_ev_stations && markerTag.getType().equals("ev")) {
                    matchesType = true;
                } else if (checkedId == R.id.radio_fuel_stations && markerTag.getType().equals("fuel")) {
                    matchesType = true;
                } else if (checkedId == -1) {
                    matchesType = true;
                }


                if (!matchesPrice) {
                    if (markerTag.getType().equals("fuel")) {
                        FuelStation station = (FuelStation) markerTag.getStation();
                        float stationPrice = (float) (station.getPrices().getE10() / 100f);
                        matchesPrice = stationPrice >= minPrice;
                    } else if (markerTag.getType().equals("ev")) {
                        ChargingStation station = (ChargingStation) markerTag.getStation();
                        float stationPrice = (float) station.getConnections().get(0).getPowerKW();
                        matchesPrice = stationPrice >= minPrice;
                    }
                }


                if (!matchesDistance && userLocation != null) {
                    LatLng stationLocation = null;
                    if (markerTag.getType().equals("fuel")) {
                        FuelStation station = (FuelStation) markerTag.getStation();
                        stationLocation = new LatLng(station.getLocation().getLatitude(), station.getLocation().getLongitude());
                    } else if (markerTag.getType().equals("ev")) {
                        ChargingStation station = (ChargingStation) markerTag.getStation();
                        stationLocation = new LatLng(station.getAddressInfo().getLatitude(), station.getAddressInfo().getLongitude());
                    }

                    if (stationLocation != null) {
                        float distance = calculateDistance(userLocation, stationLocation);
                        matchesDistance = distance <= maxDistance;
                    }
                }


                marker.setVisible(matchesType && matchesPrice && matchesDistance);


                System.out.println("Marker: " + marker.getTitle() +
                        ", Type: " + markerTag.getType() +
                        ", Price: " + (markerTag.getType().equals("fuel") ? ((FuelStation) markerTag.getStation()).getPrices().getE10() / 100f : "N/A") +
                        ", Distance: " + (matchesDistance ? "Within Range" : "Out of Range") +
                        ", Visible: " + marker.isVisible());
            }
        }
    }


    private float calculateDistance(LatLng start, LatLng end) {
        float[] results = new float[1];
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results);
        return results[0] / 1000;
    }


    private int selectedFilterId = -1;
    private int savedPriceProgress = 0;
    private int savedDistanceProgress = 0;



    public void showGraphsDialogs() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.graphs_dialog, null);


        BarChart fuelPriceChart = dialogView.findViewById(R.id.fuelPriceChart);
        BarChart evPriceChart = dialogView.findViewById(R.id.evPriceChart);
        Button btnCloseBTN = dialogView.findViewById(R.id.btnCloseBTN);



        ArrayList<BarEntry> fuelPrices = new ArrayList<>();
        fuelPrices.add(new BarEntry(0, 3.75f));
        fuelPrices.add(new BarEntry(1, 4.10f));
        fuelPrices.add(new BarEntry(2, 3.95f));

        ArrayList<BarEntry> evPrices = new ArrayList<>();
        evPrices.add(new BarEntry(0, 0.12f));
        evPrices.add(new BarEntry(1, 0.25f));
        evPrices.add(new BarEntry(2, 0.18f));


        setupBarChart(fuelPriceChart, fuelPrices, "Fuel Prices ($/gallon)",
                new String[]{"Regular", "Premium", "Diesel"}, Color.parseColor("#FF5722"));


        setupBarChart(evPriceChart, evPrices, "EV Charging Prices ($/kWh)",
                new String[]{"Home", "Public", "Fast"}, Color.parseColor("#4CAF50"));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialog.show();

        btnCloseBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }

    private void setupBarChart(BarChart chart, ArrayList<BarEntry> entries, String label,
                               String[] xAxisLabels, int barColor) {

        BarDataSet dataSet = new BarDataSet(entries, label);
        dataSet.setColor(barColor);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);


        chart.setData(barData);
        chart.getDescription().setEnabled(false);
        chart.setFitBars(true);
        chart.animateY(1000);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);


        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));


        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(0.5f);
        chart.getAxisRight().setEnabled(false);
    }
    public void showFilterDialogs() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.filter_dialog, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true);


        RadioGroup radioGroupFilter = dialogView.findViewById(R.id.radio_group_filter);
        TextView priceSeekTv = dialogView.findViewById(R.id.priceSeekTv);
        TextView distanceSeekTv = dialogView.findViewById(R.id.distanceSeekTv);

        SeekBar seekBarPrice = dialogView.findViewById(R.id.seekBarPrice);
        SeekBar seekBarDistance = dialogView.findViewById(R.id.seekBarDistance);
        Button btnClearFilters = dialogView.findViewById(R.id.btnClearFilters);
        Button btnApplyFilters = dialogView.findViewById(R.id.btnApplyFilters);

        if (selectedFilterId != -1) {
            radioGroupFilter.check(selectedFilterId);
        }

        seekBarPrice.setProgress(savedPriceProgress);
        seekBarDistance.setProgress(savedDistanceProgress);


        priceSeekTv.setText("£ " + savedPriceProgress);
        distanceSeekTv.setText(savedDistanceProgress + " km");


        seekBarPrice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                priceSeekTv.setText("£ " + progress/ 100f);
                savedPriceProgress = progress; // Save the progress
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                distanceSeekTv.setText(progress + " km");
                savedDistanceProgress = progress; // Save the progress
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Create and show the dialog
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        // Clear Filters Button
        btnClearFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show all markers
                for (Marker marker : allMarkers) {
                    marker.setVisible(true);
                }


                radioGroupFilter.clearCheck();
                selectedFilterId = -1;
                seekBarDistance.setProgress(0);
                seekBarPrice.setProgress(0);


                savedPriceProgress = 0;
                savedDistanceProgress = 0;


                dialog.dismiss();
            }
        });


        btnApplyFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int checkedId = radioGroupFilter.getCheckedRadioButtonId();


                float minPrice = seekBarPrice.getProgress() / 100f;
                int maxDistance = seekBarDistance.getProgress();


                filterMarkers(checkedId, minPrice, maxDistance);


                selectedFilterId = checkedId;
                dialog.dismiss();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        getWindow().setStatusBarColor(getResources().getColor(R.color.app_sec_color));

        fireStoreDB = new FireStoreDB();


        loading_pb = findViewById(R.id.loading_pb);
        graphIVBtn = findViewById(R.id.graphIVBtn);
        graphIVBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                showGraphsDialogs();
            }
        });

        logoutIVBtn = findViewById(R.id.logoutIVBtn);
        logoutIVBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        filterIVBtn = findViewById(R.id.filterIVBtn);
        filterIVBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showFilterDialogs();
            }
        });



        retrofit = RetrofitClient.getClient();
        retroEvClientfit = RetroEVClient.getClient();
        service = retrofit.create(FuelPriceService.class);
        servEVCharging = retroEvClientfit.create(OpenChargeMapService.class);



        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        checkLocationPermission();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {

            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getDeviceLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, new com.google.android.gms.tasks.OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                     userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                                    mMap.addMarker(new MarkerOptions().position(userLocation).title("You are here")
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                    builder.include(userLocation);

                                    getAllFuelStations(mMap,builder);




                                }
                            }


                        });
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void getEvChargningStations(GoogleMap mMap, LatLngBounds.Builder builder){
        Call<List<ChargingStation>> call = servEVCharging.getChargingStations(apiKey, countryCode, maxResults);
        call.enqueue(new Callback<List<ChargingStation>>() {
            @Override
            public void onResponse(Call<List<ChargingStation>> call, Response<List<ChargingStation>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ChargingStation> chargingStations = response.body();
                    for (ChargingStation station : chargingStations) {


                        System.out.println("@S@t@ation: " + station.getAddressInfo().getTitle() +
                                ", Location: " + station.getAddressInfo().getLatitude() +
                                ", " + station.getAddressInfo().getLongitude());


                        LatLng evSatLocations = new LatLng(station.getAddressInfo().getLatitude() ,
                                station.getAddressInfo().getLongitude());

                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(evSatLocations)
                                .title(station.getAddressInfo().getTitle() + "," +
                                        station.getAddressInfo().getAddressLine1() +
                                        ", " + station.getAddressInfo().getTown() +
                                        ", " + station.getAddressInfo().getPostcode() + "," +
                                        "Connection Type: " + station.getConnections().get(0).getConnectionType().getTitle()+ "," +
                                        "PowerKW: " + station.getConnections().get(0).getPowerKW())
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ev_charge))
                        );
                        //marker.setTag(station);
                        marker.setTag(new MarkerTag("ev", station)); // Set custom tag
                        allMarkers.add(marker);

                        builder.include(evSatLocations);


                    }

                    LatLngBounds bounds = builder.build();
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 50);
                    mMap.animateCamera(cameraUpdate);

                    mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker) {
                            MarkerTag markerTag = (MarkerTag) marker.getTag();

                            if (markerTag != null) {
                                String type = markerTag.getType();
                                Object station = markerTag.getStation();

                                if (type.equals("fuel")) {

                                    FuelStation fuelStation = (FuelStation) station;


                                    loading_pb.setVisibility(View.VISIBLE);
                                    fireStoreDB.getAllReviewsFetched(MainActivity.this,MainActivity.this,
                                            fuelStation.getSiteId()+"","fuel",fuelStation,null);
                                    //openBottomSheet(fuelStation);
                                } else if (type.equals("ev")) {
                                    
                                    ChargingStation chargingStation = (ChargingStation) station;
//                                    String message = chargingStation.getAddressInfo().getTitle() + "," +
//                                            chargingStation.getAddressInfo().getAddressLine1() +
//                                            ", " + chargingStation.getAddressInfo().getTown() +
//                                            ", " + chargingStation.getAddressInfo().getPostcode() + "," +
//                                            "Connection Type: " + chargingStation.getConnections().get(0).getConnectionType().getTitle() + "," +
//                                            "PowerKW: " + chargingStation.getConnections().get(0).getPowerKW();

                                    loading_pb.setVisibility(View.VISIBLE);
                                    fireStoreDB.getAllReviewsFetched(MainActivity.this,MainActivity.this,
                                            chargingStation.getId()+"","ev",null,chargingStation);
                                    //openEVStationSheet(chargingStation);

                                }
                            } else {
                                Toast.makeText(MainActivity.this, "Station data not found", Toast.LENGTH_SHORT).show();
                            }

                            return false;
                        }
                    });



                    loading_pb.setVisibility(View.GONE);



                } else {
                    System.out.println("API call failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<ChargingStation>> call, Throwable t) {
                System.out.println("API call failed: " + t.getMessage());
            }
        });
    }

    private void getAllFuelStations(GoogleMap mMap, LatLngBounds.Builder builder) {

        Call<FuelPriceResponse> call = service.getFuelPrices();
        call.enqueue(new Callback<FuelPriceResponse>() {
            @Override
            public void onResponse(Call<FuelPriceResponse> call, Response<FuelPriceResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    FuelPriceResponse fuelPriceResponse = response.body();
                    String lastUpdated = fuelPriceResponse.getLastUpdated();
                    List<FuelStation> stations = fuelPriceResponse.getStations();


                    System.out.println("Last Updated: " + lastUpdated);

                    for (FuelStation station : stations) {
//                        System.out.println("Station: " + station.getBrand() +
//                                ", E10 Price: " + station.getPrices().getE10());

                        LatLng fuelLocations = new LatLng(station.getLocation().getLatitude(),
                                station.getLocation().getLongitude());

                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(fuelLocations)
                                .title(station.getBrand() + "," +
                                        station.getAddress() + "," +
                                        "Petrol Price: " + station.getPrices().getE10() + "," +
                                        "Diesel Price: " + station.getPrices().getB7())
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.fuel_point))
                        );
                        //marker.setTag(station);

                        marker.setTag(new MarkerTag("fuel", station)); // Set custom tag
                        allMarkers.add(marker);

                        builder.include(fuelLocations);
                    }

                    LatLngBounds bounds = builder.build();
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 50);
                    mMap.animateCamera(cameraUpdate);


//                    mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
//                        @Override
//                        public boolean onMarkerClick(Marker marker) {
//                            FuelStation station = (FuelStation) marker.getTag();
//
//                            if (station != null) {
//                                String message = "Station: " + station.getBrand() + "\n" +
//                                        "Address: " + station.getAddress() + "\n" +
//                                        "Petrol Price: " + station.getPrices().getE10() + "\n" +
//                                        "Diesel Price: " + station.getPrices().getB7();
//
//                                openBottomSheet(station);
//                            } else {
//                                Toast.makeText(MainActivity.this, "Station data not found", Toast.LENGTH_SHORT).show();
//                            }
//
//                            return false;
//                        }
//
//
//                    });


                    getEvChargningStations(mMap,builder);
                    //loading_pb.setVisibility(View.GONE);
                } else {
                    System.out.println("API call failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<FuelPriceResponse> call, Throwable t) {
                System.out.println("API call failed: " + t.getMessage());
            }
        });

    }

    private void showRateAndReviewSheet(String stationID){

        View bottomSheetView = getLayoutInflater().inflate(R.layout.rate_review_bottom_sheet, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);

        Button btn_submit = bottomSheetView.findViewById(R.id.btn_submit);
        EditText edit_review = bottomSheetView.findViewById(R.id.edit_review);
        RatingBar ratingBar = bottomSheetView.findViewById(R.id.rating_bar);

        FuelFirebaseModel fuelFirebaseModel = new FuelFirebaseModel();
        fuelFirebaseModel.setStationID(stationID);
        fuelFirebaseModel.setRating(ratingBar.getRating());
        fuelFirebaseModel.setUserEmail(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        fuelFirebaseModel.setUserID(FirebaseAuth.getInstance().getUid());


        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float rating = ratingBar.getRating();
                String reviewText = edit_review.getText().toString();
                if (rating >= 1 && !reviewText.isEmpty()) {
                    loading_pb.setVisibility(View.VISIBLE);

                    fuelFirebaseModel.setRating(ratingBar.getRating());
                    fuelFirebaseModel.setStationReview(reviewText);

                    fireStoreDB.createRatingsToFireBaseDB(MainActivity.this,
                            MainActivity.this,fuelFirebaseModel);


                    bottomSheetDialog.dismiss();
                } else {
                    edit_review.setError("Please enter a review and select a rating.");
                }
            }
        });




        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();

    }


    /////////////////////////////////////
    /////EV Station Bottom Sheet/////////
    /////////////////////////////////////
    private void openEVStationSheet(ChargingStation chargingStation){

        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_ev_sheet, null);
        BottomSheetDialog bottomEVStationSheet = new BottomSheetDialog(MainActivity.this);

        RatingBar rating_bar = bottomSheetView.findViewById(R.id.rating_bar);
        rating_bar.setRating(totalRating);
        TextView tvViewAllRatings = bottomSheetView.findViewById(R.id.tvViewAllRatings);
        tvViewAllRatings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,AllRatingsActivity.class);
                intent.putExtra("STATION_ID", chargingStation.getId()+"");
                startActivity(intent);
            }
        });
        TextView tvEvTitle = bottomSheetView.findViewById(R.id.tvEvTitle);
        TextView tvConType = bottomSheetView.findViewById(R.id.tvConType);
        TextView tvPower = bottomSheetView.findViewById(R.id.tvPower);
        TextView tvAddress = bottomSheetView.findViewById(R.id.tvAddress);
        TextView tvPostcode = bottomSheetView.findViewById(R.id.tvPostcode);

        Button btnDirections = bottomSheetView.findViewById(R.id.btnDirections);
        btnDirections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



                bottomEVStationSheet.dismiss();
                LatLng markerLocation = new LatLng(
                        chargingStation.getAddressInfo().getLatitude(),
                        chargingStation.getAddressInfo().getLongitude()
                );
                openGoogleMaps(userLocation,markerLocation);

            }
        });


        Button btnRateAndReview = bottomSheetView.findViewById(R.id.btnRateAndReview);
        btnRateAndReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomEVStationSheet.dismiss();
                showRateAndReviewSheet(chargingStation.getId()+"");
            }
        });



        tvEvTitle.setText(chargingStation.getAddressInfo().getTitle());
        tvAddress.setText("Address: " +  chargingStation.getAddressInfo().getAddressLine1() +
                ", " + chargingStation.getAddressInfo().getTown());
        tvPostcode.setText("Postcode: " + chargingStation.getAddressInfo().getPostcode());
        tvPower.setText("PowerKW: " + chargingStation.getConnections().get(0).getPowerKW());
        tvConType.setText("ConnectionType: " + chargingStation.getConnections().get(0).getConnectionType().getTitle());


        bottomEVStationSheet.setContentView(bottomSheetView);
        bottomEVStationSheet.show();

    }
    ///////////////////////////////////////
    /////Fuel Station Bottom Sheet/////////
    ///////////////////////////////////////
    private void openBottomSheet(FuelStation station){


        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_fuel_station_sheet, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);

        RatingBar rating_bar = bottomSheetView.findViewById(R.id.rating_bar);
        rating_bar.setRating(totalRating);
        TextView tvViewAllRatings = bottomSheetView.findViewById(R.id.tvViewAllRatings);
        tvViewAllRatings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,AllRatingsActivity.class);
                intent.putExtra("STATION_ID", station.getSiteId());
                startActivity(intent);
            }
        });
        TextView tvBrand = bottomSheetView.findViewById(R.id.tvBrand);
        TextView tvAddress = bottomSheetView.findViewById(R.id.tvAddress);
        TextView tvPostcode = bottomSheetView.findViewById(R.id.tvPostcode);
        TextView tvPetrolPrice = bottomSheetView.findViewById(R.id.tvPetrolPrice);
        TextView tvDieselPrice = bottomSheetView.findViewById(R.id.tvDieselPrice);
        Button btnDirections = bottomSheetView.findViewById(R.id.btnDirections);
        btnDirections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bottomSheetDialog.dismiss();
                LatLng markerLocation = new LatLng(
                        station.getLocation().getLatitude(),
                        station.getLocation().getLongitude()
                );
                openGoogleMaps(userLocation,markerLocation);

            }
        });


        Button btnRateAndReview = bottomSheetView.findViewById(R.id.btnRateAndReview);
        btnRateAndReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.dismiss();
                showRateAndReviewSheet(station.getSiteId());
            }
        });



        tvBrand.setText("Brand: " + station.getBrand());
        tvAddress.setText("Address: " + station.getAddress());
        tvPostcode.setText("Postcode: " + station.getPostcode());
        tvPetrolPrice.setText("Petrol Price: £" + station.getPrices().getE10());
        tvDieselPrice.setText("Diesel Price: £" + station.getPrices().getB7() );


        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();

    }



    private void openGoogleMaps(LatLng origin, LatLng destination) {
        String uri = "https://www.google.com/maps/dir/?api=1" +
                "&origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&travelmode=driving";





        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable location
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    getDeviceLocation();
                }
            }
        }
    }






    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getDeviceLocation();
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
    }


    public void onReviewPosted(boolean postedSuccess, String error, String stationID) {


        loading_pb.setVisibility(View.GONE);
        if (postedSuccess) {
            Toast.makeText(MainActivity.this, getString(R.string.review_submitted_successfully), Toast.LENGTH_SHORT).show();


        } else {
            Toast.makeText(MainActivity.this,error.toString(),Toast.LENGTH_SHORT).show();
        }


    }

    Float totalRating = 0.0f;

    public void allDataFetched(boolean allDataFetched, String error, String restaurantKey,String stationType,
                               FuelStation fuelStation,ChargingStation chargingStation) {
        loading_pb.setVisibility(View.GONE);
        if(allDataFetched){
            Float localRating = 0.0f;
            for(FuelFirebaseModel model : FireStoreDB.allFuelFirebaseList){
                localRating  = localRating + model.getRating();
            }

            totalRating = localRating/FireStoreDB.allFuelFirebaseList.size();


            if(stationType.equals("ev")){
                openEVStationSheet(chargingStation);
            }else
            if(stationType.equals("fuel")){
                openBottomSheet(fuelStation);
            }
            //updateRestaurantRating(restaurantKey,totalRating/FireStoreDB.allFuelFirebaseList.size());
        }else{
            Toast.makeText(MainActivity.this,error.toString(),Toast.LENGTH_SHORT).show();
            totalRating = 0.0f;
        }


    }
}