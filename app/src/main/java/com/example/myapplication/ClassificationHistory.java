package com.example.myapplication; // Ensure this matches your package structure

import com.google.firebase.database.PropertyName;

public class ClassificationHistory {
    private String id;
    private String result;
    private float confidence;
    private String timestamp;
    private String imageUrl; // New field for storing image URL

    // Default constructor required for Firebase calls
    public ClassificationHistory() {
    }

    // Constructor including the imageUrl
    public ClassificationHistory(String id, String result, float confidence, String timestamp, String imageUrl) {
        this.id = id;
        this.result = result;
        this.confidence = confidence;
        this.timestamp = timestamp;
        this.imageUrl = imageUrl;
    }

    // Constructor without the imageUrl for backward compatibility
    public ClassificationHistory(String id, String result, float confidence, String timestamp) {
        this.id = id;
        this.result = result;
        this.confidence = confidence;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    @PropertyName("dateTime")
    public String getTimestamp() {
        return timestamp;
    }

    @PropertyName("dateTime")
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // Override toString() method to include the image URL
    @Override
    public String toString() {
        String confidencePercentage;
        if (confidence == 1.0) { // Check if confidence is 100%
            confidencePercentage = "100%"; // No decimals for 100%
        } else {
            confidencePercentage = String.format("%.2f%%", confidence * 100); // Format with two decimals
        }

        return "Result: " + result + "\n" +
                "Percentage: " + confidencePercentage + "\n" +
                "Date: " + timestamp;
    }

}
