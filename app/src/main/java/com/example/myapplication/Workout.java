package com.example.myapplication;

// Class representing a workout with a name, repetitions, and sets
public class Workout {
    // Name of the workout exercise
    private String workoutName;
    // Number of repetitions per set
    private int reps;
    // Number of sets to perform
    private int sets;

    // Constructor to initialize a Workout object with name, reps, and sets
    public Workout(String workoutName, int reps, int sets) {
        this.workoutName = workoutName;
        this.reps = reps;
        this.sets = sets;
    }

    // Getter method to retrieve the workout name
    public String getWorkoutName() {
        return workoutName;
    }

    // Getter method to retrieve the number of repetitions
    public int getReps() {
        return reps;
    }

    // Getter method to retrieve the number of sets
    public int getSets() {
        return sets;
    }

}
