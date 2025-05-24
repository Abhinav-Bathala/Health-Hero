package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;

public class ProfileFragment extends Fragment {

    // Declare UI elements and Firebase instances
    private TextView emailTextView, streakText, weightText, calorieText, rankingText, welcomeText, quoteText, pointsText;
    private Button logoutButton;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    @Nullable
    @Override
    // Inflate the fragment layout and initialize Firebase and UI components
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase Authentication instance
        auth = FirebaseAuth.getInstance();
        // Get current logged-in user
        user = auth.getCurrentUser();
        // Initialize Firestore database instance
        db = FirebaseFirestore.getInstance();

        // Bind UI elements to their respective views
        emailTextView = view.findViewById(R.id.userEmail);
        logoutButton = view.findViewById(R.id.logout);
        streakText = view.findViewById(R.id.streakText);
        weightText = view.findViewById(R.id.weightText);
        calorieText = view.findViewById(R.id.calorieText);
        rankingText = view.findViewById(R.id.rankingText);
        welcomeText = view.findViewById(R.id.welcometext);
        quoteText = view.findViewById(R.id.quotetext);
        pointsText = view.findViewById(R.id.pointsText);

        // If user is logged in, load profile data
        if (user != null) {
            // Display user email
            emailTextView.setText(user.getEmail());
            // Load welcome message based on user name
            loadWelcomeMessage(user.getUid());
            // Check and update user's streak count
            checkAndUpdateStreak(user.getUid());
            // Load fitness-related data
            loadFitnessData(user.getUid());
            // Load user's leaderboard ranking
            loadUserRanking(user.getEmail());
        } else {
            // If no user logged in, redirect to login activity and finish this one
            startActivity(new Intent(getActivity(), Login.class));
            getActivity().finish();
        }

        // Set logout button click listener
        logoutButton.setOnClickListener(v -> {
            // Sign out user from Firebase
            FirebaseAuth.getInstance().signOut();
            // Redirect to login activity and finish current one
            startActivity(new Intent(getActivity(), Login.class));
            getActivity().finish();
        });

        // Return the inflated view
        return view;
    }

    // Load and display personalized welcome message using Firestore user document
    private void loadWelcomeMessage(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.get().addOnSuccessListener(document -> {
            // Get user name from Firestore document
            String name = document.getString("name");
            // Set welcome text with user name if available
            welcomeText.setText(name != null && !name.isEmpty() ? "Welcome, " + name + "!" : "Welcome!");
        }).addOnFailureListener(e -> {
            // On failure, show generic welcome message and log error
            welcomeText.setText("Welcome!");
            Log.e("ProfileFragment", "Failed to load user name", e);
        });
    }

    // Check the current streak and update it if needed
    private void checkAndUpdateStreak(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.get().addOnSuccessListener(document -> {
            // Get current streak count or default to 0
            int currentStreak = document.contains("streakCount") ? document.getLong("streakCount").intValue() : 0;
            // Get last check-in date string
            String lastCheckIn = document.getString("lastCheckInDate");
            // Get saved quote associated with streak
            String savedQuote = document.getString("streakQuote");

            // Formatter for date strings (year-month-day)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            // Today's date formatted
            String todayStr = sdf.format(new Date());

            // Calculate yesterday's date string
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            String yesterdayStr = sdf.format(cal.getTime());

            // Flag to track if streak was updated
            boolean streakIncreased = false;

            // Reset streak if last check-in is older than yesterday
            if (lastCheckIn == null || lastCheckIn.compareTo(yesterdayStr) < 0){
                currentStreak = 1;
                streakIncreased = true;
                // Increment streak if last check-in was exactly yesterday
            } else if (lastCheckIn.equals(yesterdayStr)) {
                currentStreak += 1;
                streakIncreased = true;
            }

            // Prepare map of fields to update in Firestore
            Map<String, Object> updates = new HashMap<>();
            updates.put("streakCount", currentStreak);
            updates.put("lastCheckInDate", todayStr);
            // Update streak info in Firestore
            userRef.update(updates).addOnFailureListener(e -> Log.e("Streak", "Failed to update: " + e.getMessage()));

            // Update streak display text
            streakText.setText("Streak:\n" + currentStreak + " 🔥");

            // If streak changed, display and save a new quote
            if (streakIncreased) {
                displayAndSaveQuote(userRef);
                // Else if saved quote exists, display it
            } else if (savedQuote != null && !savedQuote.isEmpty()) {
                quoteText.setText(savedQuote);
                // Fallback quote if none saved
            } else {
                quoteText.setText("- Keep pushing forward!");
            }

        }).addOnFailureListener(e -> Log.e("Streak", "Failed to load streak data", e));
    }

    // Display a random motivational quote from raw resource file
    private void displayRandomQuote() {
        try {
            // Open raw resource file containing motivational quotes
            InputStream inputStream = getResources().openRawResource(R.raw.motivationalquotes);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            List<String> quotes = new ArrayList<>();
            String line;
            // Read each line, trim, and add non-empty lines to list
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    quotes.add(line.trim());
                }
            }
            reader.close();

            // If quotes list is not empty, pick one at random and display
            if (!quotes.isEmpty()) {
                String selectedQuote = quotes.get(new Random().nextInt(quotes.size()));
                quoteText.setText(selectedQuote);
            }
        } catch (Exception e) {
            // On error, log and display fallback motivational message
            Log.e("QuoteError", "Could not load quote", e);
            quoteText.setText("- Stay strong and keep going!");
        }
    }

    // Display a random quote and save it to Firestore for persistence
    private void displayAndSaveQuote(DocumentReference userRef) {
        try {
            // Open raw resource file containing motivational quotes
            InputStream inputStream = getResources().openRawResource(R.raw.motivationalquotes);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            List<String> quotes = new ArrayList<>();
            String line;
            // Read and collect non-empty lines as quotes
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    quotes.add(line.trim());
                }
            }
            reader.close();

            // If quotes list is not empty, pick one randomly
            if (!quotes.isEmpty()) {
                String selectedQuote = quotes.get(new Random().nextInt(quotes.size()));
                // Display selected quote on UI
                quoteText.setText(selectedQuote);

                // Prepare map to save quote in Firestore under streakQuote field
                Map<String, Object> quoteUpdate = new HashMap<>();
                quoteUpdate.put("streakQuote", selectedQuote);
                // Update Firestore document with new quote, log failure if occurs
                userRef.update(quoteUpdate).addOnFailureListener(e -> Log.e("QuoteSave", "Failed to save quote", e));
            }
        } catch (Exception e) {
            // On error, log it and set fallback quote text
            Log.e("QuoteError", "Could not load quote", e);
            quoteText.setText("- Stay strong and keep going!");
        }
    }

    // Load and display user's fitness data such as weight, calories, and points
    private void loadFitnessData(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.get().addOnSuccessListener(document -> {
            // Display current weight if available
            if (document.contains("weight")) {
                weightText.setText("Current Weight:\n" + document.getDouble("weight") + " kg");
            } else {
                weightText.setText("Current Weight:\nN/A");
            }

            // Display recommended calorie intake if available
            if (document.contains("recommendedCalories")) {
                calorieText.setText("Calorie Intake:\n" + document.getLong("recommendedCalories") + " kcal");
            } else {
                calorieText.setText("Calorie Intake:\nN/A");
            }

            // Display user's points if available
            if(document.contains("points")) {
                pointsText.setText("Points:\n" + document.getLong("points"));
            }
        }).addOnFailureListener(e -> Log.e("FitnessData", "Failed to load fitness data", e));
    }

    // Load the user's leaderboard rank based on points in descending order
    private void loadUserRanking(String userEmail) {
        db.collection("users")
                // Order users by points descending to get ranking
                .orderBy("points", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int rank = 1;
                    // Iterate over all user documents to find the current user's rank
                    for (DocumentSnapshot doc : querySnapshot) {
                        if (userEmail.equals(doc.getString("email"))) {
                            // Display the rank once user is found
                            rankingText.setText("Leaderboard Rank:\n#" + rank);
                            return;
                        }
                        rank++;
                    }
                    // If user not found, show N/A
                    rankingText.setText("Ranking:\nN/A");
                })
                .addOnFailureListener(e -> {
                    // On failure to get ranking, show N/A and log error
                    rankingText.setText("Ranking:\nN/A");
                    Log.e("Ranking", "Failed to load ranking", e);
                });
    }
}
