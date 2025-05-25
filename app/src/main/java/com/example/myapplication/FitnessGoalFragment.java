package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class FitnessGoalFragment extends Fragment {
    // UI components
    private RadioGroup goalRadioGroup, genderRadioGroup;
    private EditText etAge, etHeight, etWeight, etCaloricIntake,
            etWeightGoalDiff, etWeightUpdate;
    private Spinner spinnerActivityLevel;
    private Button btnSubmit, btnUpdateProgress;
    private TextView tvRecommendation, tvInitial, tvProgressPercent;

    private FirebaseFirestore db;

    private static final String[] ACTIVITY_OPTIONS = {
            "Sedentary",
            "Lightly Active",
            "Active",
            "Very Active"
    };

    public FitnessGoalFragment() {
        super(R.layout.fragment_fitness_goal);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        goalRadioGroup = view.findViewById(R.id.goalRadioGroup);
        genderRadioGroup = view.findViewById(R.id.genderRadioGroup);
        etAge = view.findViewById(R.id.etAge);
        etHeight = view.findViewById(R.id.etHeight);
        etWeight  = view.findViewById(R.id.etWeight);
        etWeightGoalDiff = view.findViewById(R.id.etWeightGoalDiff);
        etCaloricIntake = view.findViewById(R.id.etCaloricIntake);
        etWeightUpdate = view.findViewById(R.id.etWeightUpdate);
        spinnerActivityLevel = view.findViewById(R.id.spinnerActivityLevel);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnUpdateProgress = view.findViewById(R.id.btnUpdateProgress);
        tvRecommendation = view.findViewById(R.id.tvRecommendation);
        tvInitial = view.findViewById(R.id.tvInitial);
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent);

        db = FirebaseFirestore.getInstance();

        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                ACTIVITY_OPTIONS
        );
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivityLevel.setAdapter(activityAdapter);

        loadExistingData();

        btnSubmit.setOnClickListener(v -> handleSubmit());
        btnUpdateProgress.setOnClickListener(v -> handleProgressUpdate());
    }

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

            StringBuilder stats = new StringBuilder();
            Long ageVal        = documentSnapshot.getLong("age");
            Double heightVal   = documentSnapshot.getDouble("height");
            Double weightVal   = documentSnapshot.getDouble("originalWeight");
            String activity    = documentSnapshot.getString("activityLevel");
            String timeMsg     = documentSnapshot.getString("timeToReachGoal");
            Double weightDiff  = documentSnapshot.getDouble("originalWeightGoalDiff");

            if (ageVal != null && heightVal != null && weightVal != null && activity != null) {
                stats.append("Age: ").append(ageVal).append(" yrs\n")
                        .append("Height: ").append(heightVal).append(" cm\n")
                        .append("Weight: ").append(weightVal).append(" kg\n")
                        .append("Activity Level: ").append(activity);

                if (weightDiff != null) {
                    if (Math.abs(weightDiff) < 0.5) {
                        stats.append("\nDesired Weight Change: Goal reached!");
                    } else {
                        double roundedWeightDiff = Math.round(Math.abs(weightDiff) * 10.0) / 10.0;
                        stats.append("\nDesired Weight Change: ").append(roundedWeightDiff).append(" kg");
                    }
                }
            } else {
                stats.append("No initial stats found.");
            }

            if (timeMsg != null) stats.append("\n\n").append(timeMsg);
            tvInitial.setText(stats.toString());

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

    private void handleSubmit() {
        int selectedGoalId = goalRadioGroup.getCheckedRadioButtonId();
        int selectedGenderId = genderRadioGroup.getCheckedRadioButtonId();

        String goal = selectedGoalId == R.id.radioBulk ? "Bulking" :
                selectedGoalId == R.id.radioCut ? "Cutting" : "";

        String gender = selectedGenderId == R.id.radioMale ? "Male" :
                selectedGenderId == R.id.radioFemale ? "Female" : "";

        String ageStr = etAge.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String activityLevel = spinnerActivityLevel.getSelectedItem().toString();
        String weightGoalDiffStr = etWeightGoalDiff.getText().toString().trim();

        if (goal.isEmpty() || gender.isEmpty() || ageStr.isEmpty() ||
                heightStr.isEmpty() || weightStr.isEmpty() || weightGoalDiffStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int age;
        float height, weight, weightGoalDiff;
        try {
            age = Integer.parseInt(ageStr);
            height = Float.parseFloat(heightStr);
            weight = Float.parseFloat(weightStr);
            weightGoalDiff = Float.parseFloat(weightGoalDiffStr);

            if (goal.equals("Cutting")) {
                weightGoalDiff = -Math.abs(weightGoalDiff);
            } else if (goal.equals("Bulking")) {
                weightGoalDiff = Math.abs(weightGoalDiff);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number input", Toast.LENGTH_SHORT).show();
            return;
        }

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
        int recommendedCalories = goal.equals("Bulking") ? (int) (tdee + 500) : (int) (tdee - 500);

        double daysToReachGoal = (Math.abs(weightGoalDiff) * 7700) / 500;
        String timeToReachGoal = "It should take approximately " + Math.round(daysToReachGoal) + " days to reach your weight goal in a healthy manner.";

        String message = "Your estimated Total Daily Energy Expenditure: " +
                (int) tdee + " kcal/day\n" +
                "Recommended Intake for " + goal + ": " + recommendedCalories + " kcal/day";

        tvRecommendation.setText(message);

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
        userData.put("originalWeight", weight); // initial weight
        userData.put("activityLevel", activityLevel);
        userData.put("tdee", (int) tdee);
        userData.put("recommendedCalories", recommendedCalories);
        userData.put("message", message);
        userData.put("timeToReachGoal", timeToReachGoal);
        userData.put("weightGoalDiff", (double) weightGoalDiff);
        userData.put("originalWeightGoalDiff", weightGoalDiff);
        userData.put("goalProgressPercent", 0.0);
        userData.put("goalCompleted", false);

        db.collection("users").document(uid)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Data saved successfully!", Toast.LENGTH_SHORT).show();
                    loadExistingData();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

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
        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Double currentWeightObj = documentSnapshot.getDouble("weight");
            Double currentGoalDiffObj = documentSnapshot.getDouble("weightGoalDiff");
            Double originalGoalObj = documentSnapshot.getDouble("originalWeightGoalDiff");
            String goal = documentSnapshot.getString("goal");
            Boolean goalCompleted = documentSnapshot.getBoolean("goalCompleted");

            if (currentWeightObj == null || currentGoalDiffObj == null || originalGoalObj == null || goal == null || goalCompleted == null) {
                Toast.makeText(getContext(), "Incomplete user data", Toast.LENGTH_SHORT).show();
                return;
            }

            if (goalCompleted) {
                Toast.makeText(getContext(), "Goal already completed! Please make a new goal!", Toast.LENGTH_SHORT).show();
                return;
            }

            double currentWeight = currentWeightObj;
            double currentGoalDiff = currentGoalDiffObj;
            double originalGoal = originalGoalObj;

            double newWeightDiff;
            double newWeight;

            // Correct the weight difference and weight update based on goal type
            if ("Cutting".equals(goal)) {
                // For cutting, weightGoalDiff is negative and moves toward zero by adding positive weight lost
                newWeightDiff = currentGoalDiff + weightUpdate;
                newWeight = currentWeight - weightUpdate;
            } else if ("Bulking".equals(goal)) {
                // For bulking, weightGoalDiff is positive and moves toward zero by subtracting positive weight gained
                newWeightDiff = currentGoalDiff - weightUpdate;
                newWeight = currentWeight + weightUpdate;
            } else {
                // Default fallback, treat as bulking style
                newWeightDiff = currentGoalDiff - weightUpdate;
                newWeight = currentWeight + weightUpdate;
            }

            // Clamp newWeightDiff to zero if overshoot occurs
            if ((originalGoal < 0 && newWeightDiff > 0) || (originalGoal > 0 && newWeightDiff < 0)) {
                newWeightDiff = 0;
            }

            double progressPercent = 0;
            if (originalGoal != 0) {
                progressPercent = 100.0 * (originalGoal - newWeightDiff) / originalGoal;
                // Clamp to [0, 100]
                progressPercent = Math.max(0, Math.min(100, progressPercent));
            }

            boolean completed = Math.abs(newWeightDiff) < 0.5;  // Consider goal completed if within 0.5 kg

            // Points awarding logic:
            // Award 100 points once when goal is completed

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("weightGoalDiff", newWeightDiff);
            updateData.put("weight", newWeight);
            updateData.put("goalProgressPercent", progressPercent);

            if (completed && !goalCompleted) {
                updateData.put("goalCompleted", true);
                updateData.put("points", FieldValue.increment(100)); // add 100 points on completion
                Toast.makeText(getContext(), "Congratulations! Goal completed! 100 points awarded.", Toast.LENGTH_LONG).show();
            }

            userRef.set(updateData, SetOptions.merge()).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Progress updated successfully", Toast.LENGTH_SHORT).show();
                loadExistingData();  // Refresh UI
                etWeightUpdate.setText("");
            }).addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Failed to update progress: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
