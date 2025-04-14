package com.example.myapplication;

import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions; // <-- Added import here

import java.util.HashMap;
import java.util.Map;

public class FitnessGoalFragment extends Fragment {

    private RadioGroup goalRadioGroup, genderRadioGroup;
    private EditText etAge, etHeight, etWeight, etCaloricIntake;
    private Spinner spinnerActivityLevel;
    private Button btnSubmit;
    private TextView tvRecommendation;
    private FirebaseFirestore db;

    public FitnessGoalFragment() {
        super(R.layout.fragment_fitness_goal);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        goalRadioGroup = view.findViewById(R.id.goalRadioGroup);
        tvRecommendation = view.findViewById(R.id.tvRecommendation);
        genderRadioGroup = view.findViewById(R.id.genderRadioGroup);
        etAge = view.findViewById(R.id.etAge);
        etHeight = view.findViewById(R.id.etHeight);
        etWeight = view.findViewById(R.id.etWeight);
        etCaloricIntake = view.findViewById(R.id.etCaloricIntake);
        spinnerActivityLevel = view.findViewById(R.id.spinnerActivityLevel);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        db = FirebaseFirestore.getInstance();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            String uid = user.getUid();

            db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Long tdeeValue = documentSnapshot.getLong("tdee");
                            Long recommendedCaloriesValue = documentSnapshot.getLong("recommendedCalories");

                            if (tdeeValue != null && recommendedCaloriesValue != null) {
                                int tdee = tdeeValue.intValue();
                                int recommendedCalories = recommendedCaloriesValue.intValue();

                                String goal = documentSnapshot.getString("goal");
                                String gender = documentSnapshot.getString("gender");

                                String message = "Your estimated Total Daily Energy Expenditure: " + tdee + " kcal/day\n" +
                                        "Recommended Intake for " + goal + ": " + recommendedCalories + " kcal/day";
                                tvRecommendation.setText(message);
                            } else {
                                tvRecommendation.setText("No recommendation data found. Please submit your goal.");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }

        btnSubmit.setOnClickListener(v -> handleSubmit());
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

        if (goal.isEmpty() || gender.isEmpty() || ageStr.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int age;
        float height, weight;
        try {
            age = Integer.parseInt(ageStr);
            height = Float.parseFloat(heightStr);
            weight = Float.parseFloat(weightStr);
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
                (int) (tdee + 400) :
                (int) (tdee - 400);

        String message = "Your estimated Total Daily Energy Expenditure: " + (int) tdee + " kcal/day\n" +
                "Recommended Intake for " + goal + ": " + recommendedCalories + " kcal/day";

        tvRecommendation.setText(message);

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

            db.collection("users")
                    .document(uid)
                    .set(userData, SetOptions.merge()) // <-- Merge instead of overwrite
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
}
