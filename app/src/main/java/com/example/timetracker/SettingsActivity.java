package com.example.timetracker;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.icu.text.BreakIterator;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private GridLayout labelContainer;
    private Button btnAddNew;
    private EditText newLabelInput;
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private boolean isAddingNew = false; // Flag to check if adding a new label

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
        setContentView(R.layout.action_settings);

        labelContainer = findViewById(R.id.label_container_1);
        btnAddNew = findViewById(R.id.btn_add_new);
        newLabelInput = new EditText(this); // Initialize it here or in addNewLabelInput()

        loadLabelsFromRealtimeDB();

        btnAddNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isAddingNew) {
                    btnAddNew.setText("Save");
                    addNewLabelInput();  // This now opens a dialog for input
                    isAddingNew = true;  // You may still track this if it affects other parts of your UI logic
                } else {
                    // This might be redundant now if all saving is handled via the dialog
                    btnAddNew.setText("Add New");
                    isAddingNew = false;
                }
            }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish(); // Close the activity
                overridePendingTransition(R.anim.fade_in, R.anim.slide_out_bottom);
            }
        });
    }

    private boolean isValidEmojiInput(String input) {
        BreakIterator iterator = BreakIterator.getCharacterInstance();
        iterator.setText(input);
        int count = 0;
        while (iterator.next() != BreakIterator.DONE) {
            count++;
        }
        return count == 1; // Only valid if exactly one grapheme cluster
    }

    private void loadLabelsFromRealtimeDB() {
        if (!isNetworkAvailable()) {
            loadLabelsFromLocalStorage();
            return;
        }

        String userId = "user1";
        DatabaseReference labelsRef = firebaseDatabase.getReference("user_data").child(userId).child("labels");

        labelsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                labelContainer.removeAllViews(); // Clears existing views
                List<EmojiData> labelList = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    GenericTypeIndicator<Map<String, String>> t = new GenericTypeIndicator<Map<String, String>>() {};
                    Map<String, String> labelData = snapshot.getValue(t);
                    if (labelData != null) {
                        String label = labelData.get("text");
                        String emoji = labelData.get("emoji");
                        if (label != null) {
                            FrameLayout labelView = createLabelView(label, emoji, R.drawable.def);
                            String key = snapshot.getKey();
                            labelContainer.addView(labelView);
                            labelList.add(new EmojiData(emoji, label));
                            labelView.setOnLongClickListener(v -> {
                                showPopup(v, label, key);
                                return true;
                            });
                        }
                    }
                }
                saveEmojiList(labelList); // Save data locally
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                loadLabelsFromLocalStorage();
            }
        });
    }

    private void loadLabelsFromLocalStorage() {
        List<EmojiData> localLabels = getEmojiList();

        if (localLabels != null && !localLabels.isEmpty()) {
            labelContainer.removeAllViews();

            for (EmojiData item : localLabels) {
                FrameLayout labelView = createLabelView(item.text, item.emoji, R.drawable.def);
                labelContainer.addView(labelView);
            }

            Toast.makeText(this, "Loaded from local storage", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No saved labels found!", Toast.LENGTH_SHORT).show();
        }
    }


    private void saveEmojiList(List<EmojiData> emojiList) {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String json = gson.toJson(emojiList);
        editor.putString("emoji_list", json);
        editor.apply();
    }

    private List<EmojiData> getEmojiList() {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("emoji_list", null);
        Type type = new TypeToken<List<EmojiData>>() {}.getType();

        return json != null ? gson.fromJson(json, type) : new ArrayList<>();
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }


    private void showPopup(View view, String label, String key) {
        PopupMenu popup = new PopupMenu(SettingsActivity.this, view);
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
//            Log.d("MenuTest", "Menu item selected ID: " + item.getItemId());
            int id = item.getItemId();
//            if (id == R.id.edit) {
////                editLabel(label, key);
//                return true;
//            } else
                if (id == R.id.delete) {
                deleteLabel(view, key);
                return true;
            } else {
                return false;
            }

        });

        popup.show();
    }

    private void deleteLabel(View labelView, String key) {
        String userId = "user1";
        DatabaseReference labelRef = firebaseDatabase.getReference("user_data").child(userId).child("labels").child(key);
        labelRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Ensure labelView is not null
                if (labelView != null && labelView.getParent() != null) {
                    runOnUiThread(() -> {
                        ((ViewGroup) labelView.getParent()).removeView(labelView);
                        Toast.makeText(getApplicationContext(), "Label deleted successfully", Toast.LENGTH_SHORT).show();
                    });
                }
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Error deleting label", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void editLabel(String currentLabel, String key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Label");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currentLabel);
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newLabel = input.getText().toString();
            updateLabelInFirebase(key, newLabel);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateLabelInFirebase(String key, String newLabel) {
        DatabaseReference labelRef = firebaseDatabase.getReference("user_data").child("user1").child("labels").child(key);
        labelRef.setValue(newLabel).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getApplicationContext(), "Label updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Error updating label", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private FrameLayout createLabelView(String text, String emoji, int drawableId) {
        Log.d("label_testing", "createLabelView: " + text);

        // Creating a new FrameLayout programmatically
        FrameLayout frameLayout = new FrameLayout(this);
        GridLayout.LayoutParams frameParams = new GridLayout.LayoutParams();
        frameParams.width = convertDpToPixels(128); // Width as per XML
        frameParams.height = GridLayout.LayoutParams.WRAP_CONTENT; // Height set to wrap content
        frameParams.setMargins(convertDpToPixels(0), convertDpToPixels(8), convertDpToPixels(0), convertDpToPixels(8));
        frameLayout.setLayoutParams(frameParams);

        // Creating TextView programmatically for the emoji
        TextView emojiView = new TextView(this);
        FrameLayout.LayoutParams emojiParams = new FrameLayout.LayoutParams(
                convertDpToPixels(70), convertDpToPixels(70), Gravity.CENTER_HORIZONTAL);
        emojiView.setLayoutParams(emojiParams);
        emojiView.setGravity(Gravity.CENTER);
        emojiView.setText(emoji);
        emojiView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32); // Set emoji size
        emojiView.setBackground(ContextCompat.getDrawable(this, R.drawable.circular_background)); // Set circular background
        frameLayout.addView(emojiView);

        // Creating TextView programmatically for the label
        TextView textView = new TextView(this);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        textParams.topMargin = convertDpToPixels(80); // Top margin to position text below the emoji
        textView.setLayoutParams(textParams);
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setTextAppearance(android.R.style.TextAppearance_Medium); // Styling text appearance
        frameLayout.addView(textView);

        return frameLayout;
    }

    private int convertDpToPixels(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void addNewLabelInput() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter New Label and Emoji");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText labelInput = new EditText(this);
        labelInput.setInputType(InputType.TYPE_CLASS_TEXT);
        labelInput.setHint("Label name");
        layout.addView(labelInput);

        final EditText emojiInput = new EditText(this);
        emojiInput.setInputType(InputType.TYPE_CLASS_TEXT);
        emojiInput.setHint("Emoji (single character)");
        layout.addView(emojiInput);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String label = labelInput.getText().toString().trim();
            String emoji = emojiInput.getText().toString().trim();
            if (!label.isEmpty()) {
                if(isValidEmojiInput(emoji)) {
                    saveNewLabel(label, emoji);
                }else{
                    Toast.makeText(SettingsActivity.this, "Try some different Emoji!", Toast.LENGTH_SHORT).show();
                    // This line should not toggle `isAddingNew` but ensure UI consistency
                    btnAddNew.setText("Add New");
                    isAddingNew = false;
                }
            } else {
                Toast.makeText(SettingsActivity.this, "Label and emoji can't be empty, emoji must be a single character", Toast.LENGTH_SHORT).show();
                // This line should not toggle `isAddingNew` but ensure UI consistency
                btnAddNew.setText("Add New");
                isAddingNew = false;
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            btnAddNew.setText("Add New");
            isAddingNew = false;
        });

        builder.setOnCancelListener(dialog -> {
            btnAddNew.setText("Add New");
            isAddingNew = false;
        });

        builder.show();
        labelInput.requestFocus();
    }

    private void saveNewLabel(String label, String emoji) {
        if (!label.isEmpty() && isValidEmojiInput(emoji)) {
            FrameLayout newLabelView = createLabelView(label, emoji, R.drawable.def);
            labelContainer.addView(newLabelView);
            btnAddNew.setText("Add New");
            isAddingNew = false;

            String userId = "user1";
            DatabaseReference rootRef = firebaseDatabase.getReference("user_data").child(userId);
            DatabaseReference labelsRef = rootRef.child("labels");
            DatabaseReference counterRef = rootRef.child("labelCounter");


            // Read the current counter
            counterRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Integer count = dataSnapshot.getValue(Integer.class);
                    if (count == null) count = 0;  // Default to 0 if not found

                    // Use count as the label key
                    Map<String, Object> labelData = new HashMap<>();
                    labelData.put("text", label);
                    labelData.put("emoji", emoji);

                    Integer finalCount = count;
                    labelsRef.child(String.valueOf(count)).setValue(labelData)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getApplicationContext(), "Label and emoji saved successfully", Toast.LENGTH_SHORT).show();
                                    // Increment the counter after successful save
                                    counterRef.setValue(finalCount + 1);
                                } else {
                                    Toast.makeText(getApplicationContext(), "Error saving label and emoji", Toast.LENGTH_SHORT).show();
                                }
                            });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getApplicationContext(), "Error accessing the database: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "Label and emoji can't be empty, emoji must be a single character...", Toast.LENGTH_SHORT).show();
            isAddingNew = false; // Reset the flag when cancel is clicked
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        Log.d("ActivityLifecycle", "onResume called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("ActivityLifecycle", "onPause called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("ActivityLifecycle", "onStop called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("ActivityLifecycle", "onDestroy called");
    }

}