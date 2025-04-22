package com.example.myapplication;

public class MealEntry {
    private String mealName;
    private int calories;
    private String notes;
    private String timestamp; // optional

    public MealEntry() {} // Needed for Firebase

    public MealEntry(String mealName, int calories, String notes, String timestamp) {
        this.mealName = mealName;
        this.calories = calories;
        this.notes = notes;
        this.timestamp = timestamp;
    }

    public String getMealName() { return mealName; }
    public int getCalories() { return calories; }
    public String getNotes() { return notes; }
    public String getTimestamp() { return timestamp; }
}

