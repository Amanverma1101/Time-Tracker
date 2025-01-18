package com.example.timetracker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
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

    private Button btnShowReports, btnShowLabelReports;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initializeFirebase();
        initializeGoogleSignIn();
        setupUI();
        fetchAllBoxesData();
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
        btnShowReports = findViewById(R.id.btn_show_reports);
        btnShowLabelReports = findViewById(R.id.show_label_report);

        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        tvCurrentDate.setText(currentDate);
    }

    private void fetchAllBoxesData() {
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
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
        TextView[] allTextViews = {box1TextView, box2TextView, box3TextView, box4TextView, box5TextView,
                box6TextView, box7TextView, box8TextView, box9TextView, box10TextView};
        for (TextView textView : allTextViews) {
            if (textView != null) {
                textView.setText(textView.getTag().toString() + " | 😢 | " + message);
            }
        }
    }



    private TextView findBoxTextView(String boxId) {
        switch (boxId) {
            case "box1": return box1TextView;
            case "box2": return box2TextView;
            case "box3": return box3TextView;
            case "box4": return box4TextView;
            case "box5": return box5TextView;
            case "box6": return box6TextView;
            case "box7": return box7TextView;
            case "box8": return box8TextView;
            case "box9": return box9TextView;
            case "box10": return box10TextView;
            default: return null;
        }
    }

    private void updateBoxData(TextView boxTextView, BoxData boxData, String boxId) {
        DatabaseReference labelsRef = mDatabase.getReference("user_data").child("user1").child("labels");
        int optionIndex = Integer.parseInt(boxData.getSelectedOption()); // Handle potential NumberFormatException

        labelsRef.child(String.valueOf(optionIndex)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot emojiSnapshot) {
                String emoji = emojiSnapshot.exists() ? emojiSnapshot.child("emoji").getValue(String.class) : "🤨";
                String displayText = boxId + " | " + emoji + " | " + boxData.getData();
                boxTextView.setText(displayText);
            }

            @Override
            public void onCancelled(DatabaseError emojiDatabaseError) {
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Start the SettingsActivity when the "Settings" item is clicked
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        } else if (id == R.id.download_data) {
            // Check if the user is signed in and initiate data download
            if (GoogleSignIn.getLastSignedInAccount(this) != null) {
                fetchDataFromFirebase();
            } else {
                Log.e("GoogleAuth", "User is not signed in");
                // Optionally, prompt the user to sign in here
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                signInLauncher.launch(signInIntent);
            }
            return true;
        } else if (id == R.id.sign_in_button) {
            // Launch the sign-in activity when the sign-in button is clicked
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            signInLauncher.launch(signInIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
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

