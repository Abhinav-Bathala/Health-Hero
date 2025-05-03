package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
    private EditText etAge, etHeight, etWeight, etCaloricIntake, etWeightGoalDiff, etWeightUpdate;
    private Spinner spinnerActivityLevel;
    private Button btnSubmit, btnUpdateProgress;
    private TextView tvRecommendation, tvInitial, tvProgressPercent;
    private FirebaseFirestore db;

    public FitnessGoalFragment() {
        super(R.layout.fragment_fitness_goal);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        goalRadioGroup = view.findViewById(R.id.goalRadioGroup);
        genderRadioGroup = view.findViewById(R.id.genderRadioGroup);
        etAge = view.findViewById(R.id.etAge);
        etHeight = view.findViewById(R.id.etHeight);
        etWeight = view.findViewById(R.id.etWeight);
        etWeightGoalDiff = view.findViewById(R.id.etWeightGoalDiff);
        etCaloricIntake = view.findViewById(R.id.etCaloricIntake);
        etWeightUpdate = view.findViewById(R.id.etWeightUpdate);
        spinnerActivityLevel = view.findViewById(R.id.spinnerActivityLevel);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        tvRecommendation = view.findViewById(R.id.tvRecommendation);
        tvInitial = view.findViewById(R.id.tvInitial);
        etWeightUpdate = view.findViewById(R.id.etWeightUpdate);
        btnUpdateProgress = view.findViewById(R.id.btnUpdateProgress);
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent);



        db = FirebaseFirestore.getInstance();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            String uid = user.getUid();

            DocumentReference userRef = db.collection("users").document(uid);
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Long tdeeValue = documentSnapshot.getLong("tdee");
                    Long recommendedCaloriesValue = documentSnapshot.getLong("recommendedCalories");

                    if (tdeeValue != null && recommendedCaloriesValue != null) {
                        int tdee = tdeeValue.intValue();
                        int recommendedCalories = recommendedCaloriesValue.intValue();
                        String goal = documentSnapshot.getString("goal");

                        String message = "Your estimated Total Daily Energy Expenditure: " + tdee + " kcal/day\n" +
                                "Recommended Intake for " + goal + ": " + recommendedCalories + " kcal/day";
                        tvRecommendation.setText(message);
                    } else {
                        tvRecommendation.setText("No recommendation data found. Please submit your goal.");
                    }

                    String initialStats = "";

                    Long ageValue = documentSnapshot.getLong("age");
                    Double heightValue = documentSnapshot.getDouble("height");
                    Double weightValue = documentSnapshot.getDouble("weight");
                    String activityLevel = documentSnapshot.getString("activityLevel");
                    String timeMessage = documentSnapshot.getString("timeToReachGoal");
                    String goal = documentSnapshot.getString("goal");
                    Double weightGoalDiff = documentSnapshot.getDouble("weightGoalDiff");

                    if (ageValue != null && heightValue != null && weightValue != null && activityLevel != null) {
                        initialStats = "Initial Stats:\n" +
                                "Age: " + ageValue + " yrs\n" +
                                "Height: " + heightValue + " cm\n" +
                                "Weight: " + weightValue + " kg\n" +
                                "Activity Level: " + activityLevel;

                        if (weightGoalDiff != null && goal != null) {
                            String sign = goal.equals("Bulking") ? "+" : goal.equals("Cutting") ? "-" : "";
                            initialStats += "\nDesired Weight Change: " + sign + weightGoalDiff + " kg";
                        }
                    } else {
                        initialStats = "No initial stats found.";
                    }

                    if (timeMessage != null) {
                        initialStats += "\n\n" + timeMessage;
                    }

                    tvInitial.setText(initialStats);
                    Double originalGoal = documentSnapshot.getDouble("originalWeightGoalDiff");
                    Double currentGoalDiff = documentSnapshot.getDouble("weightGoalDiff");

                    if (originalGoal != null && currentGoalDiff != null) {
                        double percentComplete = 100 * (originalGoal - currentGoalDiff) / originalGoal;
                        if (percentComplete > 100) percentComplete = 100;
                        if (percentComplete < 0) percentComplete = 0;
                        tvProgressPercent.setText(String.format("Goal Progress: %.1f%%", percentComplete));
                    } else {
                        tvProgressPercent.setText("Goal Progress: 0.0%");
                    }




                }
            }).addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
            );
        } else {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }

        btnSubmit.setOnClickListener(v -> handleSubmit());
        btnUpdateProgress.setOnClickListener(v -> handleProgressUpdate());

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

        if (goal.isEmpty() || gender.isEmpty() || ageStr.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty() || weightGoalDiffStr.isEmpty()) {
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
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number input", Toast.LENGTH_SHORT).show();
            return;
        }

        double bmr = gender.equals("Male") ?
                10 * weight + 6.25 * height - 5 * age + 5 :
                10 * weight + 6.25 * height - 5 * age - 161;

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

        int recommendedCalories = goal.equals("Bulking") ?
                (int) (tdee + 500) :
                (int) (tdee - 500);

        double daysToReachGoal = (weightGoalDiff * 7700) / 500;
        String timeToReachGoal = "It should take approximately " + Math.round(daysToReachGoal) + " days to reach your weight goal in a healthy manner.";

        String message = "Your estimated Total Daily Energy Expenditure: " + (int) tdee + " kcal/day\n" +
                "Recommended Intake for " + goal + ": " + recommendedCalories + " kcal/day\n" +
                timeToReachGoal;

        tvRecommendation.setText(message);
        tvInitial.setText(timeToReachGoal);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
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
            userData.put("weightGoalDiff", weightGoalDiff);

            db.collection("users")
                    .document(uid)
                    .set(userData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Data saved successfully!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
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
        if (user != null) {
            String uid = user.getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Double currentWeightDiff = documentSnapshot.getDouble("weightGoalDiff");

                            if (currentWeightDiff != null) {
                                double newWeightDiff = currentWeightDiff - weightUpdate;

                                // Calculate total goal and completed amount
                                Double originalGoal = documentSnapshot.getDouble("originalWeightGoalDiff");
                                if (originalGoal == null) {
                                    originalGoal = currentWeightDiff; // Store original goal if not set yet
                                    Map<String, Object> originSet = new HashMap<>();
                                    originSet.put("originalWeightGoalDiff", originalGoal);
                                    db.collection("users").document(uid).set(originSet, SetOptions.merge());
                                }

                                double percentComplete = 100 * (originalGoal - newWeightDiff) / originalGoal;
                                if (percentComplete > 100) percentComplete = 100;
                                if (percentComplete < 0) percentComplete = 0;

                                tvProgressPercent.setText(String.format("Goal Progress: %.1f%%", percentComplete));


                                Map<String, Object> updateMap = new HashMap<>();
                                updateMap.put("weightGoalDiff", newWeightDiff);
                                double percentComplete2 = (weightUpdate/originalGoal)*100;

                                int pointsEarned = Math.round((float) percentComplete2); // Ensure correct type

                                db.collection("users").document(uid)
                                        .set(updateMap, SetOptions.merge())
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(getContext(), "Progress updated! Remaining: " + newWeightDiff + " kg", Toast.LENGTH_SHORT).show();
                                            awardPoints(uid, pointsEarned);
                                        })

                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }

                        } else {
                            Toast.makeText(getContext(), "No goal data found to update", Toast.LENGTH_SHORT).show();
                        }

                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error accessing Firestore", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }


    private void awardPoints(String uid, int pointsToAdd) {
        DocumentReference userRef = db.collection("users").document(uid);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);
            long currentPoints = 0;
            if (snapshot.exists() && snapshot.contains("points")) {
                Long val = snapshot.getLong("points");
                if (val != null) {
                    currentPoints = val;
                }
            }
            long updatedPoints = currentPoints + pointsToAdd;
            transaction.update(userRef, "points", updatedPoints);
            return null;
        }).addOnSuccessListener(unused -> {
            Log.d("FitnessGoalFragment", pointsToAdd + " points awarded.");
        }).addOnFailureListener(e -> {
            Log.e("FitnessGoalFragment", "Failed to award points", e);
        });
    }


}