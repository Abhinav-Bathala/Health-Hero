package com.example.myapplication;

public class LeaderboardEntry {
    private String name;
    private long points;

    public LeaderboardEntry() {}

    public LeaderboardEntry(String name, long points) {
        this.name = name;
        this.points = points;
    }

    public String getEmail() {
        return name;
    }

    public long getPoints() {
        return points;
    }
}