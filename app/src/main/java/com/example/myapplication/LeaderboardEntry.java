package com.example.myapplication;

public class LeaderboardEntry {
    private String email;
    private long points;

    public LeaderboardEntry() {}

    public LeaderboardEntry(String email, long points) {
        this.email = email;
        this.points = points;
    }

    public String getEmail() {
        return email;
    }

    public long getPoints() {
        return points;
    }
}
