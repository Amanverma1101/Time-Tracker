package com.example.timetracker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private FirebaseDatabase mDatabase;
    TextView[] boxTextViews = new TextView[144];
    private TextView tvCurrentDate;
    private Sheets sheetsService;
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<Intent> signInLauncher;
    private AlertDialog signInDialog;
    private GoogleSignInClient mGoogleSignInClient;
    private ToggleButton toggleDatePicker;
    private DatePicker datePicker;
    private BottomNavigationView bottomNavigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("");

        initializeFirebase();
        initializeGoogleSignIn();
        setupUI();
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        fetchAllBoxesData(currentDate);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish(); // Close the activity
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Set the home item as selected
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        if (!isUserSignedIn()) {
            showSignInDialog();
        }
        invalidateOptionsMenu();
    }
    public boolean isUserSignedIn() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        return account != null;
    }
    private void promptSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }
    private String getTimeIntervalForBox(int boxNumber) {
        int startMinutes = (boxNumber - 1) * 10; // Each box represents 10 minutes
        int startHour = startMinutes / 60;
        int startMinute = startMinutes % 60;

        int endMinutes = startMinutes + 10; // End time is 10 minutes later
        int endHour = endMinutes / 60;
        int endMinute = endMinutes % 60;

        return String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute);
    }

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
    private void signOut() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("userId"); // Remove the stored user ID
        editor.apply();

        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // Update UI after sign out completes
            Toast.makeText(MainActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();
            invalidateOptionsMenu();  // Force calling onPrepareOptionsMenu to update the menu item
        });


    }




    private void initializeFirebase() {
        mDatabase = FirebaseDatabase.getInstance();
    }

    private void initializeGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleSignInResult(task);
                    }
                });
    }


    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            setupGoogleSheetsService(account);

            // Log user details
            Log.i("SignInSuccess", "Signed in as: " + account.getEmail());
            Log.i("SignInSuccess", "Display Name: " + account.getDisplayName());
            Log.i("SignInSuccess", "Given Name: " + account.getGivenName());
            Log.i("SignInSuccess", "Family Name: " + account.getFamilyName());
            Log.i("SignInSuccess", "Photo URL: " + (account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "No photo available"));
            Log.i("SignInSuccess", "Google ID: " + account.getId());

            // Signed in successfully, show authenticated UI.
            String userId = account.getId();  // Get the unique ID from Google account
            if (userId != null) {
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);  // Store the ID in SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("userId", userId);  // Save the user ID
                editor.apply();  // Apply changes
            }

            Log.i("SignInSuccess", "Signed in as: " + account.getEmail());

            // After saving the userId, check if the user has default labels
            if(userId!=null) {
                DatabaseReference labelsRef = FirebaseDatabase.getInstance().getReference("user_data").child(userId).child("labels");
                DatabaseReference counterRef = FirebaseDatabase.getInstance().getReference("user_data").child(userId).child("labelCounter");

                // Check if the labels node is empty (for first-time users)
                labelsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                            // If no labels exist, set the default labels
                            setDefaultLabels(userId, counterRef,getApplicationContext());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("FirebaseError", "Error checking for labels", databaseError.toException());
                    }
                });
            }
            String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            fetchAllBoxesData(currentDate);
            // Dismiss the sign-in dialog if it's showing
            if (signInDialog != null && signInDialog.isShowing()) {
                signInDialog.dismiss();
            }

            // Notify user of successful sign-in
            Toast.makeText(MainActivity.this, "Signed in successfully", Toast.LENGTH_SHORT).show();

        } catch (ApiException e) {
            Log.w("SignInFailure", "signInResult:failed code=" + e.getStatusCode());
            // Handle the exception here
        }
    }

    // Method to set default labels for the user
    private void setDefaultLabels(String userId, DatabaseReference counterRef, Context context) {
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

                List<Map<String, String>> savedLabels = new ArrayList<>();

                // Insert labels with the current counter as the index
                for (Map<String, String> label : defaultLabels) {
                    String labelKey = String.valueOf(count); // Use the current count as the label key
                    labelsRef.child(labelKey).setValue(label);
                    savedLabels.add(label);  // Add to list for SharedPreferences
                    count++;  // Increment the counter for the next label
                }

                // Update the labelCounter to the new value after adding all default labels
                counterRef.setValue(count);

                // Save to SharedPreferences
                saveToSharedPreferences(savedLabels, context);

                // Optionally show a Toast message or log it
                Toast.makeText(context, "Default labels added successfully!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error updating labelCounter", databaseError.toException());
            }
        });
    }

    private void saveToSharedPreferences(List<Map<String, String>> labelList, Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Convert List<Map<String, String>> to List<EmojiData>
        List<SettingsActivity.EmojiData> emojiDataList = new ArrayList<>();
        for (Map<String, String> label : labelList) {
            String emoji = label.get("emoji");
            String text = label.get("text");
            if (emoji != null && text != null) {
                emojiDataList.add(new SettingsActivity.EmojiData(emoji, text));
            }
        }

        // Convert List<EmojiData> to JSON
        Gson gson = new Gson();
        String json = gson.toJson(emojiDataList);
        editor.putString("emoji_list", json);
        editor.apply();
    }


    // Helper method to create a label map with emoji and text
    private Map<String, String> createLabel(String emoji, String text) {
        Map<String, String> label = new HashMap<>();
        label.put("emoji", emoji);
        label.put("text", text);
        return label;
    }


    private void setupGoogleSheetsService(GoogleSignInAccount account) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                this, Collections.singletonList(SheetsScopes.SPREADSHEETS));
        credential.setSelectedAccountName(account.getEmail());

        sheetsService = new Sheets.Builder(
                new NetHttpTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName("Time Tracker")
                .build();
    }
    private void setupUI() {

        for (int i = 0; i < 144; i++) {
            String textViewId = "box_text_" + (i + 1); // Construct the ID name dynamically
            @SuppressLint("DiscouragedApi") int resID = getResources().getIdentifier(textViewId, "id", getPackageName());
            boxTextViews[i] = findViewById(resID);
        }
        tvCurrentDate = findViewById(R.id.tv_current_date);
        datePicker = findViewById(R.id.date_picker);;
        toggleDatePicker = findViewById(R.id.toggle_date_picker);
        if (toggleDatePicker == null) {
            throw new RuntimeException("ToggleButton not found. Check your layout!");
        }

        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        tvCurrentDate.setText(currentDate);



        // Set the navigation item select listener

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
//                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (id == R.id.btn_show_reports) {
                startActivity(new Intent(this, ReportActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            } else if (id == R.id.show_label_report) {
                startActivity(new Intent(this, LabelReportsActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        });

        toggleDatePicker.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showDatePickerDialog();
            } else {
                datePicker.setVisibility(View.GONE);
//                setCurrentDate();
            }
        });

        // Initialize with current date
        setCurrentDate();
    }

    private void showDatePickerDialog() {
        // Get current date
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, monthOfYear, dayOfMonth);
                        String formattedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(selectedDate.getTime());
                        tvCurrentDate.setText(formattedDate);
                        fetchAllBoxesData(formattedDate);
                    }
                }, year, month, day);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.setOnDismissListener(dialog -> {
            // Check if the DatePickerDialog is dismissed without setting a date
            if (!datePickerDialog.isShowing()) {
                toggleDatePicker.setChecked(false);
            }
        });

        // Show the dialog
        datePickerDialog.show();
    }
    private void setCurrentDate() {
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        tvCurrentDate.setText(currentDate);
//        fetchDataForDate(currentDate);  // Fetch data for the current date
    }
    private void fetchAllBoxesData(String currentDate) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null); // Replace "defaultUser" with a fallback

        if(userId != null){
        DatabaseReference dateRef = mDatabase.getReference("user_data").child(userId).child(currentDate);

        dateRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Check if there's data at this reference
                if (!dataSnapshot.exists()) {
                    // If no data exists for any box, update all boxes to indicate no data
                    updateAllTextViewsForNoData();
                } else {
                    // Process each box individually
                    processEachBox(dataSnapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });}
    }

    private void processEachBox(DataSnapshot dataSnapshot) {
        TextView[] boxTextViews = new TextView[144];

        // Initialize all TextViews dynamically
        for (int i = 0; i < 144; i++) {
            String textViewId = "box_text_" + (i + 1);
            @SuppressLint("DiscouragedApi") int resID = getResources().getIdentifier(textViewId, "id", getPackageName());
            boxTextViews[i] = findViewById(resID);
        }

        // Process each box and update the UI
        for (int i = 0; i < boxTextViews.length; i++) {
            String boxKey = "box" + (i + 1);
            DataSnapshot boxSnapshot = dataSnapshot.child(boxKey);
            TextView boxTextView = boxTextViews[i];

            if (boxTextView != null) {
                if (boxSnapshot.exists() && boxSnapshot.hasChildren()) {
                    BoxData boxData = boxSnapshot.getValue(BoxData.class);
                    if (boxData != null) {
                        updateBoxData(boxTextView, boxData, boxKey);
                    } else {
                        String timeInterval = getTimeIntervalForBox(i + 1);
                        boxTextView.setText(timeInterval + " | 😢 | No Data Available!");
                    }
                } else {
                    String timeInterval = getTimeIntervalForBox(i + 1);
                    boxTextView.setText(timeInterval + " | 😢 | No Data Available!");
                }
            }
        }
    }



    private void updateAllTextViewsForNoData() {
        TextView[] boxTextViews = new TextView[144];

        // Initialize all TextViews dynamically
        for (int i = 0; i < 144; i++) {
            String textViewId = "box_text_" + (i + 1);
            @SuppressLint("DiscouragedApi") int resID = getResources().getIdentifier(textViewId, "id", getPackageName());
            boxTextViews[i] = findViewById(resID);
        }

        // Update each TextView with the no-data message
        for (int i = 0; i < boxTextViews.length; i++) {
            TextView textView = boxTextViews[i];
            if (textView != null) {
                String timeInterval = getTimeIntervalForBox(i + 1); // Convert box number to time interval
                textView.setText(timeInterval + " | 😢 | " + "No Data Available!");
            }
        }
    }


    private void updateBoxData(TextView boxTextView, BoxData boxData, String boxId) {
        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null); // Use default if user is not signed in

        // Update Firebase reference to use the dynamic userId
        if(userId != null){
        DatabaseReference labelsRef = mDatabase.getReference("user_data").child(userId).child("labels");
        String labelName = boxData.getSelectedOption(); // labelName contains the label name directly

        labelsRef.orderByChild("text").equalTo(labelName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String emoji = "🤨"; // Default emoji if no match is found
                String lbl_nam="";
                if (dataSnapshot.exists()) {
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        emoji = childSnapshot.child("emoji").getValue(String.class);
                        lbl_nam = childSnapshot.child("text").getValue(String.class);
                        break; // Assume the first match is the correct one since label names should be unique
                    }
                }

                int boxNumber = Integer.parseInt(boxId.replace("box", ""));
                String timeInterval = getTimeIntervalForBox(boxNumber); // Get the time range

                String displayText = timeInterval + " | " + lbl_nam + " " + emoji + " | " + boxData.getData();
                boxTextView.setText(displayText);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                int boxNumber = Integer.parseInt(boxId.replace("box", ""));
                String timeInterval = getTimeIntervalForBox(boxNumber);
                boxTextView.setText(timeInterval + " | 🤨 | " + boxData.getData());
                Toast.makeText(MainActivity.this, "Failed to fetch emoji", Toast.LENGTH_SHORT).show();
            }
        });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem signInItem = menu.findItem(R.id.sign_in_button);
        if (isUserSignedIn()) {
            signInItem.setTitle("Sign Out");
        } else {
            signInItem.setTitle("Sign In");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Start the SettingsActivity when the "Settings" item is clicked
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
//            overridePendingTransition(R.anim.slide_in_top_right, R.anim.fade_out);
            return true;
        } else if (id == R.id.download_data) {
            // Check if the user is signed in and initiate data download
            if (GoogleSignIn.getLastSignedInAccount(this) != null) {
                fetchDataFromFirebase();
            } else {
                Log.e("GoogleAuth", "User is not signed in");
                // Optionally, prompt the user to sign in here
                Toast.makeText(MainActivity.this, "SignIn Required", Toast.LENGTH_SHORT).show();
                showSignInDialog();
            }
            return true;
        } else if (id == R.id.sign_in_button) {
            if (isUserSignedIn()) {
                signOut();
            } else {
                showSignInDialog();
            }
            return true;
        }


        return super.onOptionsItemSelected(item);
    }


    private void fetchDataFromFirebase() {
        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null); // Use default if user is not signed in

        // Update Firebase reference to use the dynamic userId
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        if(userId != null) {
            DatabaseReference ref = database.getReference("user_data").child(userId);
            Toast.makeText(MainActivity.this, "Fetching Data...", Toast.LENGTH_SHORT).show();
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<List<Object>> rows = new ArrayList<>();
                    for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                        Log.d("test_input", dateSnapshot.toString());
                        String date = dateSnapshot.getKey();
                        assert date != null;
                        if (date.length() == 10) {
                            for (DataSnapshot boxSnapshot : dateSnapshot.getChildren()) {
                                Object rawValue = boxSnapshot.getValue();
                                assert rawValue != null;
                                Log.d("test_input_1", rawValue.toString());
                                if (rawValue instanceof Map) {
                                    Map<String, Object> boxData = (Map<String, Object>) rawValue;
                                    List<Object> rowData = new ArrayList<>();
                                    rowData.add(date);
                                    rowData.add(boxSnapshot.getKey());
                                    rowData.add(boxData.get("data"));
                                    rowData.add(boxData.get("selectedOption"));
//                                rowData.add(boxData.get("timestamp"));
                                    rows.add(rowData);
                                } else {
                                    Log.e("DataError", "Unexpected data type in Firebase: " + rawValue.getClass().getName());
                                }
                            }
                        }
                    }
                    writeToGoogleSheet(rows);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.w("FirebaseData", "loadPost:onCancelled", databaseError.toException());
                    Toast.makeText(MainActivity.this, "Fetching Data Failed!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void writeToGoogleSheet(List<List<Object>> data) {
        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null); // Use default if user is not signed in

        // Update Firebase reference to use the dynamic userId
        if(userId != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(userId).child("spreadsheetId");

            // Show Toast to indicate that data is being written
            Toast.makeText(MainActivity.this, "Writing Data...", Toast.LENGTH_SHORT).show();
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String spreadsheetId = dataSnapshot.getValue(String.class);
                    if (spreadsheetId == null || spreadsheetId.isEmpty()) {
                        // No Spreadsheet ID found, create a new spreadsheet
                        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                                MainActivity.this, Collections.singletonList(SheetsScopes.SPREADSHEETS));
                        credential.setSelectedAccountName(Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(MainActivity.this)).getEmail());
                        createNewSpreadsheet(credential, data);
                    } else {
                        // Spreadsheet ID exists, proceed to update
                        updateSpreadsheet(spreadsheetId, data);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("Firebase", "Failed to read spreadsheet ID", databaseError.toException());
                    Toast.makeText(MainActivity.this, "Writing Data Failed!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void saveSpreadsheetIdToFirebase(String spreadsheetId) {
        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null); // Use default if user is not signed in

        // Update Firebase reference to use the dynamic userId
        if(userId != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(userId);

            // Save the spreadsheetId to Firebase for the logged-in user
            ref.child("spreadsheetId").setValue(spreadsheetId).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Spreadsheet ID saved successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to save Spreadsheet ID", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }



    private void createNewSpreadsheet(GoogleAccountCredential credential, List<List<Object>> data) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Sheets service = new Sheets.Builder(
                        new NetHttpTransport(), new GsonFactory(), credential)
                        .setApplicationName("Time Tracker")
                        .build();

                Spreadsheet spreadsheet = new Spreadsheet()
                        .setProperties(new SpreadsheetProperties().setTitle("Time Tracker Report"));
                spreadsheet = service.spreadsheets().create(spreadsheet)
                        .setFields("spreadsheetId")
                        .execute();

                // Save the new spreadsheet ID in Firebase
                saveSpreadsheetIdToFirebase(spreadsheet.getSpreadsheetId());
                // Update spreadsheet with initial data
                updateSpreadsheet(spreadsheet.getSpreadsheetId(), data);
                // Show the link to the new spreadsheet
//                showSpreadsheetLink(spreadsheet.getSpreadsheetId());
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("GoogleSheets", "Failed to create spreadsheet", e);
            }
        });
    }

    private void updateSpreadsheet(String spreadsheetId, List<List<Object>> data) {
        if (sheetsService == null) {
            Log.e("GoogleSheets", "Sheets service has not been initialized");
            Toast.makeText(MainActivity.this, "SignIn Required !", Toast.LENGTH_SHORT).show();
            showSignInDialog();
            return;
        }
        String range = "Sheet1!A1";  // Adjust as needed.

        backgroundExecutor.execute(() -> {
            ValueRange body = new ValueRange().setValues(data);
            try {
                sheetsService.spreadsheets().values().update(spreadsheetId, range, body)
                        .setValueInputOption("USER_ENTERED")
                        .execute();
                Log.i("GoogleSheets", "Data updated successfully.");

                // Show the link to the updated spreadsheet
                runOnUiThread(() -> showSpreadsheetLink(spreadsheetId));
            } catch (Exception e) {
                Log.e("GoogleSheets", "Failed to update data", e);
                Toast.makeText(MainActivity.this, "Sheet Update Failed !", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showSpreadsheetLink(String spreadsheetId) {
        Toast.makeText(MainActivity.this, "Sheet Updated Successfully!", Toast.LENGTH_SHORT).show();
        String url = "https://docs.google.com/spreadsheets/d/" + spreadsheetId;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Access Your Spreadsheet");
        builder.setMessage("Click here to open your spreadsheet: " + url);
        builder.setPositiveButton("Open", (dialog, which) -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }



}

