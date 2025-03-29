package com.example.myapplication;

public class Workout {
    private String workoutName;
    private int reps;
    private int sets;
    public Workout(String workoutName, int reps, int sets) {
        this.workoutName = workoutName;
        this.reps = reps;
        this.sets = sets;
    }

    public String getWorkoutName() {
        return workoutName;
    }

    public int getReps() {
        return reps;
    }

    public int getSets() {
        return sets;
    }

}
