package com.example.timetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private FirebaseDatabase mDatabase;
    private TextView box1TextView, box2TextView, box3TextView, box4TextView, box5TextView;
    private TextView box6TextView, box7TextView, box8TextView, box9TextView, box10TextView;
    private TextView tvCurrentDate;
    private Button btnShowReports; // Button to view reports

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance();

        // Initialize UI elements (TextViews for each box and the button)
        box1TextView = findViewById(R.id.box_text_1);
        box2TextView = findViewById(R.id.box_text_2);
        box3TextView = findViewById(R.id.box_text_3);
        box4TextView = findViewById(R.id.box_text_4);
        box5TextView = findViewById(R.id.box_text_5);
        box6TextView = findViewById(R.id.box_text_6);
        box7TextView = findViewById(R.id.box_text_7);
        box8TextView = findViewById(R.id.box_text_8);
        box9TextView = findViewById(R.id.box_text_9);
        box10TextView = findViewById(R.id.box_text_10);
        tvCurrentDate = findViewById(R.id.tv_current_date);
        btnShowReports = findViewById(R.id.btn_show_reports); // Initialize the button

        // Set the current date
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        tvCurrentDate.setText(currentDate);

        // Fetch data from Firebase for each box
        fetchData("box1", box1TextView);
        fetchData("box2", box2TextView);
        fetchData("box3", box3TextView);
        fetchData("box4", box4TextView);
        fetchData("box5", box5TextView);
        fetchData("box6", box6TextView);
        fetchData("box7", box7TextView);
        fetchData("box8", box8TextView);
        fetchData("box9", box9TextView);
        fetchData("box10", box10TextView);

        // Set up the button click listener to open ReportActivity
        btnShowReports.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportActivity.class);
            startActivity(intent);
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Navigation icon click listener
        toolbar.setNavigationOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, view);
            popup.getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                // Handle item clicks here
                return false;
//                switch (item.getItemId()) {
//                    case R.id.action_settings:
//                        // Action for "Settings" menu item
//                        return true;
//                    default:
//                        return false;
//                }
            });
            popup.show();
        });
    }

    // Method to fetch data from Firebase and update the UI
    private void fetchData(String boxId, TextView boxTextView) {
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String userId = "user1";

        DatabaseReference databaseReference = mDatabase.getReference("user_data")
                .child(userId)
                .child(currentDate)
                .child(boxId);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                BoxData boxData = dataSnapshot.getValue(BoxData.class);

                if (boxData != null) {
                    String displayText = "Data: " + boxData.getData() + "\nOption: " + boxData.getSelectedOption();
                    boxTextView.setText(displayText);
                } else {
                    boxTextView.setText("😔 No Data Available");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // Start the SettingsActivity when the "Settings" item is clicked
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
