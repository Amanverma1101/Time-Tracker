package com.example.timetracker;

public class BoxData {
    private String data;           // The data entered by the user
    private String selectedOption; // Option selected by the user
    private long timestamp;        // Timestamp when data was entered

    // Default constructor required for Firebase
    public BoxData() {}

    // Constructor
    public BoxData(String data, String selectedOption, long timestamp) {
        this.data = data;
        this.selectedOption = selectedOption;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
