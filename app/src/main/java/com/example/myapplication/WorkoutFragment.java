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

    private LinearLayout durationLayout;
    private NumberPicker durationPicker;

    private Spinner categorySelector, workoutSelector;
    private NumberPicker repsPicker, setsPicker;
    private CheckBox convertToPointsCheckbox;
    private Button submitWorkoutButton, clearHistoryButton;
    private TextView totalPointsText;

    private RecyclerView recyclerView;
    private WorkoutAdapter workoutAdapter;
    private ArrayList<WorkoutEntry> workoutHistoryList;

    private int totalPoints = 0;

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
        View view = inflater.inflate(R.layout.fragment_workout, container, false);

        categorySelector = view.findViewById(R.id.categorySelector);
        workoutSelector = view.findViewById(R.id.workoutSelector);
        repsPicker = view.findViewById(R.id.repsPicker);
        setsPicker = view.findViewById(R.id.setsPicker);
        convertToPointsCheckbox = view.findViewById(R.id.convertToPointsCheckbox);
        submitWorkoutButton = view.findViewById(R.id.submitWorkoutButton);
        clearHistoryButton = view.findViewById(R.id.clearHistoryButton);
        totalPointsText = view.findViewById(R.id.totalPointsText);
        recyclerView = view.findViewById(R.id.workoutHistoryRecyclerView);

        LinearLayout repsColumn = view.findViewById(R.id.repsColumn);
        LinearLayout setsColumn = view.findViewById(R.id.setsColumn);

        //For Sets and Reps
        repsPicker.setMinValue(1);
        repsPicker.setMaxValue(100);
        setsPicker.setMinValue(1);
        setsPicker.setMaxValue(20);

        //For Time
        durationLayout = view.findViewById(R.id.durationLayout);
        durationPicker = view.findViewById(R.id.durationPicker);
        durationPicker.setMinValue(1);
        durationPicker.setMaxValue(120); // 2 hours max
        durationLayout.setVisibility(View.GONE);


        // Set up category selector
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.workout_categories,
                android.R.layout.simple_spinner_item
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySelector.setAdapter(categoryAdapter);

        // Set default workout options
        updateWorkoutSpinner(R.array.bodyweight_exercises);

        // Change workout spinner when category changes
        categorySelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int arrayRes;

                boolean isCardio = position == 2;

                repsColumn.setVisibility(isCardio ? View.GONE : View.VISIBLE);
                setsColumn.setVisibility(isCardio ? View.GONE : View.VISIBLE);
                durationLayout.setVisibility(isCardio ? View.VISIBLE : View.GONE);

                switch (position) {
                    case 0: arrayRes = R.array.bodyweight_exercises; break;
                    case 1: arrayRes = R.array.powerlifting_exercises; break;
                    case 2: arrayRes = R.array.cardio_exercises; break;
                    default: arrayRes = R.array.bodyweight_exercises;
                }

                updateWorkoutSpinner(arrayRes);
            }



            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        workoutHistoryList = new ArrayList<>();
        workoutAdapter = new WorkoutAdapter(workoutHistoryList, (entry, position) -> deleteWorkout(entry, position));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(workoutAdapter);

        submitWorkoutButton.setOnClickListener(v -> submitWorkout());
        clearHistoryButton.setOnClickListener(v -> clearWorkoutHistory());

        loadWorkouts();

        return view;
    }

    private void updateWorkoutSpinner(int arrayResId) {
        ArrayAdapter<CharSequence> workoutAdapter = ArrayAdapter.createFromResource(
                requireContext(), arrayResId, android.R.layout.simple_spinner_item);
        workoutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        workoutSelector.setAdapter(workoutAdapter);
    }

    private void submitWorkout() {
        String workoutType = workoutSelector.getSelectedItem().toString();
        boolean convertToPoints = convertToPointsCheckbox.isChecked();
        boolean isCardio = categorySelector.getSelectedItemPosition() == 2;

        int points = 0;
        String workoutEntry;

        if (isCardio) {
            int duration = durationPicker.getValue(); // in minutes
            double multiplier = getPointMultiplier(workoutType);
            points = convertToPoints ? (int) Math.round((duration / 10.0) * multiplier) : 0;

            workoutEntry = "\t" + workoutType + " - Duration: " + duration + " min" +
                    (convertToPoints ? ", Points: " + points : "");
        } else {
            int reps = repsPicker.getValue();
            int sets = setsPicker.getValue();
            double multiplier = getPointMultiplier(workoutType);
            points = convertToPoints ? (int) Math.round(reps * sets * multiplier) : 0;

            workoutEntry = "\t" + workoutType + " - Reps: " + reps + ", Sets: " + sets +
                    (convertToPoints ? ", Points: " + points : "");
        }

        totalPoints += points;
        WorkoutEntry entry = new WorkoutEntry(workoutEntry, points);
        submitWorkoutButton.setEnabled(false);
        addWorkoutToFirestore(entry);
    }


    private void addWorkoutToFirestore(WorkoutEntry entry) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        Map<String, Object> workoutMap = new HashMap<>();
        workoutMap.put("workout", entry.getWorkout());
        workoutMap.put("points", entry.getPoints());

        db.collection("users").document(uid).collection("workouts")
                .add(workoutMap)
                .addOnSuccessListener(documentReference -> {
                    entry.setDocumentId(documentReference.getId());
                    workoutHistoryList.add(0, entry);
                    workoutAdapter.notifyItemInserted(0);

                    DocumentReference userRef = db.collection("users").document(uid);
                    userRef.update("points", FieldValue.increment(entry.getPoints()))
                            .addOnSuccessListener(aVoid -> Log.d("Firestore", "User points updated"))
                            .addOnFailureListener(e -> Log.e("Firestore", "Error updating points", e));

                    totalPointsText.setText("Total Points: " + totalPoints);

                    submitWorkoutButton.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to add workout", e);
                    submitWorkoutButton.setEnabled(true);

                    Activity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, "Failed to submit workout", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadWorkouts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("users").document(uid).collection("workouts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    workoutHistoryList.clear();
                    totalPoints = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String workout = doc.getString("workout");
                        Long pointsLong = doc.getLong("points");

                        if (workout == null || pointsLong == null) continue;

                        int points = pointsLong.intValue();

                        WorkoutEntry entry = new WorkoutEntry(workout, points);
                        entry.setDocumentId(doc.getId());
                        workoutHistoryList.add(0, entry);

                        totalPoints += points;
                    }

                    workoutAdapter.notifyDataSetChanged();
                    totalPointsText.setText("Total Points: " + totalPoints);
                });
    }

    private void deleteWorkout(WorkoutEntry entry, int position) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("users").document(uid).collection("workouts")
                .document(entry.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (position >= 0 && position < workoutHistoryList.size()) {
                        workoutHistoryList.remove(position);
                        workoutAdapter.notifyItemRemoved(position);
                    }

                    totalPoints -= entry.getPoints();
                    if (totalPoints < 0) totalPoints = 0;

                    totalPointsText.setText("Total Points: " + totalPoints);

                    DocumentReference userRef = db.collection("users").document(uid);
                    userRef.update("points", FieldValue.increment(-entry.getPoints()))
                            .addOnSuccessListener(aVoid2 -> Log.d("Firestore", "User points updated after workout deletion"))
                            .addOnFailureListener(e -> Log.e("Firestore", "Error updating points after deletion", e));

                    Activity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, "Workout deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Activity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, "Failed to delete workout", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void clearWorkoutHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        AtomicInteger totalPointsToDeduct = new AtomicInteger(0);

        db.collection("users").document(uid).collection("workouts")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Long pointsLong = doc.getLong("points");
                        if (pointsLong != null) {
                            int points = pointsLong.intValue();
                            totalPointsToDeduct.addAndGet(points);
                        }
                        batch.delete(doc.getReference());
                    }

                    batch.commit().addOnSuccessListener(aVoid -> {
                        workoutHistoryList.clear();
                        workoutAdapter.notifyDataSetChanged();
                        totalPoints = 0;
                        totalPointsText.setText("Total Points: 0");

                        DocumentReference userRef = db.collection("users").document(uid);
                        userRef.update("points", FieldValue.increment(-totalPointsToDeduct.get()))
                                .addOnSuccessListener(aVoid2 -> Log.d("Firestore", "User points updated after clearing history"))
                                .addOnFailureListener(e -> Log.e("Firestore", "Error updating points after clearing history", e));

                        Activity activity = getActivity();
                        if (activity != null) {
                            Toast.makeText(activity, "All workouts cleared", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }
    private double getPointMultiplier(String workoutName) {
        switch (workoutName) {
            case "Push-ups":
            case "Pullups":
            case "Dips":
            case "Squats":
            case "Core":
                return 1.0;

            case "Bench Press":
                return 2.0;

            case "Barbell Squats":
                return 2.0;

            case "Deadlift":
                return 2.0;

            case "Walking":
                return 1.0;

            case "Joggling/Running":
            case "Hiking":
            case "Swimming/Rowing":
            case "Biking":
            case "Other Sports":
            case "Other Cardio":
                return 2.0;

            default:
                return 1.0;
        }
    }

}


