package com.example.myapplication;

/**
 * Class representing a single entry in the leaderboard.
 * Stores a user's name (used as email) and their corresponding point total.
 */
public class LeaderboardEntry {
    // Fields to store the user's name (used as email) and points
    private String name;
    private long points;

    /**
     * Default constructor required for Firebase and other serialization libraries.
     */
    public LeaderboardEntry() {}

    /**
     * Constructor to initialize a leaderboard entry with a name and points.
     *
     * @param name   The user's name or email
     * @param points The user's points for the leaderboard
     */
    public LeaderboardEntry(String name, long points) {
        this.name = name;
        this.points = points;
    }

    /**
     * Getter method to retrieve the user's name or email.
     *
     * @return The name or email of the user
     */
    public String getEmail() {
        return name;
    }

    /**
     * Getter method to retrieve the user's points.
     *
     * @return The number of points the user has
     */
    public long getPoints() {
        return points;
    }
}
