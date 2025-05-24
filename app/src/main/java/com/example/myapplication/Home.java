package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Home extends AppCompatActivity {

    // Bottom navigation view used to switch between fragments
    BottomNavigationView bottomNavigationView;

    // Instances of fragments used in this activity
    FitnessGoalFragment fitnessGoalFragment = new FitnessGoalFragment();
    ProfileFragment profileFragment = new ProfileFragment();
    NutritionFragment nutritionFragment = new NutritionFragment();
    LeaderboardFragment leaderboardFragment = new LeaderboardFragment();
    WorkoutFragment workoutFragment = new WorkoutFragment();

    // Firebase Authentication variables (not used here but might be for future features)
    FirebaseAuth auth;
    Button button;
    TextView textView;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout for this activity
        setContentView(R.layout.activity_home);

        // Bind the BottomNavigationView from the layout
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Set the default fragment when the activity starts (Profile tab)
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.flFragment, profileFragment)
                .commit();

        // Set the selected item to Profile in the bottom nav bar
        bottomNavigationView.setSelectedItemId(R.id.profile);

        // Set listener for navigation item selection
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Switch fragments depending on which item is selected
            if (item.getItemId() == R.id.profile) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, profileFragment)
                        .commit();
                return true;
            } else if (item.getItemId() == R.id.leaderboard) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, leaderboardFragment)
                        .commit();
                return true;
            } else if (item.getItemId() == R.id.workouts) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, workoutFragment)
                        .commit();
                return true;
            } else if (item.getItemId() == R.id.nutrition) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, nutritionFragment)
                        .commit();
                return true;
            } else if (item.getItemId() == R.id.fitness) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flFragment, fitnessGoalFragment)
                        .commit();
                return true;
            } else {
                // Return false if no known item is selected
                return false;
            }
        });
    }
}
