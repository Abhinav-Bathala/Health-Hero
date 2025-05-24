package com.example.myapplication;

public class MealEntry {

    // Name of the meal
    private String mealName;

    // Number of calories in the meal
    private int calories;

    // Additional notes about the meal
    private String notes;

    // Timestamp of when the meal was recorded
    private String timestamp;

    // Default constructor required for Firebase deserialization
    public MealEntry() {}

    // Constructor to initialize all meal entry fields
    public MealEntry(String mealName, int calories, String notes, String timestamp) {
        this.mealName = mealName;
        this.calories = calories;
        this.notes = notes;
        this.timestamp = timestamp;
    }

    // Getter for meal name
    public String getMealName() { return mealName; }

    // Getter for calories
    public int getCalories() { return calories; }

    // Getter for notes
    public String getNotes() { return notes; }

    // Getter for timestamp
    public String getTimestamp() { return timestamp; }
}
