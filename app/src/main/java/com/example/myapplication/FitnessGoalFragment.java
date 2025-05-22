package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class FitnessGoalFragment extends Fragment {

    private RadioGroup goalRadioGroup, genderRadioGroup;
    private EditText etAge, etHeight, etWeight, etCaloricIntake,
            etWeightGoalDiff, etWeightUpdate;
    private Spinner spinnerActivityLevel;
    private Button btnSubmit, btnUpdateProgress;
    private TextView tvRecommendation, tvInitial, tvProgressPercent;

    private FirebaseFirestore db;

    /** Activity‑level labels shown in the Spinner and used in the multiplier switch. */
    private static final String[] ACTIVITY_OPTIONS = {
            "Sedentary",
            "Lightly Active",
            "Active",
            "Very Active"
    };

    public FitnessGoalFragment() {
        super(R.layout.fragment_fitness_goal);
    }

    // ─────────────────────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────────────────────

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {


        super.onViewCreated(view, savedInstanceState);




        // ‑‑‑ view binding ------------------------------------------------------
        goalRadioGroup     = view.findViewById(R.id.goalRadioGroup);
        genderRadioGroup   = view.findViewById(R.id.genderRadioGroup);
        etAge              = view.findViewById(R.id.etAge);
        etHeight           = view.findViewById(R.id.etHeight);
        etWeight           = view.findViewById(R.id.etWeight);
        etWeightGoalDiff   = view.findViewById(R.id.etWeightGoalDiff);
        etCaloricIntake    = view.findViewById(R.id.etCaloricIntake);
        etWeightUpdate     = view.findViewById(R.id.etWeightUpdate);
        spinnerActivityLevel = view.findViewById(R.id.spinnerActivityLevel);
        btnSubmit            = view.findViewById(R.id.btnSubmit);
        btnUpdateProgress    = view.findViewById(R.id.btnUpdateProgress);
        tvRecommendation     = view.findViewById(R.id.tvRecommendation);
        tvInitial            = view.findViewById(R.id.tvInitial);
        tvProgressPercent    = view.findViewById(R.id.tvProgressPercent);

        // ‑‑‑ Firestore ---------------------------------------------------------
        db = FirebaseFirestore.getInstance();

        // ‑‑‑ Spinner: plug in choices so it never crashes ----------------------
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                ACTIVITY_OPTIONS
        );
        activityAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerActivityLevel.setAdapter(activityAdapter);

        // ‑‑‑ Load any previously saved data -----------------------------------
        loadExistingData();

        // ‑‑‑ Button listeners --------------------------------------------------
        btnSubmit.setOnClickListener(v -> handleSubmit());
        btnUpdateProgress.setOnClickListener(v -> handleProgressUpdate());
    }

    // ─────────────────────────────────────────────────────────────
    // LOAD EXISTING USER DATA
    // ─────────────────────────────────────────────────────────────

    private void loadExistingData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                return;
            }

            // Show TDEE + calories if they were stored earlier
            Long tdeeVal = documentSnapshot.getLong("tdee");
            Long recCal  = documentSnapshot.getLong("recommendedCalories");
            String goal  = documentSnapshot.getString("goal");
            if (tdeeVal != null && recCal != null && goal != null) {
                String msg = "Your estimated Total Daily Energy Expenditure: "
                        + tdeeVal + " kcal/day\nRecommended Intake for "
                        + goal + ": " + recCal + " kcal/day";
                tvRecommendation.setText(msg);
            } else {
                tvRecommendation.setText("No recommendation data found. Please submit your goal.");
            }

            // Initial stats block
            StringBuilder stats = new StringBuilder();
            Long ageVal        = documentSnapshot.getLong("age");
            Double heightVal   = documentSnapshot.getDouble("height");
            Double weightVal   = documentSnapshot.getDouble("weight");
            String activity    = documentSnapshot.getString("activityLevel");
            String timeMsg     = documentSnapshot.getString("timeToReachGoal");
            Double weightDiff  = documentSnapshot.getDouble("weightGoalDiff");

            if (ageVal != null && heightVal != null && weightVal != null && activity != null) {
                stats.append("Initial Stats:\n")
                        .append("Age: ").append(ageVal).append(" yrs\n")
                        .append("Height: ").append(heightVal).append(" cm\n")
                        .append("Weight: ").append(weightVal).append(" kg\n")
                        .append("Activity Level: ").append(activity);

                if (weightDiff != null) {
                    if (Math.abs(weightDiff) < 0.5) {
                        stats.append("\nDesired Weight Change: Goal reached!");
                    } else {
                        double roundedWeightDiff = Math.round(Math.abs(weightDiff) * 10.0) / 10.0;
                        String sign = weightDiff > 0 ? "+" : "-";
                        stats.append("\nDesired Weight Change: ").append(sign).append(roundedWeightDiff).append(" kg");
                    }
                }



            } else {
                stats.append("No initial stats found.");
            }

            if (timeMsg != null) stats.append("\n\n").append(timeMsg);
            tvInitial.setText(stats.toString());

            // Progress %
            Double originalGoal   = documentSnapshot.getDouble("originalWeightGoalDiff");
            Double currentGoalDiff= documentSnapshot.getDouble("weightGoalDiff");
            if (originalGoal != null && currentGoalDiff != null && originalGoal != 0) {
                double pct = 100 * (originalGoal - currentGoalDiff) / originalGoal;
                pct = Math.max(0, Math.min(100, pct));
                tvProgressPercent.setText(String.format("Goal Progress: %.1f%%", pct));
            } else {
                tvProgressPercent.setText("Goal Progress: 0.0%");
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
        );
    }

    // ─────────────────────────────────────────────────────────────
    // SUBMIT (CREATE/UPDATE GOAL)
    // ─────────────────────────────────────────────────────────────

    private void handleSubmit() {
        // grab selections ------------------------------------------------------
        int   selectedGoalId   = goalRadioGroup.getCheckedRadioButtonId();
        int   selectedGenderId = genderRadioGroup.getCheckedRadioButtonId();

        String goal = selectedGoalId == R.id.radioBulk ? "Bulking" :
                selectedGoalId == R.id.radioCut  ? "Cutting" : "";

        String gender = selectedGenderId == R.id.radioMale   ? "Male" :
                selectedGenderId == R.id.radioFemale ? "Female" : "";

        String ageStr           = etAge.getText().toString().trim();
        String heightStr        = etHeight.getText().toString().trim();
        String weightStr        = etWeight.getText().toString().trim();
        String activityLevel    = spinnerActivityLevel.getSelectedItem().toString();
        String weightGoalDiffStr= etWeightGoalDiff.getText().toString().trim();

        if (goal.isEmpty() || gender.isEmpty() || ageStr.isEmpty() ||
                heightStr.isEmpty() || weightStr.isEmpty() || weightGoalDiffStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // numeric parsing ------------------------------------------------------
        int age;
        float height, weight, weightGoalDiff;
        try {
            age = Integer.parseInt(ageStr);
            height = Float.parseFloat(heightStr);
            weight = Float.parseFloat(weightStr);
            weightGoalDiff = Float.parseFloat(weightGoalDiffStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number input", Toast.LENGTH_SHORT).show();
            return;
        }

        // calculations ---------------------------------------------------------
        double bmr = gender.equals("Male")
                ? 10 * weight + 6.25 * height - 5 * age + 5
                : 10 * weight + 6.25 * height - 5 * age - 161;

        double activityMultiplier;
        switch (activityLevel) {
            case "Lightly Active":
                activityMultiplier = 1.375;
                break;
            case "Active":
                activityMultiplier = 1.55;
                break;
            case "Very Active":
                activityMultiplier = 1.725;
                break;
            case "Sedentary":
            default:
                activityMultiplier = 1.2;
                break;
        }

        double tdee = bmr * activityMultiplier;
        int recommendedCalories = goal.equals("Bulking")
                ? (int) (tdee + 500)
                : (int) (tdee - 500);

        double daysToReachGoal = (weightGoalDiff * 7700) / 500; // 500 kcal deficit/surplus a day
        String timeToReachGoal = "It should take approximately " +
                Math.round(daysToReachGoal) +
                " days to reach your weight goal in a healthy manner.";

        // output to UI ---------------------------------------------------------
        String message = "Your estimated Total Daily Energy Expenditure: " +
                (int) tdee + " kcal/day\n" +
                "Recommended Intake for " + goal + ": " + recommendedCalories + " kcal/day\n" +
                timeToReachGoal;

        tvRecommendation.setText(message);
        tvInitial.setText(timeToReachGoal);

        // save to Firestore ----------------------------------------------------
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        Map<String, Object> userData = new HashMap<>();
        userData.put("goal", goal);
        userData.put("gender", gender);
        userData.put("age", age);
        userData.put("height", height);
        userData.put("weight", weight);
        userData.put("activityLevel", activityLevel);
        userData.put("tdee", (int) tdee);
        userData.put("recommendedCalories", recommendedCalories);
        userData.put("message", message);
        userData.put("timeToReachGoal", timeToReachGoal);
        userData.put("weightGoalDiff", (double) weightGoalDiff);
        userData.put("originalWeightGoalDiff", weightGoalDiff);
        userData.put("goalProgressPercent", 0.0);




        db.collection("users").document(uid)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), "Data saved successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE PROGRESS
    // ─────────────────────────────────────────────────────────────

    private void handleProgressUpdate() {
        String updateStr = etWeightUpdate.getText().toString().trim();
        if (updateStr.isEmpty()) {
            Toast.makeText(getContext(), "Please enter your weight change", Toast.LENGTH_SHORT).show();
            return;
        }

        float weightUpdate;
        try {
            weightUpdate = Float.parseFloat(updateStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(getContext(), "No goal data found to update", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Double currentWeightDiff = documentSnapshot.getDouble("weightGoalDiff");
                    Double originalGoal = documentSnapshot.getDouble("originalWeightGoalDiff");

                    if (currentWeightDiff == null) return;

                    // Initialize originalGoal if null
                    if (originalGoal == null) {
                        originalGoal = currentWeightDiff;
                        Map<String, Object> originSet = new HashMap<>();
                        originSet.put("originalWeightGoalDiff", originalGoal);
                        db.collection("users").document(uid).set(originSet, SetOptions.merge());
                    }

                    // Check if already completed
                    double percentComplete = 100 * (originalGoal - currentWeightDiff) / originalGoal;
                    percentComplete = Math.max(0, Math.min(100, percentComplete));

                    if (percentComplete >= 100) {
                        Toast.makeText(getContext(), "Set a new goal now and update your stats!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Apply update
                    double newWeightDiff = currentWeightDiff - weightUpdate;
                    double updatedPercent = 100 * (originalGoal - newWeightDiff) / originalGoal;
                    updatedPercent = Math.max(0, Math.min(100, updatedPercent));

                    tvProgressPercent.setText(String.format("Goal Progress: %.1f%%", updatedPercent));

                    Map<String, Object> updateMap = new HashMap<>();
                    updateMap.put("weightGoalDiff", newWeightDiff);

                    double percentChangeThisUpdate = (weightUpdate / originalGoal) * 100;
                    int pointsEarned = Math.round((float) percentChangeThisUpdate);

                    db.collection("users").document(uid)
                            .set(updateMap, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(),
                                        "Progress updated! Remaining: " + newWeightDiff + " kg",
                                        Toast.LENGTH_SHORT).show();
                                awardPoints(uid, pointsEarned);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Update failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());

                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error accessing Firestore", Toast.LENGTH_SHORT).show());
    }


    // ─────────────────────────────────────────────────────────────
    // AWARD POINTS
    // ─────────────────────────────────────────────────────────────

    private void awardPoints(String uid, int pointsToAdd) {
        DocumentReference userRef = db.collection("users").document(uid);

        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(userRef);
                    long currentPoints = snapshot.contains("points") && snapshot.getLong("points") != null
                            ? snapshot.getLong("points") : 0;
                    transaction.update(userRef, "points", currentPoints + pointsToAdd);
                    return null;
                }).addOnSuccessListener(unused ->
                        Log.d("FitnessGoalFragment", pointsToAdd + " points awarded."))
                .addOnFailureListener(e ->
                        Log.e("FitnessGoalFragment", "Failed to award points", e));
    }
}
