package com.example.myapplication;

// Class representing a single entry in the leaderboard
public class LeaderboardEntry {
    // Fields to store the user's name (used as email) and points
    private String name;
    private long points;

    // Default constructor required for Firebase or other serialization libraries
    public LeaderboardEntry() {}

    // Constructor to initialize a leaderboard entry with name and points
    public LeaderboardEntry(String name, long points) {
        this.name = name;
        this.points = points;
    }

    // Getter method to retrieve the user's name
    public String getEmail() {
        return name;
    }

    // Getter method to retrieve the user's points
    public long getPoints() {
        return points;
    }
}
