package com.example.timetracker;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private FirebaseDatabase mDatabase;
    private TextView box1TextView, box2TextView, box3TextView, box4TextView, box5TextView;
    private TextView box6TextView, box7TextView, box8TextView, box9TextView, box10TextView;
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
        getSupportActionBar().setTitle("");

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
            // Signed in successfully, show authenticated UI.
            Log.i("SignInSuccess", "Signed in as: " + account.getEmail());
            if (signInDialog != null && signInDialog.isShowing()) {
                signInDialog.dismiss();
            }
            Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show();
        } catch (ApiException e) {
            Log.w("SignInFailure", "signInResult:failed code=" + e.getStatusCode());
            // Handle the exception here
        }
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
        DatabaseReference dateRef = mDatabase.getReference("user_data").child("user1").child(currentDate);

        dateRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Check if there's data at this reference
                if (!dataSnapshot.exists()) {
                    // If no data exists for any box, update all boxes to indicate no data
                    updateAllTextViewsForNoData("No Data Available!");
                } else {
                    // Process each box individually
                    processEachBox(dataSnapshot);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processEachBox(DataSnapshot dataSnapshot) {
        TextView[] allTextViews = {box1TextView, box2TextView, box3TextView, box4TextView, box5TextView,
                box6TextView, box7TextView, box8TextView, box9TextView, box10TextView};
        for (int i = 0; i < allTextViews.length; i++) {
            String boxKey = "box" + (i + 1);
            DataSnapshot boxSnapshot = dataSnapshot.child(boxKey);
            TextView boxTextView = allTextViews[i];

            if (boxTextView != null) {
                if (boxSnapshot.exists() && boxSnapshot.hasChildren()) {
                    BoxData boxData = boxSnapshot.getValue(BoxData.class);
                    if (boxData != null) {
                        updateBoxData(boxTextView, boxData, boxKey);
                    } else {
                        boxTextView.setText(boxKey + " | 😢 | No Data Available!");
                    }
                } else {
                    boxTextView.setText(boxKey + " | 😢 | No Data Available!");
                }
            }
        }
    }


    private void updateAllTextViewsForNoData(String message) {
        TextView[] allTextViews = {
                box1TextView, box2TextView, box3TextView, box4TextView, box5TextView,
                box6TextView, box7TextView, box8TextView, box9TextView, box10TextView
        };

        for (TextView textView : allTextViews) {
            if (textView != null) {
                String tag = (textView.getTag() != null) ? textView.getTag().toString() : "Default Tag";
                textView.setText(tag + " | 😢 | " + message);
            }
        }
    }

    private void updateBoxData(TextView boxTextView, BoxData boxData, String boxId) {
        DatabaseReference labelsRef = mDatabase.getReference("user_data").child("user1").child("labels");
        String labelName = boxData.getSelectedOption(); // labelName contains the label name directly

        labelsRef.orderByChild("text").equalTo(labelName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String emoji = "🤨"; // Default emoji if no match is found
                String lbl_nam="";
                if (dataSnapshot.exists()) {
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        emoji = childSnapshot.child("emoji").getValue(String.class);
                        lbl_nam = childSnapshot.child("text").getValue(String.class);
                        break; // Assume the first match is the correct one since label names should be unique
                    }
                }
                String displayText = boxId + " | "+lbl_nam + " " + emoji + " | " + boxData.getData();
                boxTextView.setText(displayText);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                boxTextView.setText(boxId + " | 🤨 | " + boxData.getData());
                Toast.makeText(MainActivity.this, "Failed to fetch emoji", Toast.LENGTH_SHORT).show();
            }
        });
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
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("user_data/user1");
        Toast.makeText(MainActivity.this, "Fetching Data...", Toast.LENGTH_SHORT).show();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<List<Object>> rows = new ArrayList<>();
                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    Log.d("test_input", dateSnapshot.toString());
                    String date = dateSnapshot.getKey().toString();
                    if(date.length()==10) {
                        for (DataSnapshot boxSnapshot : dateSnapshot.getChildren()) {
                            Object rawValue = boxSnapshot.getValue();
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
            public void onCancelled(DatabaseError databaseError) {
                Log.w("FirebaseData", "loadPost:onCancelled", databaseError.toException());
                Toast.makeText(MainActivity.this, "Fetching Data Failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void writeToGoogleSheet(List<List<Object>> data) {
        String userId = "user1"; // Replace with FirebaseAuth.getInstance().getCurrentUser().getUid(); for actual user IDs
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(userId).child("spreadsheetId");
        Toast.makeText(MainActivity.this, "Writing Data...", Toast.LENGTH_SHORT).show();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String spreadsheetId = dataSnapshot.getValue(String.class);
                if (spreadsheetId == null || spreadsheetId.isEmpty()) {
                    // No Spreadsheet ID found, create a new spreadsheet
                    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                            MainActivity.this, Collections.singletonList(SheetsScopes.SPREADSHEETS));
                    credential.setSelectedAccountName(GoogleSignIn.getLastSignedInAccount(MainActivity.this).getEmail());
                    createNewSpreadsheet(credential, data);
                } else {
                    // Spreadsheet ID exists, proceed to update
                    updateSpreadsheet(spreadsheetId, data);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Firebase", "Failed to read spreadsheet ID", databaseError.toException());
                Toast.makeText(MainActivity.this, "Writing Data Failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveSpreadsheetIdToFirebase(String spreadsheetId) {
//        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String userId = "user1";
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(userId);
        ref.child("spreadsheetId").setValue(spreadsheetId);
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

