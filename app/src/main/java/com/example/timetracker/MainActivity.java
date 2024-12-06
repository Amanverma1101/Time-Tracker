package com.example.timetracker;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private FirebaseDatabase mDatabase;
    private TextView box1TextView, box2TextView, box3TextView, box4TextView, box5TextView;
    private TextView box6TextView, box7TextView, box8TextView, box9TextView, box10TextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // Ensure the layout XML is set up correctly

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance();

        // Initialize UI elements (TextViews for each box)
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
    }

    // Method to fetch data from Firebase and update the UI
    private void fetchData(String boxId, TextView boxTextView) {

        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String userId = "user1";

        DatabaseReference databaseReference = mDatabase.getReference("user_data")
                .child(userId)
                .child(currentDate)
                .child(boxId);

        // Listen for changes to the specified box (e.g., box1, box2, etc.)
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Retrieve BoxData object from Firebase
                BoxData boxData = dataSnapshot.getValue(BoxData.class);

                if (boxData != null) {
                    // Update the TextView with the data from Firebase
                    String displayText = "Data: " + boxData.getData() + "\nOption: " + boxData.getSelectedOption();
                    boxTextView.setText(displayText);
                } else {
                    // Set default value if data is null (e.g., first time or empty)
                    boxTextView.setText("No data available");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
