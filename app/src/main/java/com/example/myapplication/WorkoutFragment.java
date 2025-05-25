package com.example.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkoutFragment extends Fragment {

    // Layout for duration picker (cardio)
    private LinearLayout durationLayout;
    // NumberPicker for duration (cardio)
    private NumberPicker durationPicker;

    // Spinner for workout category (bodyweight, powerlifting, cardio)
    private Spinner categorySelector, workoutSelector;
    // NumberPickers for reps and sets
    private NumberPicker repsPicker, setsPicker;
    // Checkbox to convert workout to points
    private CheckBox convertToPointsCheckbox;
    // Button to submit workout
    private Button submitWorkoutButton, clearHistoryButton;
    // TextView to display total points
    private TextView totalPointsText;

    // RecyclerView for workout history
    private RecyclerView recyclerView;
    // Adapter for RecyclerView
    private WorkoutAdapter workoutAdapter;
    // List to hold workout history
    private ArrayList<WorkoutEntry> workoutHistoryList;

    // Total points accumulated
    private int totalPoints = 0;

    // Model class for a workout entry
    public static class WorkoutEntry {
        private String workout;
        private int points;
        private String documentId;

        public WorkoutEntry() {}

        public WorkoutEntry(String workout, int points) {
            this.workout = workout;
            this.points = points;
        }

        public String getWorkout() {
            return workout;
        }

        public int getPoints() {
            return points;
        }

        public void setWorkout(String workout) {
            this.workout = workout;
        }

        public String getDocumentId() {
            return documentId;
        }

        public void setDocumentId(String documentId) {
            this.documentId = documentId;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_workout, container, false);

        // Initialize UI components
        categorySelector = view.findViewById(R.id.categorySelector);
        workoutSelector = view.findViewById(R.id.workoutSelector);
        repsPicker = view.findViewById(R.id.repsPicker);
        setsPicker = view.findViewById(R.id.setsPicker);
        convertToPointsCheckbox = view.findViewById(R.id.convertToPointsCheckbox);
        submitWorkoutButton = view.findViewById(R.id.submitWorkoutButton);
        clearHistoryButton = view.findViewById(R.id.clearHistoryButton);
        totalPointsText = view.findViewById(R.id.totalPointsText);
        recyclerView = view.findViewById(R.id.workoutHistoryRecyclerView);

        // Columns for reps and sets
        LinearLayout repsColumn = view.findViewById(R.id.repsColumn);
        LinearLayout setsColumn = view.findViewById(R.id.setsColumn);

        // Set range for reps picker
        repsPicker.setMinValue(1);
        repsPicker.setMaxValue(100);
        // Set range for sets picker
        setsPicker.setMinValue(1);
        setsPicker.setMaxValue(20);

        // Initialize duration picker layout and set range
        durationLayout = view.findViewById(R.id.durationLayout);
        durationPicker = view.findViewById(R.id.durationPicker);
        durationPicker.setMinValue(1);
        durationPicker.setMaxValue(120); // 2 hours max
        durationLayout.setVisibility(View.GONE);

        // Set up category selector with workout categories
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.workout_categories,
                android.R.layout.simple_spinner_item
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySelector.setAdapter(categoryAdapter);

        // Set default workout options (bodyweight)
        updateWorkoutSpinner(R.array.bodyweight_exercises);

        // Change workout spinner and input fields when category changes
        categorySelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Select array resource based on category
                int arrayRes;

                // Cardio is the third item (index 2)
                boolean isCardio = position == 2;

                // Show/hide reps and sets or duration fields based on category
                repsColumn.setVisibility(isCardio ? View.GONE : View.VISIBLE);
                setsColumn.setVisibility(isCardio ? View.GONE : View.VISIBLE);
                durationLayout.setVisibility(isCardio ? View.VISIBLE : View.GONE);

                // Select workout array based on category
                switch (position) {
                    case 0: arrayRes = R.array.bodyweight_exercises; break;
                    case 1: arrayRes = R.array.powerlifting_exercises; break;
                    case 2: arrayRes = R.array.cardio_exercises; break;
                    default: arrayRes = R.array.bodyweight_exercises;
                }

                // Update workout spinner with selected category
                updateWorkoutSpinner(arrayRes);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Initialize workout history list and adapter
        workoutHistoryList = new ArrayList<>();
        workoutAdapter = new WorkoutAdapter(workoutHistoryList, (entry, position) -> deleteWorkout(entry, position));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(workoutAdapter);

        // Set up submit and clear history buttons
        submitWorkoutButton.setOnClickListener(v -> submitWorkout());
        clearHistoryButton.setOnClickListener(v -> clearWorkoutHistory());

        // Load workouts from Firestore
        loadWorkouts();

        return view;
    }

    // Update the workout spinner with exercises from the selected array resource
    private void updateWorkoutSpinner(int arrayResId) {
        ArrayAdapter<CharSequence> workoutAdapter = ArrayAdapter.createFromResource(
                requireContext(), arrayResId, android.R.layout.simple_spinner_item);
        workoutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        workoutSelector.setAdapter(workoutAdapter);
    }

    // Handle workout submission
    private void submitWorkout() {
        // Get selected workout type
        String workoutType = workoutSelector.getSelectedItem().toString();
        // Check if points should be calculated
        boolean convertToPoints = convertToPointsCheckbox.isChecked();
        // Check if selected category is cardio
        boolean isCardio = categorySelector.getSelectedItemPosition() == 2;

        int points = 0;
        String workoutEntry;

        if (isCardio) {
            // Get duration for cardio workout
            int duration = durationPicker.getValue(); // in minutes
            // Get multiplier for selected workout
            double multiplier = getPointMultiplier(workoutType);
            // Calculate points if checkbox is checked
            points = convertToPoints ? (int) Math.round((duration / 10.0) * multiplier) : 0;

            // Build workout entry string
            workoutEntry = "\t" + workoutType + " - Duration: " + duration + " min" +
                    (convertToPoints ? ", Points: " + points : "");
        } else {
            // Get reps and sets for strength workout
            int reps = repsPicker.getValue();
            int sets = setsPicker.getValue();
            // Get multiplier for selected workout
            double multiplier = getPointMultiplier(workoutType);
            // Calculate points if checkbox is checked
            points = convertToPoints ? (int) Math.round(reps * sets * multiplier) : 0;

            // Build workout entry string
            workoutEntry = "\t" + workoutType + " - Reps: " + reps + ", Sets: " + sets +
                    (convertToPoints ? ", Points: " + points : "");
        }

        // Add points to total
        totalPoints += points;
        // Create new workout entry
        WorkoutEntry entry = new WorkoutEntry(workoutEntry, points);
        // Disable submit button to prevent double submits
        submitWorkoutButton.setEnabled(false);
        // Add workout to Firestore
        addWorkoutToFirestore(entry);
    }

    // Add a workout entry to Firestore
    private void addWorkoutToFirestore(WorkoutEntry entry) {
        // Get current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Get Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        // Prepare workout data map
        Map<String, Object> workoutMap = new HashMap<>();
        workoutMap.put("workout", entry.getWorkout());
        workoutMap.put("points", entry.getPoints());

        // Add workout to user's workouts collection
        db.collection("users").document(uid).collection("workouts")
                .add(workoutMap)
                .addOnSuccessListener(documentReference -> {
                    // Set document ID for entry
                    entry.setDocumentId(documentReference.getId());
                    // Add entry to history list at the top
                    workoutHistoryList.add(0, entry);
                    workoutAdapter.notifyItemInserted(0);

                    // Update user's total points in Firestore
                    DocumentReference userRef = db.collection("users").document(uid);
                    userRef.update("points", FieldValue.increment(entry.getPoints()))
                            .addOnSuccessListener(aVoid -> Log.d("Firestore", "User points updated"))
                            .addOnFailureListener(e -> Log.e("Firestore", "Error updating points", e));

                    // Update total points text
                    totalPointsText.setText("Total Points: " + totalPoints);
                    // Re-enable submit button
                    submitWorkoutButton.setEnabled(true);

                    // Show success toast
                    Activity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, "Workout added successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Log error and re-enable button
                    Log.e("Firestore", "Failed to add workout", e);
                    submitWorkoutButton.setEnabled(true);

                    // Show failure toast
                    Activity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, "Failed to submit workout", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Load workouts from Firestore and update UI
    private void loadWorkouts() {
        // Get current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Get Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        // Get all workout documents for user
        db.collection("users").document(uid).collection("workouts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Clear local history and reset points
                    workoutHistoryList.clear();
                    totalPoints = 0;

                    // Iterate through each workout document
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String workout = doc.getString("workout");
                        Long pointsLong = doc.getLong("points");

                        if (workout == null || pointsLong == null) continue;

                        int points = pointsLong.intValue();

                        // Create workout entry and add to list
                        WorkoutEntry entry = new WorkoutEntry(workout, points);
                        entry.setDocumentId(doc.getId());
                        workoutHistoryList.add(0, entry);

                        // Add points to total
                        totalPoints += points;
                    }

                    // Notify adapter and update points text
                    workoutAdapter.notifyDataSetChanged();
                    totalPointsText.setText("Total Points: " + totalPoints);
                });
    }

    // Delete a workout entry from Firestore and update UI
    private void deleteWorkout(WorkoutEntry entry, int position) {
        // Get current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Get Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        // Delete workout document
        db.collection("users").document(uid).collection("workouts")
                .document(entry.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove entry from local list and notify adapter
                    if (position >= 0 && position < workoutHistoryList.size()) {
                        workoutHistoryList.remove(position);
                        workoutAdapter.notifyItemRemoved(position);
                    }

                    // Subtract points from total and update UI
                    totalPoints -= entry.getPoints();
                    if (totalPoints < 0) totalPoints = 0;

                    totalPointsText.setText("Total Points: " + totalPoints);

                    // Update user's points in Firestore
                    DocumentReference userRef = db.collection("users").document(uid);
                    userRef.update("points", FieldValue.increment(-entry.getPoints()))
                            .addOnSuccessListener(aVoid2 -> Log.d("Firestore", "User points updated after workout deletion"))
                            .addOnFailureListener(e -> Log.e("Firestore", "Error updating points after deletion", e));

                    // Show success toast
                    Activity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, "Workout deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Show failure toast
                    Activity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, "Failed to delete workout", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Clear all workout history for the user
    private void clearWorkoutHistory() {
        // Get current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Get Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        // Track total points to deduct
        AtomicInteger totalPointsToDeduct = new AtomicInteger(0);

        // Get all workout documents for user
        db.collection("users").document(uid).collection("workouts")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Create a batch to delete all workouts
                    WriteBatch batch = db.batch();

                    // Add each workout to batch delete and sum points
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Long pointsLong = doc.getLong("points");
                        if (pointsLong != null) {
                            int points = pointsLong.intValue();
                            totalPointsToDeduct.addAndGet(points);
                        }
                        batch.delete(doc.getReference());
                    }

                    // Commit batch delete
                    batch.commit().addOnSuccessListener(aVoid -> {
                        // Clear local list and update UI
                        workoutHistoryList.clear();
                        workoutAdapter.notifyDataSetChanged();
                        totalPoints = 0;
                        totalPointsText.setText("Total Points: 0");

                        // Update user's points in Firestore
                        DocumentReference userRef = db.collection("users").document(uid);
                        userRef.update("points", FieldValue.increment(-totalPointsToDeduct.get()))
                                .addOnSuccessListener(aVoid2 -> Log.d("Firestore", "User points updated after clearing history"))
                                .addOnFailureListener(e -> Log.e("Firestore", "Error updating points after clearing history", e));

                        // Show success toast
                        Activity activity = getActivity();
                        if (activity != null) {
                            Toast.makeText(activity, "All workouts cleared", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    // Get point multiplier for a given workout name
    private double getPointMultiplier(String workoutName) {
        switch (workoutName) {
            case "Push-ups":
            case "Pullups":
            case "Dips":
            case "Squats":
            case "Core":
                return 0.2;

            case "Bench Press":
                return 2.0;

            case "Barbell Squats":
                return 2.0;

            case "Deadlift":
                return 2.0;

            case "Walking":
                return 4.0;

            case "Joggling/Running":
            case "Hiking":
            case "Swimming/Rowing":
            case "Biking":
            case "Other Sports":
            case "Other Cardio":
                return 8.0;

            default:
                return 1.0;
        }
    }
}
