package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Fragment displaying user profile information including email, streak, weight,
 * calorie target, ranking, welcome message, motivational quote, and points.
 * Handles logout and real-time updates to fitness data.
 */
public class ProfileFragment extends Fragment {

    // UI elements
    private TextView emailTextView, streakText, weightText, calorieText, rankingText, welcomeText, quoteText, pointsText;
    private Button logoutButton;

    // Firebase authentication and database
    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    // Listener for real-time updates to user document
    private ListenerRegistration userDocListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Bind UI elements to views
        emailTextView = view.findViewById(R.id.userEmail);
        logoutButton = view.findViewById(R.id.logout);
        streakText = view.findViewById(R.id.streakText);
        weightText = view.findViewById(R.id.weightText);
        calorieText = view.findViewById(R.id.calorieText);
        rankingText = view.findViewById(R.id.rankingText);
        welcomeText = view.findViewById(R.id.welcometext);
        quoteText = view.findViewById(R.id.quotetext);
        pointsText = view.findViewById(R.id.pointsText);

        if (user != null) {
            // Display user's email
            emailTextView.setText(user.getEmail());

            // Load additional user info and setup listeners
            loadWelcomeMessage(user.getUid());
            checkAndUpdateStreak(user.getUid());
            startListeningToFitnessData(user.getUid());
            loadUserRanking(user.getEmail());
        } else {
            // Redirect to login if user is not authenticated
            startActivity(new Intent(getActivity(), Login.class));
            getActivity().finish();
        }

        // Setup logout button to sign out and navigate to login screen
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), Login.class));
            getActivity().finish();
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove Firestore real-time listener to avoid memory leaks
        if (userDocListener != null) {
            userDocListener.remove();
        }
    }

    /**
     * Loads and sets a personalized welcome message using the user's name from Firestore.
     * @param uid The user's unique identifier.
     */
    private void loadWelcomeMessage(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.get().addOnSuccessListener(document -> {
            String name = document.getString("name");
            welcomeText.setText(name != null && !name.isEmpty() ? "Welcome, " + name + "!" : "Welcome!");
        }).addOnFailureListener(e -> {
            welcomeText.setText("Welcome!");
            Log.e("ProfileFragment", "Failed to load user name", e);
        });
    }

    /**
     * Checks the user's current streak, updates it if necessary,
     * displays a motivational quote, and saves the quote if the streak increases.
     * @param uid The user's unique identifier.
     */
    private void checkAndUpdateStreak(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.get().addOnSuccessListener(document -> {
            int currentStreak = document.contains("streakCount") ? document.getLong("streakCount").intValue() : 0;
            String lastCheckIn = document.getString("lastCheckInDate");
            String savedQuote = document.getString("streakQuote");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String todayStr = sdf.format(new Date());

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            String yesterdayStr = sdf.format(cal.getTime());

            boolean streakIncreased = false;

            // Reset or increment streak based on last check-in date
            if (lastCheckIn == null || lastCheckIn.compareTo(yesterdayStr) < 0){
                currentStreak = 1;
                streakIncreased = true;
            } else if (lastCheckIn.equals(yesterdayStr)) {
                currentStreak += 1;
                streakIncreased = true;
            }

            // Update Firestore with new streak info
            Map<String, Object> updates = new HashMap<>();
            updates.put("streakCount", currentStreak);
            updates.put("lastCheckInDate", todayStr);
            userRef.update(updates).addOnFailureListener(e -> Log.e("Streak", "Failed to update: " + e.getMessage()));

            // Update UI streak display
            streakText.setText("Streak:\n" + currentStreak + " 🔥");

            // Display a motivational quote
            if (streakIncreased) {
                displayAndSaveQuote(userRef);
            } else if (savedQuote != null && !savedQuote.isEmpty()) {
                quoteText.setText(savedQuote);
            } else {
                quoteText.setText("- Keep pushing forward!");
            }

        }).addOnFailureListener(e -> Log.e("Streak", "Failed to load streak data", e));
    }

    /**
     * Selects a random motivational quote from a raw resource file,
     * displays it in the UI, and saves it to Firestore for the current user.
     * @param userRef Reference to the current user's Firestore document.
     */
    private void displayAndSaveQuote(DocumentReference userRef) {
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.motivationalquotes);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            List<String> quotes = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    quotes.add(line.trim());
                }
            }
            reader.close();

            if (!quotes.isEmpty()) {
                String selectedQuote = quotes.get(new Random().nextInt(quotes.size()));
                quoteText.setText(selectedQuote);

                Map<String, Object> quoteUpdate = new HashMap<>();
                quoteUpdate.put("streakQuote", selectedQuote);
                userRef.update(quoteUpdate).addOnFailureListener(e -> Log.e("QuoteSave", "Failed to save quote", e));
            }
        } catch (Exception e) {
            Log.e("QuoteError", "Could not load quote", e);
            quoteText.setText("- Stay strong and keep going!");
        }
    }

    /**
     * Starts a real-time Firestore listener on the user's document
     * to update fitness-related UI elements when data changes.
     * @param uid The user's unique identifier.
     */
    private void startListeningToFitnessData(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);

        userDocListener = userRef.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) {
                Log.e("FitnessData", "Snapshot listener failed", error);
                return;
            }

            if (snapshot.contains("weight")) {
                weightText.setText("Current Weight:\n" + snapshot.getDouble("weight") + " kg");
            } else {
                weightText.setText("Current Weight:\nN/A");
            }

            if (snapshot.contains("recommendedCalories")) {
                calorieText.setText("Calorie Target:\n" + snapshot.getLong("recommendedCalories") + " kcal");
            } else {
                calorieText.setText("Calorie Target:\nN/A");
            }

            if (snapshot.contains("points")) {
                pointsText.setText("Points:\n" + snapshot.getLong("points"));
            }
        });
    }

    /**
     * Loads the user's leaderboard ranking based on points
     * and updates the UI with their rank or "N/A" if not found.
     * @param userEmail The user's email to identify them in the ranking list.
     */
    private void loadUserRanking(String userEmail) {
        db.collection("users")
                .orderBy("points", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int rank = 1;
                    for (DocumentSnapshot doc : querySnapshot) {
                        if (userEmail.equals(doc.getString("email"))) {
                            rankingText.setText("Leaderboard Rank:\n#" + rank);
                            return;
                        }
                        rank++;
                    }
                    rankingText.setText("Ranking:\nN/A");
                })
                .addOnFailureListener(e -> {
                    rankingText.setText("Ranking:\nN/A");
                    Log.e("Ranking", "Failed to load ranking", e);
                });
    }
}
