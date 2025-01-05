package com.example.timetracker;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SettingsActivity extends AppCompatActivity {

    private LinearLayout labelContainer;
    private Button btnAddNew;
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private boolean isAddingNew = false; // Flag to check if adding a new label

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.action_settings); // Ensure this is the correct layout reference

        labelContainer = findViewById(R.id.label_container_1);
        btnAddNew = findViewById(R.id.btn_add_new);

        loadLabelsFromRealtimeDB();

        btnAddNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isAddingNew) {
                    btnAddNew.setText("Save");
                    addNewLabelInput(); // Method to add new EditText
                    isAddingNew = true;
                } else {
                    saveNewLabel();
                }
            }
        });
    }

    private void loadLabelsFromRealtimeDB() {
        String userId = "user1";
        DatabaseReference labelsRef = firebaseDatabase.getReference("user_data").child(userId).child("labels");

        labelsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                labelContainer.removeAllViews();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String label = snapshot.getValue(String.class);
                    TextView textView = createLabelTextView(label);
                    String key = snapshot.getKey();
                    textView.setPadding(16, 16, 16, 16);
                    textView.setTextColor(Color.WHITE);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                    labelContainer.addView(textView);

                    textView.setOnLongClickListener(v -> {
                        showPopup(v, label,key);
                        return true;
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Error loading labels: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPopup(View view, String label, String key) {
        PopupMenu popup = new PopupMenu(SettingsActivity.this, view);
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
//            Log.d("MenuTest", "Menu item selected ID: " + item.getItemId());
            int id = item.getItemId();
            if (id == R.id.edit) {
                editLabel(label, key);
                return true;
            } else if (id == R.id.delete) {
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


    private TextView createLabelTextView(String label) {
        TextView labelView = new TextView(this, null, 0, R.style.BoxStyle);
        labelView.setText(label);
        adjustViewMargins(labelView);
        return labelView;
    }
    private void addNewLabelInput() {
        EditText newLabel = new EditText(this);
        newLabel.setHint("Enter new label");
        adjustViewMargins(newLabel);
        labelContainer.addView(newLabel);
        newLabel.requestFocus();  // Focus on the new label input
    }
    private void saveNewLabel() {
        EditText newLabelInput = (EditText) labelContainer.getChildAt(labelContainer.getChildCount() - 1);
        if (newLabelInput instanceof EditText && !newLabelInput.getText().toString().isEmpty()) {
            String newLabel = newLabelInput.getText().toString();

            convertEditTextToTextView(newLabelInput);
            btnAddNew.setText("Add New");
            isAddingNew = false;

            String userId = "user1";
            DatabaseReference labelsRef = firebaseDatabase.getReference("user_data").child(userId).child("labels");

            String labelKey = labelsRef.push().getKey();  // Generate a unique key
            assert labelKey != null;
            labelsRef.child(labelKey).setValue(newLabel)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Label saved successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Error saving label", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getApplicationContext(), "Label can't be empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void convertEditTextToTextView(EditText editText) {
        TextView labelView = createLabelTextView(editText.getText().toString());
        labelContainer.removeView(editText);
        labelContainer.addView(labelView);
    }

    private void adjustViewMargins(View view) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(8, 8, 8, 16);  // Applying margins to ensure spacing between elements
        view.setLayoutParams(params);
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
