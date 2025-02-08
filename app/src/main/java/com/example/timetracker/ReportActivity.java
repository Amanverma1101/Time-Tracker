package com.example.timetracker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private FirebaseDatabase mDatabase;
    private BottomNavigationView bottomNavigationView;

    private TextView dateDisplay;
    private ImageButton leftArrow, rightArrow;
    private Calendar currentDate = Calendar.getInstance();
    private AlertDialog signInDialog;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> signInLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_activity);
        if (!isUserSignedIn()) {
            showSignInDialog();
        }
        setupUI();
        mDatabase = FirebaseDatabase.getInstance();
        currentDate = Calendar.getInstance();
        updateDateDisplay();
        updateChartData();
        updateBarChartData();

    }

    private boolean isUserSignedIn() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);  // Retrieve the userId
        return userId != null;
    }

    // Show the sign-in dialog if the user is not signed in
    private void showSignInDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_sign_in, null);
        builder.setView(dialogView);

        signInDialog = builder.create(); // Create and store the dialog in the signInDialog field

        ImageView googleSignInButton = dialogView.findViewById(R.id.img_google_sign_in);
        googleSignInButton.setOnClickListener(v -> {
            // Trigger Google Sign-In
            promptSignIn();
        });

        Button remindLaterButton = dialogView.findViewById(R.id.btn_remind_later);
        remindLaterButton.setOnClickListener(v -> {
            // Dismiss the dialog, remind later logic here
            signInDialog.dismiss();  // Correctly dismiss the dialog using the field
        });

        signInDialog.show(); // Show the dialog stored in the field
    }

    // Prompt the user to sign in
    private void promptSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Handle the result of Google sign-in
        if (requestCode == 1) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Get user ID and save it in SharedPreferences
            String userId = account.getId();
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("userId", userId);  // Save the user ID
            editor.apply();

            // After saving the userId, check if the user has default labels
            if(userId != null) {
                DatabaseReference labelsRef = FirebaseDatabase.getInstance().getReference("user_data").child(userId).child("labels");
                DatabaseReference counterRef = FirebaseDatabase.getInstance().getReference("user_data").child(userId).child("labelCounter");

                // Check if the labels node is empty
                labelsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                            // If no labels exist, set the default labels
                            setDefaultLabels(userId, counterRef);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("FirebaseError", "Error checking for labels", databaseError.toException());
                    }
                });
            }

            // Dismiss the sign-in dialog if it's showing
            if (signInDialog != null && signInDialog.isShowing()) {
                signInDialog.dismiss();
            }

            // Notify user of successful sign-in
            Toast.makeText(ReportActivity.this, "Signed in successfully", Toast.LENGTH_SHORT).show();
        } catch (ApiException e) {
            Log.w("SignInFailure", "signInResult:failed code=" + e.getStatusCode());
            // Handle sign-in failure
        }
    }

    // Method to set default labels for the user
    private void setDefaultLabels(String userId, DatabaseReference counterRef) {
        // Default labels data
        List<Map<String, String>> defaultLabels = new ArrayList<>();
        defaultLabels.add(createLabel("Y", "Youtube"));
        defaultLabels.add(createLabel("🎥", "Instagram"));
        defaultLabels.add(createLabel("🧘‍♂️", "Meditation"));
        defaultLabels.add(createLabel("🏋️‍♂️", "Gym"));
        defaultLabels.add(createLabel("🏃", "Running"));
        defaultLabels.add(createLabel("🍜", "Food"));
        defaultLabels.add(createLabel("📚", "Reading"));

        // Insert default labels into Firebase
        DatabaseReference labelsRef = FirebaseDatabase.getInstance().getReference("user_data").child(userId).child("labels");

        // Read the current counter to use as the label key
        counterRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Integer count = dataSnapshot.getValue(Integer.class);
                if (count == null) count = 0;  // Default to 0 if not found

                // Insert labels with the current count as the index
                for (Map<String, String> label : defaultLabels) {
                    String labelKey = String.valueOf(count); // Use the current count as the label key
                    labelsRef.child(labelKey).setValue(label);
                    count++;  // Increment the counter for the next label
                }

                // Update the labelCounter to the new value after adding all default labels
                counterRef.setValue(count);

                // Optionally show a Toast message or log it
                Toast.makeText(ReportActivity.this, "Default labels added successfully!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error updating labelCounter", databaseError.toException());
            }
        });
    }

    // Helper method to create a label map with emoji and text
    private Map<String, String> createLabel(String emoji, String text) {
        Map<String, String> label = new HashMap<>();
        label.put("emoji", emoji);
        label.put("text", text);
        return label;
    }


    private void updateBarChartData() {
        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null); // Use default if user is not signed in

        // Get the formatted date for the current date
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        String formattedDate = sdf.format(currentDate.getTime());
        if(userId!=null) {
            // Update Firebase reference to dynamically use the userId
            DatabaseReference ref = mDatabase.getReference("user_data/" + userId + "/" + formattedDate);

            Map<String, Integer> fetchedData = new HashMap<>();

            // Show "no data" image initially
            ImageView noDataImage = findViewById(R.id.no_data_image);
            noDataImage.setVisibility(View.GONE); // Hide it initially

            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Check if data is available
                    if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                        // No data available
                        noDataImage.setVisibility(View.VISIBLE); // Show the "no data" image
                        // Hide the charts
                        BarChart chart = findViewById(R.id.barChart);
                        chart.setVisibility(View.GONE);
                        FrameLayout pieChartContainer = findViewById(R.id.chart_container);
                        pieChartContainer.setVisibility(View.GONE);
                    } else {
                        // Data is available, proceed to display the chart
                        noDataImage.setVisibility(View.GONE); // Hide the "no data" image
                        // Show the charts
                        BarChart chart = findViewById(R.id.barChart);
                        chart.setVisibility(View.VISIBLE);
                        FrameLayout pieChartContainer = findViewById(R.id.chart_container);
                        pieChartContainer.setVisibility(View.VISIBLE);

                        // Process the data and display the bar chart
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String activity = snapshot.child("selectedOption").getValue(String.class);
                            Integer count = fetchedData.getOrDefault(activity, 0);
                            if(count!=null) {
                                fetchedData.put(activity, count + 1);
                            }
                        }
                        displayBarChart(fetchedData); // Display bar chart with the fetched data
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(ReportActivity.this, "Failed to load data for the bar chart", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void displayBarChart(Map<String, Integer> data) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int index = 0;

        // Fetch the color array from resources
        @SuppressLint("Recycle") TypedArray colorsArray = getResources().obtainTypedArray(R.array.pie_chart_colors);
        int colorCount = colorsArray.length();

        // Add data entries to the chart
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            Log.d("Bar Chart Entry", "Activity: " + entry.getKey() + ", Count: " + entry.getValue());

            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;

            if (index == colorCount) {
                index = 0;
            }
        }

        if (entries.isEmpty()) {
            Log.d("Bar Chart", "No entries to display");
        } else {
            Log.d("Bar Chart", "Entries count: " + entries.size());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Activity Data");

        ArrayList<Integer> colors = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            colors.add(colorsArray.getColor(i % colorCount, Color.BLACK));
        }

        dataSet.setColors(colors);
        dataSet.setValueTextSize(18f);
        dataSet.setDrawValues(true);

        BarData barData = new BarData(dataSet);
        BarChart chart = findViewById(R.id.barChart);
        chart.setData(barData);

        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setTextSize(12f);

        chart.getAxisLeft().setTextSize(14f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setGranularityEnabled(true);

        // Set maximum value dynamically based on data
        float maxDataValue = 0;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            if (entry.getValue() > maxDataValue) {
                maxDataValue = entry.getValue();
            }
        }
        leftAxis.setAxisMaximum(maxDataValue + 1);

        chart.getAxisRight().setEnabled(false);

        chart.getBarData().setBarWidth(0.9f);

        chart.getDescription().setText("");

        chart.animateY(1500);

        chart.notifyDataSetChanged();
        chart.invalidate();
    }





    private void updateChartData() {
        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null); // Use default if user is not signed in

        // Get the formatted date for today's chart
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        String formattedDate = sdf.format(currentDate.getTime());

        if(userId!=null) {
            // Update Firebase reference to dynamically use the userId
            DatabaseReference ref = mDatabase.getReference("user_data/" + userId + "/" + formattedDate);

            // Fetch and display the data on the chart
            fetchAndDisplayPieChart(ref);
        }
    }

    private void fetchAndDisplayPieChart(DatabaseReference ref) {
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Integer> fetchedData = new HashMap<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String activity = snapshot.child("selectedOption").getValue(String.class);
                    Integer count = fetchedData.getOrDefault(activity, 0);
                    if(count!=null) {
                        fetchedData.put(activity, count + 1);
                    }
                }

                updatePieChart(fetchedData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ReportActivity.this, "Failed to load data for the chart", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePieChart(Map<String, Integer> data) {
        DonutChartView chartView = new DonutChartView(ReportActivity.this);
        chartView.setData(data);

        FrameLayout chartContainer = findViewById(R.id.chart_container);
        chartContainer.removeAllViews();
        chartContainer.addView(chartView);
    }




    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
        dateDisplay.setText(sdf.format(currentDate.getTime()));

        // Disable right arrow if today's date is shown
        rightArrow.setEnabled(!sdf.format(currentDate.getTime()).equals(sdf.format(Calendar.getInstance().getTime())));
    }
    private void setupUI() {


        dateDisplay = findViewById(R.id.date_display);
        leftArrow = findViewById(R.id.left_arrow);
        rightArrow = findViewById(R.id.right_arrow);



        leftArrow.setOnClickListener(v -> {
//            Log.d("DateNav", "Left arrow clicked");
            currentDate.add(Calendar.DAY_OF_MONTH, -1);
            updateDateDisplay();
            updateChartData();
            updateBarChartData();
//            fetchedData.clear();
        });

        rightArrow.setOnClickListener(v -> {
//            Log.d("DateNav", "Right arrow clicked");
            currentDate.add(Calendar.DAY_OF_MONTH, 1);
            updateDateDisplay();
            updateChartData();
            updateBarChartData();
//            fetchedData.clear();
        });

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);// Smooth transition
                return true;
            } else if (id == R.id.btn_show_reports) {
//                startActivity(new Intent(this, ReportActivity.class));
                return true;
            } else if (id == R.id.show_label_report) {
                Intent intent = new Intent(this, LabelReportsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left); // Smooth transition
                return true;
            }
            return false;
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.btn_show_reports);

    }


}
