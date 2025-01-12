package com.example.timetracker;

import android.content.DialogInterface;
import android.graphics.Color;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SettingsActivity extends AppCompatActivity {

    private GridLayout labelContainer;
    private Button btnAddNew;
    private EditText newLabelInput;
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private boolean isAddingNew = false; // Flag to check if adding a new label

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



    }

    private void loadLabelsFromRealtimeDB() {
        String userId = "user1";
        DatabaseReference labelsRef = firebaseDatabase.getReference("user_data").child(userId).child("labels");

        labelsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                labelContainer.removeAllViews(); // Clears all existing views to prevent duplication
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String label = snapshot.getValue(String.class);
                    FrameLayout labelView = createLabelView(label, R.drawable.def); // Correctly name it to reflect it's a FrameLayout
                    String key = snapshot.getKey();

                    labelContainer.addView(labelView);

                    labelView.setOnLongClickListener(v -> {
                        showPopup(v, label, key);
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


    private FrameLayout createLabelView(String label, int drawableId) {
        FrameLayout frameLayout = new FrameLayout(this);
        GridLayout.LayoutParams frameParams = new GridLayout.LayoutParams();
        frameParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 126, getResources().getDisplayMetrics());
        frameParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
        frameParams.setMargins(0, 12, 0, 8);  // Set consistent margins
        frameLayout.setLayoutParams(frameParams);


        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(70, 70, Gravity.CENTER_HORIZONTAL));
        imageView.setImageDrawable(ContextCompat.getDrawable(this, drawableId)); // Set the actual image
        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(126, 126, Gravity.CENTER_HORIZONTAL); // Adjust 70dp to match your XML or required size
        imageView.setLayoutParams(imageParams);
        imageView.setBackground(ContextCompat.getDrawable(this, R.drawable.circular_background)); // Ensure this line is correct
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);


        TextView textView = new TextView(this);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        textParams.topMargin = 140;
        textView.setLayoutParams(textParams);
        textView.setText(label);
        textView.setGravity(Gravity.CENTER);
        textView.setTextAppearance(android.R.style.TextAppearance_Medium);

        frameLayout.addView(imageView);
        frameLayout.addView(textView);

        return frameLayout;
    }


    private void addNewLabelInput() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter New Label");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Label name");

        // Specify the type of input expected
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String label = input.getText().toString().trim();
                if (!label.isEmpty()) {
                    saveNewLabel(label);
                } else {
                    Toast.makeText(SettingsActivity.this, "Label can't be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
        input.requestFocus();
    }








    private void saveNewLabel(String label) {
        if (!label.isEmpty()) {
            FrameLayout newLabelView = createLabelView(label,R.drawable.def);
            labelContainer.addView(newLabelView);
            btnAddNew.setText("Add New");
            isAddingNew = false;

            String userId = "user1";
            DatabaseReference labelsRef = firebaseDatabase.getReference("user_data").child(userId).child("labels");
            String labelKey = labelsRef.push().getKey(); // Generate a unique key

            labelsRef.child(labelKey).setValue(label)
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
