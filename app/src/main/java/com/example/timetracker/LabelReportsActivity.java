package com.example.timetracker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

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
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
    private BottomNavigationView bottomNavigationView;

    private TextView emojiView;
    private final List<String> labels = new ArrayList<>();
    private final List<String> emojis = new ArrayList<>();
    private AlertDialog signInDialog;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> signInLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label_reports);

        // Ensure that BottomNavigationView is initialized after setContentView
        bottomNavigationView = findViewById(R.id.bottom_navigation); // Initialize BottomNavigationView here

        // Check if the user is signed in, if not, show the sign-in dialog
        if (!isUserSignedIn()) {
            showSignInDialog();
        } else {
            setupUI();
            labelSpinner = findViewById(R.id.label_spinner);
            labelImage = findViewById(R.id.label_image);
            fullDataLayout = findViewById(R.id.full_data_container);
            dataLayout = findViewById(R.id.data_container);
            emojiView = findViewById(R.id.emoji_view);

            setupLabelImages();
            setupFirebase();
            setupSpinner();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish(); // Close the activity
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });
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
            Toast.makeText(LabelReportsActivity.this, "Signed in successfully", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(LabelReportsActivity.this, "Default labels added successfully!", Toast.LENGTH_SHORT).show();
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



    private void setupUI() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); // Smooth transition
                return true;
            } else if (id == R.id.btn_show_reports) {
                startActivity(new Intent(this, ReportActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else return id == R.id.show_label_report;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.show_label_report);
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
        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null); // Use default if user is not signed in

        // Initialize Firebase reference dynamically using the userId
        if (userId != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("user_data").child(userId).child("labels");
        }
    }

    private void setupSpinner() {

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        labelSpinner.setAdapter(adapter);

        // Fetch labels from Firebase
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                labels.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    GenericTypeIndicator<Map<String, String>> t = new GenericTypeIndicator<Map<String, String>>() {};
                    Map<String, String> labelData = snapshot.getValue(t);
                    // Retrieve the text part of the label
                    if (labelData != null) {
                        String label = labelData.get("text");
                        String emoji_icon = labelData.get("emoji");
                        if(emoji_icon != null){
                            emojis.add(emoji_icon);
                        }
                        if (label != null) {
                            labels.add(label);
                        }
                    }
                }
                adapter.notifyDataSetChanged(); // Refresh spinner with new data
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
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

    private int convertDpToPixels(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void updateUIForLabel(String label) {
        // Update the image and data for the selected label
        int imageRes = labelImages.getOrDefault(label, 0);
        if (imageRes == 0) {
            // Set emojiView visible and update its properties
            emojiView.setVisibility(View.VISIBLE);
            int idx = labels.indexOf(label);
            if (idx != -1 && idx >= 0) {
                emojiView.setText(emojis.get(idx));
            } else {
                emojiView.setText("🔃");
            }
            LinearLayout.LayoutParams emojiParams = new LinearLayout.LayoutParams(
                    (int) (150 * getResources().getDisplayMetrics().density), // converts 150dp to pixels
                    (int) (150 * getResources().getDisplayMetrics().density)  );// converts 150dp to pixels
            emojiParams.gravity = Gravity.CENTER_HORIZONTAL; // Set the gravity specifically for the layout params

            emojiView.setLayoutParams(emojiParams);
            emojiView.setGravity(Gravity.CENTER);
            emojiView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 60); // Set emoji size
            emojiView.setBackground(ContextCompat.getDrawable(this, R.drawable.circular_background));

            // Clear and hide labelImage
            labelImage.setVisibility(View.GONE);
            labelImage.setImageDrawable(null); // Optional: Clear the current image resource
        } else {
            // Set labelImage visible and update its properties
            labelImage.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (150 * getResources().getDisplayMetrics().density), // converts 150dp to pixels
                    (int) (150 * getResources().getDisplayMetrics().density)  // converts 150dp to pixels
            );
            params.gravity = Gravity.CENTER_HORIZONTAL;
            labelImage.setLayoutParams(params);
            labelImage.setImageResource(imageRes);
            labelImage.setBackgroundResource(R.drawable.circular_background);

            // Clear and hide emojiView
            emojiView.setVisibility(View.GONE);
            emojiView.setText(""); // Clear the text
        }

        updateDataText(label);
    }

    private void updateDataText(String label) {
        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null); // Use default if user is not signed in

        // Update Firebase reference to dynamically use the userId
        if(userId != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user_data").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    fullDataLayout.removeAllViews(); // Clear previous entries for new label selection
                    Log.d("onDataChange", "onDataChange: Entered the func");
                    boolean checkEmpty = true;
                    for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                        String date = dateSnapshot.getKey(); // The key here is expected to be the date
                        Log.d("onDataChange", "onDataChange0: " + date);
                        assert date != null;
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
                            Log.d("onDataChange", "onDataChange2: " + date);
                            // Process each entry for the given date
                            for (DataSnapshot entrySnapshot : dateSnapshot.getChildren()) {
                                Log.d("onDataChange", "onDataChange3: " + entrySnapshot.toString());
                                String selectedOption = entrySnapshot.child("selectedOption").getValue(String.class);

                                //Check if selectedOption is not null and equals labelIndex
                                assert selectedOption != null;
                                if (selectedOption.equals(label)) {
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
                    if (checkEmpty) {
                        Toast.makeText(LabelReportsActivity.this, "Entries not found for this Label!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("updateDataText", "Error fetching data", databaseError.toException());
                }
            });
        }
    }

}
