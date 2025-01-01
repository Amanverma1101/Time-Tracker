package com.example.timetracker;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private FirebaseDatabase mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_activity);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance();

        // Add the first donut chart with static data
        addFirstDonutChart();

        // Fetch and display the second chart with data from Firebase
        fetchAndDisplaySecondDonutChart();
    }

    // Add the first donut chart with static data
    private void addFirstDonutChart() {
        // Example static data
        Map<String, Integer> staticData = new HashMap<>();
        staticData.put("Exercise", 25);
        staticData.put("Work", 35);
        staticData.put("Leisure", 20);
        staticData.put("Sleep", 20);

        // Create and set up the first donut chart view
        DonutChartView firstDonutChart = new DonutChartView(this);
        firstDonutChart.setData(staticData);

        // Add the first chart to the layout
        FrameLayout firstChartContainer = findViewById(R.id.chart_container);
        firstChartContainer.addView(firstDonutChart);
    }

    // Fetch and display the second donut chart with data from Firebase
    private void fetchAndDisplaySecondDonutChart() {
        DatabaseReference ref = mDatabase.getReference("user_data/user1/06-12-2024");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Aggregate data for the second chart
                Map<String, Integer> fetchedData = new HashMap<>();
                for (DataSnapshot boxSnapshot : dataSnapshot.getChildren()) {
                    String activity = boxSnapshot.child("data").getValue(String.class);
                    if (activity != null && !activity.isEmpty()) {
                        Integer count = fetchedData.getOrDefault(activity, 0);
                        fetchedData.put(activity, count + 1);
                    }
                }

                // Create the second donut chart dynamically
                DonutChartView secondDonutChart = new DonutChartView(ReportActivity.this);
                secondDonutChart.setData(fetchedData);

                // Add the second chart to the layout below the first chart
                FrameLayout secondChartContainer = findViewById(R.id.second_chart_container);
                secondChartContainer.removeAllViews(); // Clear existing views
                secondChartContainer.addView(secondDonutChart);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ReportActivity.this, "Failed to load data for the second chart", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
