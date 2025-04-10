package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;

public class FitnessGoalFragment extends Fragment {

    private RadioGroup goalRadioGroup;
    private EditText etAge, etHeight, etWeight, etCaloricIntake;
    private Spinner spinnerActivityLevel;
    private Button btnSubmit;

    public FitnessGoalFragment() {
        super(R.layout.fragment_fitness_goal);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        goalRadioGroup = view.findViewById(R.id.goalRadioGroup);
        etAge = view.findViewById(R.id.etAge);
        etHeight = view.findViewById(R.id.etHeight);
        etWeight = view.findViewById(R.id.etWeight);
        etCaloricIntake = view.findViewById(R.id.etCaloricIntake);
        spinnerActivityLevel = view.findViewById(R.id.spinnerActivityLevel);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        // Get user input
        int selectedGoalId = goalRadioGroup.getCheckedRadioButtonId();
        String goal = selectedGoalId == R.id.radioBulk ? "Bulking" :
                selectedGoalId == R.id.radioCut ? "Cutting" : "";

        String age = etAge.getText().toString().trim();
        String height = etHeight.getText().toString().trim();
        String weight = etWeight.getText().toString().trim();
        String caloricIntake = etCaloricIntake.getText().toString().trim();
        String activityLevel = spinnerActivityLevel.getSelectedItem().toString();

        // Input validation
        if (goal.isEmpty() || age.isEmpty() || height.isEmpty() || weight.isEmpty() || caloricIntake.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert values to appropriate types
        int userAge = Integer.parseInt(age);
        float userHeight = Float.parseFloat(height);
        float userWeight = Float.parseFloat(weight);
        int userCalories = Integer.parseInt(caloricIntake);

        // Display a confirmation message
        String message = "Goal: " + goal + "\nAge: " + userAge + "\nHeight: " + userHeight +
                " cm\nWeight: " + userWeight + " kg\nActivity: " + activityLevel +
                "\nCalories: " + userCalories;
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }
}
