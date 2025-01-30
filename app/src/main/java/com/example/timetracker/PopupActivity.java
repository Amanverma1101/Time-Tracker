package com.example.timetracker;


import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class PopupActivity extends AppCompatActivity {
    private FirebaseDatabase mDatabase;
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private ArrayList<String> valuesList = new ArrayList<>();
    private static final String TAG = "PopupActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("PopupActivityDebug", "PopupActivity started");
        setContentView(R.layout.dialog_layout);

        mDatabase = FirebaseDatabase.getInstance();
        // Get the box position passed to the popup
        int boxPosition = getIntent().getIntExtra("BOX_POSITION", -1);
        Log.d("PopupActivityDebug", "BOX_POSITION: " + boxPosition);
        // Example string array
//        String[] values = new String[] {"YouTube", "Instagram", "Meditation", "Food", "Gym", "Reading", "Running"};
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        String userId = "user1";
        DatabaseReference labelsRef = firebaseDatabase.getReference("user_data").child(userId).child("labels");

        // Get the NumberPicker reference
        NumberPicker numberPicker = findViewById(R.id.number_picker);
        setupNumberPicker(numberPicker, labelsRef);
        // Initialize EditText
        EditText inputField = findViewById(R.id.input_field);

        // Initialize Submit Button
        Button submitButton = findViewById(R.id.submit_button);
        submitButton.setOnClickListener(v -> {
            String inputValue = inputField.getText().toString();
            int selectedNumber = numberPicker.getValue();
            String selectedOption = String.valueOf(selectedNumber);
            long timestamp = System.currentTimeMillis();
//            Log.d("PopupActivityDebug", "selectedNumber: " + selectedNumber);
//            Log.d("PopupActivityDebug", "inputValue: " + inputValue);
            Log.d("PopupActivityDebug", "inputValue: " + selectedOption);
            if (inputValue.isEmpty()) {
                Toast.makeText(this, "Input cannot be empty!", Toast.LENGTH_SHORT).show();
            } else {
                // Broadcast the input and number back to the widget
//                Log.d("PopupActivityDebug", "selectedNumber1: " + selectedNumber);
//                Log.d("PopupActivityDebug", "inputValue1: " + inputValue);
                Intent updateIntent = new Intent("com.example.timetracker.UPDATE_BOX");
                updateIntent.setComponent(new ComponentName(getApplicationContext(), TimeTrackerWidget.class));
                updateIntent.putExtra("BOX_POSITION", boxPosition);
                updateIntent.putExtra("INPUT_OPTION", valuesList.get(selectedNumber) );
                updateIntent.putExtra("INPUT_VALUE", inputValue);
                getApplicationContext().sendBroadcast(updateIntent);
                saveDataToFirebase(inputValue, selectedNumber, boxPosition, timestamp);
                finish();
            }
        });

    }

    private void setupNumberPicker(NumberPicker numberPicker, DatabaseReference labelsRef) {


        labelsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    GenericTypeIndicator<Map<String, String>> t = new GenericTypeIndicator<Map<String, String>>() {
                    };
                    Map<String, String> labelData = snapshot.getValue(t);
                    // Retrieve the text part of the label
                    if (labelData != null) {
                        String label = labelData.get("text");
                        if (label != null) {
                            if (!valuesList.contains(label)) {
                                valuesList.add(label);
                            }
                        }
                    }
                }
                String[] values = valuesList.toArray(new String[0]);
                numberPicker.setMinValue(0);
                numberPicker.setMaxValue(values.length - 1);
                numberPicker.setDisplayedValues(null);  // Clear old values
                numberPicker.setDisplayedValues(values);
                numberPicker.setWrapSelectorWheel(true);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("PopupActivityDebug", "loadLabels:onCancelled", databaseError.toException());
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        int boxPosition = getIntent().getIntExtra("BOX_POSITION", -1);
        if (boxPosition != -1) {
//            TimeTrackerWidget.timerRunning[boxPosition - 1] = false;
            Log.d("PopupActivity", "Reset timerRunning for Box: " + boxPosition);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart called");
    }

    private void sendInputToWidget(String inputText) {
        // Prepare an Intent with the action to update the widget
        Intent intent = new Intent(this, TimeTrackerWidget.class);
        intent.setAction(TimeTrackerWidget.ACTION_UPDATE_TEXTVIEW);

        // Put the input text as an extra in the intent
        intent.putExtra(TimeTrackerWidget.EXTRA_INPUT_TEXT, inputText);

        // Get all widget IDs for this app
        int[] widgetIds = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), TimeTrackerWidget.class));
        for (int widgetId : widgetIds) {
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            sendBroadcast(intent); // Send a broadcast for each widget ID
        }
    }


    private void saveDataToFirebase(String inputValue, int selectedOption, int boxPosition, long timestamp) {
        // Log data before pushing to Firebase
        Log.d("FirebaseDebug", "Saving data: " + inputValue + ", " + selectedOption + ", " + timestamp);
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        // Default user ID (replace with Firebase user ID if needed)
        String userId = "user1";
        if (mDatabase == null) {
            Log.e("PopupActivityDebug", "FirebaseDatabase instance is null!");
            return;
        }
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("user_data").child(userId);
        BoxData boxData = new BoxData(inputValue, valuesList.get(selectedOption), timestamp);
        databaseReference.child(currentDate).child("box" + boxPosition).setValue(boxData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseDebug", "Data saved successfully!");
                        Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("FirebaseDebug", "Error saving data", task.getException());
                        Toast.makeText(this, "Error saving data", Toast.LENGTH_SHORT).show();
                    }
                });
    }


}
