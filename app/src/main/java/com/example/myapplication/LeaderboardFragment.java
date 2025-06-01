package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Fragment displaying the leaderboard. Handles leaderboard display, reset logic,
 * and countdown to the next reset.
 */
public class LeaderboardFragment extends Fragment {

    // RecyclerView to display the leaderboard
    private RecyclerView recyclerView;
    // Adapter for the leaderboard RecyclerView
    private LeaderboardAdapter adapter;
    // List to hold leaderboard entries
    private List<LeaderboardEntry> leaderboardList = new ArrayList<>();
    // Firebase Firestore instance
    private FirebaseFirestore db;
    // TextView to show countdown to next leaderboard reset
    private TextView countdownTextView;
    // Tag for logging
    private static final String TAG = "LeaderboardFragment";

    /**
     * Inflates the fragment layout and sets up the leaderboard view and Firestore instance.
     *
     * @param inflater           LayoutInflater to inflate the layout
     * @param container          ViewGroup container
     * @param savedInstanceState Bundle containing previous state
     * @return View object for the fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        // Initialize the RecyclerView from layout
        recyclerView = view.findViewById(R.id.leaderboardRecyclerView);
        // Initialize the countdown TextView from layout
        countdownTextView = view.findViewById(R.id.countdownTextView);
        // Set the layout manager for the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Initialize the adapter with the leaderboard list
        adapter = new LeaderboardAdapter(leaderboardList);
        // Set the adapter to the RecyclerView
        recyclerView.setAdapter(adapter);
        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance();
        // Check if the leaderboard needs to be reset
        checkLeaderboardReset();

        return view;
    }

    /**
     * Fetches leaderboard data from Firestore, sorts it by points descending,
     * and updates the RecyclerView adapter.
     */
    private void fetchLeaderboard() {
        db.collection("users")
                .orderBy("points", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Clear the current list
                    leaderboardList.clear();

                    // Add each document's data to the leaderboard list
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        Long points = doc.getLong("points");
                        if (name != null && points != null) {
                            leaderboardList.add(new LeaderboardEntry(name, points));
                        }
                    }

                    // Notify the adapter that the data has changed
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    // Log and show error if leaderboard fetch fails
                    Log.e(TAG, "Failed to load leaderboard", e);
                    Toast.makeText(getContext(), "Failed to load leaderboard", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Checks the last leaderboard reset date stored in Firestore and either
     * displays a countdown, freezes the leaderboard, or resets it.
     */
    private void checkLeaderboardReset() {
        DocumentReference configRef = db.collection("config").document("leaderboard");

        configRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                // Get the last reset timestamp
                Timestamp lastReset = document.getTimestamp("lastResetDate");

                if (lastReset != null) {
                    // Calculate how many days have passed since the last reset
                    long daysPassed = TimeUnit.MILLISECONDS.toDays(
                            new Date().getTime() - lastReset.toDate().getTime()
                    );

                    // Calculate how many days are left until the next reset
                    long daysUntilReset = 30 - daysPassed;

                    // Show countdown or reset message based on time passed
                    if (daysUntilReset > 0) {
                        countdownTextView.setText("Leaderboard Reset: " + daysUntilReset + " Days");
                        fetchLeaderboard();
                    } else if (daysPassed == 30) {
                        countdownTextView.setText("Leaderboard Frozen! Season Over!");
                    } else if (daysPassed > 30) {
                        countdownTextView.setText("Leaderboard reset! Next reset in 30 days.");
                        resetAllUserData(configRef);
                    }
                }
            }
        }).addOnFailureListener(e -> {
            // Log if fetching leaderboard config fails
            Log.e(TAG, "Failed to fetch leaderboard config", e);
        });
    }

    /**
     * Resets all users' data including points, meals, and workouts.
     * Updates the reset timestamp in Firestore and refreshes the leaderboard.
     *
     * @param configRef Reference to the leaderboard config document
     */
    private void resetAllUserData(DocumentReference configRef) {
        db.collection("users").get().addOnSuccessListener(querySnapshot -> {
            for (DocumentSnapshot userDoc : querySnapshot.getDocuments()) {
                DocumentReference userRef = userDoc.getReference();

                // Reset user points to 0
                userRef.update("points", 0)
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to reset points for user " + userRef.getId(), e));

                // Delete all meals for the user
                userRef.collection("meals").get()
                        .addOnSuccessListener(mealSnapshot -> {
                            for (DocumentSnapshot doc : mealSnapshot.getDocuments()) {
                                doc.getReference().delete()
                                        .addOnFailureListener(e -> Log.e(TAG, "Failed to delete meal: " + doc.getId(), e));
                            }
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch meals for user " + userRef.getId(), e));

                // Delete all workouts for the user
                userRef.collection("workouts").get()
                        .addOnSuccessListener(workoutSnapshot -> {
                            for (DocumentSnapshot doc : workoutSnapshot.getDocuments()) {
                                doc.getReference().delete()
                                        .addOnFailureListener(e -> Log.e(TAG, "Failed to delete workout: " + doc.getId(), e));
                            }
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch workouts for user " + userRef.getId(), e));
            }

            // Update the leaderboard reset timestamp
            configRef.update("lastResetDate", new Timestamp(new Date()))
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Leaderboard reset timestamp updated.");
                        fetchLeaderboard();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update leaderboard reset timestamp", e));

        }).addOnFailureListener(e -> {
            // Log and show error if user data couldn't be fetched for reset
            Log.e(TAG, "Failed to fetch users for reset", e);
            Toast.makeText(getContext(), "Failed to reset leaderboard", Toast.LENGTH_SHORT).show();
        });
    }
}
