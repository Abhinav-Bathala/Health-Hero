package com.example.myapplication;

/**
 * Class representing a workout with a name, repetitions, and sets.
 */
public class Workout {
    // Name of the workout exercise
    private String workoutName;
    // Number of repetitions per set
    private int reps;
    // Number of sets to perform
    private int sets;

    /**
     * Constructor to initialize a Workout object with the specified name, reps, and sets.
     *
     * @param workoutName The name of the workout exercise.
     * @param reps Number of repetitions per set.
     * @param sets Number of sets to perform.
     */
    public Workout(String workoutName, int reps, int sets) {
        this.workoutName = workoutName;
        this.reps = reps;
        this.sets = sets;
    }

    /**
     * Gets the name of the workout exercise.
     *
     * @return The workout name.
     */
    public String getWorkoutName() {
        return workoutName;
    }

    /**
     * Gets the number of repetitions per set.
     *
     * @return The number of reps.
     */
    public int getReps() {
        return reps;
    }

    /**
     * Gets the number of sets to perform.
     *
     * @return The number of sets.
     */
    public int getSets() {
        return sets;
    }

}
