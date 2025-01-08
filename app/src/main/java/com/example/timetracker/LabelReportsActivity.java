//package com.example.timetracker;
//
//import android.icu.text.SimpleDateFormat;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.AdapterView;
//import android.widget.ArrayAdapter;
//import android.widget.ImageView;
//import android.widget.Spinner;
//import android.widget.TextView;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Locale;
//
//public class LabelReportsActivity extends AppCompatActivity {
//    private Spinner labelSpinner;
//    private ImageView labelImage;
//    private TextView dateText, dataText;
//    private HashMap<String, Integer> labelImages; // Map label names to drawable IDs
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_label_reports);
//
//        labelSpinner = findViewById(R.id.label_spinner);
//        labelImage = findViewById(R.id.label_image);
//        dateText = findViewById(R.id.date_text);
//        dataText = findViewById(R.id.data_text);
//
//        setupLabelImages();
//        setupSpinner();
//    }
//
//    private void setupLabelImages() {
//        labelImages = new HashMap<>();
//        labelImages.put("Meditation", R.drawable.med);
//        // Add other labels and their corresponding images
//    }
//
//    private void setupSpinner() {
//        // Assuming labels are static or fetched from somewhere
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(labelImages.keySet()));
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        labelSpinner.setAdapter(adapter);
//
//        labelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                String selectedLabel = (String) parent.getItemAtPosition(position);
//                updateUIForLabel(selectedLabel);
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//            }
//        });
//    }
//
//    private void updateUIForLabel(String label) {
//        // Fetch data based on label and update UI
//        int imageRes = labelImages.getOrDefault(label, 0);
//        labelImage.setImageResource(imageRes);
//
//        // Assume fetchData returns a list of data entries
//        List<DataEntry> entries = fetchData(label);
//        if (!entries.isEmpty()) {
//            // For simplicity, showing only the latest entry
//            DataEntry latestEntry = entries.get(0);
//            dateText.setText(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date(latestEntry.timestamp)));
//            dataText.setText(latestEntry.data);
//        }
//    }
//
//    private List<DataEntry> fetchData(String label) {
//        // Implement fetching data based on label
//        return new ArrayList<>();
//    }
//
//    static class DataEntry {
//        String data;
//        long timestamp;
//
//        DataEntry(String data, long timestamp) {
//            this.data = data;
//            this.timestamp = timestamp;
//        }
//    }
//}
