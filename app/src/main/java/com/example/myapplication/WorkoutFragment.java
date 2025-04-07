package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class WorkoutFragment extends Fragment {
    private Spinner workoutSpinner;
    private NumberPicker repsPicker, setsPicker;
    private Button submitButton, clearButton;
    private RecyclerView recyclerView;
    private WorkoutAdapter workoutAdapter;
    private ArrayList<String> workoutHistory;

    private ArrayList<String> workoutArray;

    public WorkoutFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_workout, container, false);

        workoutHistory = new ArrayList<>();

        workoutSpinner = view.findViewById(R.id.workoutSelector);
        repsPicker = view.findViewById(R.id.repsSelector);
        setsPicker = view.findViewById(R.id.setsSelector);
        submitButton = view.findViewById(R.id.submitWorkoutButton);
        clearButton = view.findViewById(R.id.clearHistoryButton);
        recyclerView = view.findViewById(R.id.workoutHistoryRecyclerView);

        initializeWorkoutData();
        setupSpinners();
        setupNumberPickers();

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        workoutAdapter = new WorkoutAdapter(workoutHistory);
        recyclerView.setAdapter(workoutAdapter);

        loadWorkouts();

        submitButton.setOnClickListener(v -> addWorkout());
        clearButton.setOnClickListener(v -> clearWorkoutHistory());

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
    }

    private void setupSpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, workoutArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        workoutSpinner.setAdapter(adapter);
    }

    private void setupNumberPickers() {
        repsPicker.setMinValue(1);
        repsPicker.setMaxValue(20);
        repsPicker.setWrapSelectorWheel(true);

        setsPicker.setMinValue(1);
        setsPicker.setMaxValue(10);
        setsPicker.setWrapSelectorWheel(true);
    }

    private void addWorkout() {
        String selectedWorkout = workoutSpinner.getSelectedItem().toString();
        int selectedReps = repsPicker.getValue();
        int selectedSets = setsPicker.getValue();

        if (selectedWorkout.equals("Select a workout")) {
            Toast.makeText(getActivity(), "Please select a valid workout", Toast.LENGTH_SHORT).show();
            return;
        }

        String workoutEntry = selectedWorkout + " - " + selectedReps + " reps x " + selectedSets + " sets";
        workoutHistory.add(0, workoutEntry);
        workoutAdapter.notifyDataSetChanged();

        saveWorkout(workoutEntry);
    }

    private void saveWorkout(String workout) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getActivity(), "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("users")
                .document(uid)
                .collection("workouts")
                .add(new WorkoutEntry(workout))
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(getActivity(), "Workout saved to cloud!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getActivity(), "Failed to save workout", Toast.LENGTH_SHORT).show());
    }

    private void loadWorkouts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getActivity(), "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("users")
                .document(uid)
                .collection("workouts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        workoutHistory.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String workout = doc.getString("workout");
                            if (workout != null) {
                                workoutHistory.add(0, workout);
                            }
                        }
                        workoutAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getActivity(), "Failed to load workouts", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void clearWorkoutHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getActivity(), "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("users")
                .document(uid)
                .collection("workouts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            doc.getReference().delete();
                        }
                        workoutHistory.clear();
                        workoutAdapter.notifyDataSetChanged();
                        Toast.makeText(getActivity(), "Workout history cleared!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "Failed to clear workouts", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Helper class for Firestore document
    public static class WorkoutEntry {
        private String workout;

        public WorkoutEntry() {}

        public WorkoutEntry(String workout) {
            this.workout = workout;
        }

        public String getWorkout() {
            return workout;
        }

        public void setWorkout(String workout) {
            this.workout = workout;
        }
    }
}
