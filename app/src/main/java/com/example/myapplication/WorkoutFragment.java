package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class WorkoutFragment extends Fragment {
    private Spinner spinner1, spinner2, spinner3;
    private Button submitButton;
    private RecyclerView recyclerView;
    private WorkoutAdapter workoutAdapter;
    private ArrayList<String> workoutHistory;

    private ArrayList<String> workoutArray;
    private ArrayList<Integer> repsArray;
    private ArrayList<Integer> setsArray;

    public WorkoutFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_workout, container, false);

        // Initialize workout history
        workoutHistory = new ArrayList<>();

        // Initialize UI elements
        spinner1 = view.findViewById(R.id.workoutSelector);
        spinner2 = view.findViewById(R.id.repsSelector);
        spinner3 = view.findViewById(R.id.setsSelector);
        submitButton = view.findViewById(R.id.submitWorkoutButton);
        recyclerView = view.findViewById(R.id.workoutHistoryRecyclerView);

        // Initialize lists
        initializeWorkoutData();

        // Setup spinners
        setupSpinners();

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        workoutAdapter = new WorkoutAdapter(workoutHistory);
        recyclerView.setAdapter(workoutAdapter);

        // Load saved workouts
        loadWorkouts();

        // Handle Submit Button Click
        submitButton.setOnClickListener(v -> addWorkout());

        return view;
    }

    private void initializeWorkoutData() {
        workoutArray = new ArrayList<>();
        workoutArray.add("Select a workout");
        workoutArray.add("Bench Press");
        workoutArray.add("Squats");
        workoutArray.add("Deadlifts");
        workoutArray.add("Pull-ups");
        workoutArray.add("Lunges");
        workoutArray.add("Push-ups");
        workoutArray.add("Planks");
        workoutArray.add("Jump Rope");

        repsArray = new ArrayList<>();
        repsArray.add(0);
        repsArray.add(5);
        repsArray.add(8);
        repsArray.add(10);
        repsArray.add(12);
        repsArray.add(15);
        repsArray.add(20);

        setsArray = new ArrayList<>();
        setsArray.add(0);
        setsArray.add(3);
        setsArray.add(4);
        setsArray.add(5);
    }

    private void setupSpinners() {
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, workoutArray);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner1.setAdapter(adapter1);

        ArrayAdapter<Integer> adapter2 = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, repsArray);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(adapter2);

        ArrayAdapter<Integer> adapter3 = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, setsArray);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner3.setAdapter(adapter3);
    }

    private void addWorkout() {
        String selectedWorkout = spinner1.getSelectedItem().toString();
        int selectedReps = (Integer) spinner2.getSelectedItem();
        int selectedSets = (Integer) spinner3.getSelectedItem();

        if (selectedWorkout.equals("Select a workout") || selectedReps == 0 || selectedSets == 0) {
            Toast.makeText(getActivity(), "Please select a valid workout, reps, and sets", Toast.LENGTH_SHORT).show();
            return;
        }

        String workoutEntry = selectedWorkout + " - " + selectedReps + " reps x " + selectedSets + " sets";
        workoutHistory.add(0, workoutEntry);
        workoutAdapter.notifyDataSetChanged();

        saveWorkout(workoutEntry);
    }

    private void saveWorkout(String workout) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("WorkoutPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> workoutSet = prefs.getStringSet("workouts", new HashSet<>());
        workoutSet.add(workout);

        editor.putStringSet("workouts", workoutSet);
        editor.apply();
    }

    private void loadWorkouts() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("WorkoutPrefs", Context.MODE_PRIVATE);
        Set<String> workoutSet = prefs.getStringSet("workouts", new HashSet<>());

        workoutHistory.clear();
        workoutHistory.addAll(workoutSet);
        workoutAdapter.notifyDataSetChanged();
    }
}
