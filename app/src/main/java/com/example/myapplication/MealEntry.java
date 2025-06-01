package com.example.myapplication;

/**
 * Model class representing a meal entry.
 * Contains information about the meal name, calorie count, notes, and timestamp.
 */
public class MealEntry {

    // Name of the meal
    private String mealName;

    // Number of calories in the meal
    private int calories;

    // Additional notes about the meal
    private String notes;

    // Timestamp of when the meal was recorded
    private String timestamp;

    /**
     * Default no-argument constructor.
     * Required for Firebase deserialization.
     */
    public MealEntry() {}

    /**
     * Constructor to initialize all fields of a meal entry.
     *
     * @param mealName  Name of the meal
     * @param calories  Number of calories
     * @param notes     Additional notes
     * @param timestamp Timestamp of the entry
     */
    public MealEntry(String mealName, int calories, String notes, String timestamp) {
        this.mealName = mealName;
        this.calories = calories;
        this.notes = notes;
        this.timestamp = timestamp;
    }

    /**
     * Returns the name of the meal.
     *
     * @return Meal name
     */
    public String getMealName() {
        return mealName;
    }

    /**
     * Returns the number of calories.
     *
     * @return Calorie count
     */
    public int getCalories() {
        return calories;
    }

    /**
     * Returns any additional notes about the meal.
     *
     * @return Meal notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Returns the timestamp when the meal was recorded.
     *
     * @return Timestamp string
     */
    public String getTimestamp() {
        return timestamp;
    }
}
