package com.example.timetracker;


import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PopupActivity extends AppCompatActivity {
    private FirebaseDatabase mDatabase;
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private ArrayList<String> valuesList = new ArrayList<>();
    public static ArrayList<String> emojiList = new ArrayList<>();
    private static final String TAG = "PopupActivity";

    static class EmojiData {
        String emoji;
        String text;

        public EmojiData(String emoji, String text) {
            this.emoji = emoji;
            this.text = text;
        }
    }

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

        // Get the NumberPicker reference
        NumberPicker numberPicker = findViewById(R.id.number_picker);
        setupNumberPicker(numberPicker);
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
                updateIntent.putExtra("INPUT_EMOJI", emojiList.get(selectedNumber));
                updateIntent.putExtra("INPUT_VALUE", inputValue);
                getApplicationContext().sendBroadcast(updateIntent);

//                Intent refreshIntent = new Intent(getApplicationContext(), TimeTrackerWidget.class);
//                refreshIntent.setAction(TimeTrackerWidget.ACTION_REFRESH_WIDGET);
//                getApplicationContext().sendBroadcast(refreshIntent);

                saveDataToFirebase(inputValue, selectedNumber, boxPosition, timestamp);
                finish();
            }
        });

    }
    public int getCurrentTimeSegment() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 0 && hour < 6) return 1;   // 00:00 - 06:00
        if (hour >= 6 && hour < 12) return 2;  // 06:00 - 12:00
        if (hour >= 12 && hour < 18) return 3; // 12:00 - 18:00
        return 4;                              // 18:00 - 00:00
    }

    private void setupNumberPicker(NumberPicker numberPicker) {
        // **Fetch Labels from Local Storage Instead of Firebase**
        List<EmojiData> localLabels = getEmojiList();

        if (localLabels != null && !localLabels.isEmpty()) {
            valuesList.clear(); // Clear existing list
            emojiList.clear();
            for (EmojiData label : localLabels) {
                valuesList.add(label.text); // Store only label text
                emojiList.add(label.emoji);
            }
        }

        // Convert list to array and set values in NumberPicker
        String[] values = valuesList.toArray(new String[0]);
        if (values.length > 0) {
            numberPicker.setMinValue(0);
            numberPicker.setMaxValue(values.length - 1);
            numberPicker.setDisplayedValues(null);  // Clear old values
            numberPicker.setDisplayedValues(values);
            numberPicker.setWrapSelectorWheel(true);
        } else {
            Toast.makeText(this, "No labels found in local storage", Toast.LENGTH_SHORT).show();
        }
    }

    private List<EmojiData> getEmojiList() {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("emoji_list", null);
        Type type = new TypeToken<List<EmojiData>>() {}.getType();
        return json != null ? gson.fromJson(json, type) : new ArrayList<>();
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
//        String userId = "user1";
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);
        if (mDatabase == null) {
            Log.e("PopupActivityDebug", "FirebaseDatabase instance is null!");
            return;
        }
        int currentSegment = getCurrentTimeSegment();
        if(userId!=null) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("user_data").child(userId);
            BoxData boxData = new BoxData(inputValue, valuesList.get(selectedOption), timestamp);
            databaseReference.child(currentDate).child("box" + ((currentSegment - 1) * 36 + boxPosition)).setValue(boxData)
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




}
