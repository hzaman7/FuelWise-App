package com.example.fuelwise;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fuelwise.adapters.AllRatingAdapter;
import com.example.fuelwise.firebaseCalls.FireStoreDB;
import com.example.fuelwise.models.FuelFirebaseModel;

import java.util.ArrayList;

public class AllRatingsActivity extends AppCompatActivity {


    FireStoreDB fireStoreDB;
    View parentLayout;
    ArrayList<FuelFirebaseModel> eventsModelArrayList;
    RecyclerView allRatingRV;
    AllRatingAdapter allRatingAdapter;
    ProgressBar loading_pb;
    String STATION_ID="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_ratings);

        getWindow().setStatusBarColor(getResources().getColor(R.color.app_sec_color));

        fireStoreDB = new FireStoreDB();

        STATION_ID = getIntent().getStringExtra("STATION_ID");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true); // Show the back button
            getSupportActionBar().setTitle("All Reviews");
        }

        allRatingRV = findViewById(R.id.allRatingRV);
        allRatingRV.setHasFixedSize(true);
        allRatingRV.setLayoutManager(new LinearLayoutManager(AllRatingsActivity.this));

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(allRatingRV.getContext(),
                new LinearLayoutManager(AllRatingsActivity.this).getOrientation());
        allRatingRV.addItemDecoration(dividerItemDecoration);


        parentLayout = findViewById(android.R.id.content);
        eventsModelArrayList = new ArrayList<>();
        loading_pb = findViewById(R.id.loading_pb);

        fireStoreDB.getAllStationReviews(AllRatingsActivity.this,AllRatingsActivity.this,
                STATION_ID);

    }

    public void allRatingsFetched(boolean isSuccess, String error) {

        loading_pb.setVisibility(View.GONE);
        if (isSuccess) {

            allRatingAdapter = new AllRatingAdapter(AllRatingsActivity.this,
                    AllRatingsActivity.this,FireStoreDB.allStationRatingsList);

            allRatingRV.setAdapter(allRatingAdapter);
            allRatingAdapter.notifyDataSetChanged();

            allRatingRV.setVisibility(View.VISIBLE);

        } else {
            Toast.makeText(AllRatingsActivity.this,error.toString(),Toast.LENGTH_SHORT).show();
        }




    }


}
