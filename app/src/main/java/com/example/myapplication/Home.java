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

    BottomNavigationView bottomNavigationView;
    FitnessGoalFragment fitnessGoalFragment = new FitnessGoalFragment();
    ProfileFragment profileFragment = new ProfileFragment();
    NutritionFragment nutritionFragment = new NutritionFragment();
    LeaderboardFragment leaderboardFragment = new LeaderboardFragment();
    WorkoutFragment workoutFragment = new WorkoutFragment();


    FirebaseAuth auth;
    Button button;
    TextView textView;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Initialize fragments
        ProfileFragment profileFragment = new ProfileFragment();
        LeaderboardFragment leaderboardFragment = new LeaderboardFragment();
        WorkoutFragment workoutFragment = new WorkoutFragment();
        NutritionFragment nutritionFragment = new NutritionFragment();
        FitnessGoalFragment fitnessGoalFragment = new FitnessGoalFragment();

        // Set the initial fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, profileFragment).commit();
        bottomNavigationView.setSelectedItemId(R.id.profile);  // Replace with your actual menu item ID

        // Handle BottomNavigationView item selection
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.profile) {
                getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, profileFragment).commit();
                return true;
            } else if (item.getItemId() == R.id.leaderboard) {
                getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, leaderboardFragment).commit();
                return true;
            } else if (item.getItemId() == R.id.workouts) {
                getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, workoutFragment).commit();
                return true;
            } else if (item.getItemId() == R.id.nutrition) {
                getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, nutritionFragment).commit();
                return true;
            } else if (item.getItemId() == R.id.fitness) {
                getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, fitnessGoalFragment).commit();
                return true;
            } else {
                return false;
            }
        });
    }

}

