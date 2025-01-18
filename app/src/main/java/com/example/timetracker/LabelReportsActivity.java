package com.example.timetracker;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LabelReportsActivity extends AppCompatActivity {
    private Spinner labelSpinner;
    private ImageView labelImage;
    private LinearLayout fullDataLayout, dataLayout;
    private HashMap<String, Integer> labelImages; // Map label names to drawable IDs
    private DatabaseReference databaseReference;
    private final List<String> labels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label_reports);

        labelSpinner = findViewById(R.id.label_spinner);
        labelImage = findViewById(R.id.label_image);
        fullDataLayout = findViewById(R.id.full_data_container);
        dataLayout = findViewById(R.id.data_container);

        setupLabelImages();
        setupFirebase();
        setupSpinner();
    }

    private void setupLabelImages() {
        labelImages = new HashMap<>();
        labelImages.put("YouTube", R.drawable.yt);
        labelImages.put("Instagram", R.drawable.insta);
        labelImages.put("Meditation", R.drawable.med);
        labelImages.put("Food", R.drawable.food);
        labelImages.put("Gym", R.drawable.gym);
        labelImages.put("Reading", R.drawable.read);
        labelImages.put("Running", R.drawable.run);
        // Add other labels and their corresponding images
    }

    private void setupFirebase() {
        // Initialize your Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("user_data").child("user1").child("labels");
    }

    private void setupSpinner() {

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        labelSpinner.setAdapter(adapter);

        // Fetch labels from Firebase
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                labels.clear();
                labels.addAll(Arrays.asList("YouTube", "Instagram", "Meditation", "Food", "Gym", "Reading", "Running"));
                Log.d("LabelReportsActivity", "Snapshot: " + dataSnapshot.toString());  // Log the entire snapshot
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    GenericTypeIndicator<Map<String, String>> t = new GenericTypeIndicator<Map<String, String>>() {};
                    Map<String, String> labelData = snapshot.getValue(t);
                    // Retrieve the text part of the label
                    if (labelData != null) {
                        String label = labelData.get("text");
                        if (label != null) {
                            labels.add(label);
                        } else {
                            Log.d("LabelReportsActivity", "Label is null for snapshot: " + snapshot.getKey());
                        }
                    }
                }
                adapter.notifyDataSetChanged(); // Refresh spinner with new data
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors
                Log.e("LabelReportsActivity", "Error fetching labels", databaseError.toException());
            }
        });

        labelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLabel = (String) parent.getItemAtPosition(position);
                updateUIForLabel(selectedLabel);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Optionally handle the case where nothing is selected
            }
        });
    }

    private void updateUIForLabel(String label) {
        // Update the image and data for the selected label
        int imageRes = labelImages.getOrDefault(label, 0);
        if(imageRes==0){
            imageRes = R.drawable.def;
        }
        labelImage.setImageResource(imageRes);
        updateDataText(label);
    }

    private void updateDataText(String label) {
        DatabaseReference userRef =  FirebaseDatabase.getInstance().getReference("user_data").child("user1");
        int labelIndex = getLabelIndex(label); // Assuming you have a method to convert label to an index.

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                fullDataLayout.removeAllViews(); // Clear previous entries for new label selection
                Log.d("onDataChange", "onDataChange: Entered the func");
                boolean checkEmpty = true;
                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    String date = dateSnapshot.getKey(); // The key here is expected to be the date
                    Log.d("onDataChange", "onDataChange0: "+ date);
                    if (date.length() == 10) {
                        boolean entriesAdded = false;
                        Log.d("onDataChange", "onDataChange1: Entered the func");
                        // Create a new LinearLayout for each date
                        LinearLayout dateBlock = new LinearLayout(LabelReportsActivity.this);
                        dateBlock.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        dateBlock.setOrientation(LinearLayout.VERTICAL);
                        dateBlock.setPadding(8, 8, 8, 8);
                        dateBlock.setBackground(ContextCompat.getDrawable(LabelReportsActivity.this, android.R.drawable.dialog_holo_light_frame));

                        // Create a TextView for the date header
                        TextView dateTextView = new TextView(LabelReportsActivity.this);
                        dateTextView.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        dateTextView.setBackgroundColor(Color.parseColor("#CCCCCC"));
                        dateTextView.setTextAppearance(android.R.style.TextAppearance_Medium);
                        dateTextView.setGravity(Gravity.CENTER);
                        dateTextView.setPadding(8, 8, 8, 8);
                        dateTextView.setText(date); // Set text to the date
                        dateBlock.addView(dateTextView);
                        Log.d("onDataChange", "onDataChange2: "+date);
                        // Process each entry for the given date
                        for (DataSnapshot entrySnapshot : dateSnapshot.getChildren()) {
                            Log.d("onDataChange", "onDataChange3: "+entrySnapshot.toString());
                            String selectedOption = entrySnapshot.child("selectedOption").getValue(String.class);
                            int option; // Declare the variable option.

                            try {
                                // Attempt to parse the selectedOption string to an int.
                                option = Integer.parseInt(selectedOption);
                            } catch (NumberFormatException e) {
                                // If parsing fails, set option to 0.
                                option = -1;
                            }
                            //Check if selectedOption is not null and equals labelIndex
                            if (option ==labelIndex) {
                                String time = entrySnapshot.getKey(); // Assuming the key is the time
                                String data = entrySnapshot.child("data").getValue(String.class);

                                LinearLayout timeDataBlock = new LinearLayout(LabelReportsActivity.this);
                                timeDataBlock.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                timeDataBlock.setOrientation(LinearLayout.HORIZONTAL);
                                timeDataBlock.setBackgroundColor(Color.parseColor("#FFFFFF"));
                                timeDataBlock.setPadding(8, 8, 8, 8);

                                TextView timeTextView = new TextView(LabelReportsActivity.this);
                                timeTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                                timeTextView.setTextAppearance(android.R.style.TextAppearance_Medium);
                                timeTextView.setText(time);

                                TextView dataTextView = new TextView(LabelReportsActivity.this);
                                dataTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
                                dataTextView.setTextAppearance(android.R.style.TextAppearance_Medium);
                                dataTextView.setText(data);

                                timeDataBlock.addView(timeTextView);
                                timeDataBlock.addView(dataTextView);
                                dateBlock.addView(timeDataBlock);

                                entriesAdded = true;
                            }
                        }

                        // Only add the date block if there are entries
                        if (entriesAdded) {
                            checkEmpty = false;
                            fullDataLayout.addView(dateBlock);
                        }
                    }
                }
                if(checkEmpty){
                    Toast.makeText(LabelReportsActivity.this,"Entries not found for this Label!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("updateDataText", "Error fetching data", databaseError.toException());
            }
        });
    }




    private int getLabelIndex(String label) {
        // You need to implement this based on your application's logic
        return labels.indexOf(label);
    }

}
