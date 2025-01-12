package com.example.timetracker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;


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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;



public class MainActivity extends AppCompatActivity {

    private FirebaseDatabase mDatabase;
    private TextView box1TextView, box2TextView, box3TextView, box4TextView, box5TextView;
    private TextView box6TextView, box7TextView, box8TextView, box9TextView, box10TextView;
    private TextView tvCurrentDate;
    private Sheets sheetsService;
    // Define an executor that uses a single background thread.
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    private Button btnShowReports; // Button to view reports
    private Button btnShowLabelReports; // Button to view reports
    private static final int RC_SIGN_IN = 9001; // Can be any integer unique to the Activity
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> signInLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance();


        // Initialize the Google Sign-In client
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(SheetsScopes.SPREADSHEETS))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso); // Corrected to use the class member variable

        // Prepare the ActivityResultLauncher
        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleSignInResult(task);
                    }
                });


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
        btnShowLabelReports = findViewById(R.id.show_label_report);

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
        btnShowLabelReports.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LabelReportsActivity.class);
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
        }else if(id == R.id.download_data){
            if (mGoogleSignInClient != null) {
                fetchDataFromFirebase();
            } else {
                Log.e("GoogleAuth", "User is not signed in");
            }
            return true;
        }else if(id == R.id.sign_in_button) {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            signInLauncher.launch(signInIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Initialize Google Sheets service
            setupGoogleSheetsService(account);
            Log.i("SignInSuccess", "Signed in as: " + account.getEmail());
        } catch (ApiException e) {
            Log.w("SignInFailure", "signInResult:failed code=" + e.getStatusCode());
            // Handle the exception appropriately
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



    private void fetchDataFromFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("user_data/user1");

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
                                rowData.add(boxData.get("timestamp"));
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
            }
        });
    }
    private void writeToGoogleSheet(List<List<Object>> data) {
        String userId = "user1"; // Replace with FirebaseAuth.getInstance().getCurrentUser().getUid(); for actual user IDs
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(userId).child("spreadsheetId");

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
            }
        });
    }


    private void showSpreadsheetLink(String spreadsheetId) {
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
