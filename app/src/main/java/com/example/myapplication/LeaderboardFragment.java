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

public class LeaderboardFragment extends Fragment {

    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private List<LeaderboardEntry> leaderboardList = new ArrayList<>();
    private FirebaseFirestore db;
    private TextView countdownTextView;
    private static final String TAG = "LeaderboardFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        recyclerView = view.findViewById(R.id.leaderboardRecyclerView);
        countdownTextView = view.findViewById(R.id.countdownTextView);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LeaderboardAdapter(leaderboardList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        checkLeaderboardReset();

        return view;
    }

    private void fetchLeaderboard() {
        db.collection("users")
                .orderBy("points", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    leaderboardList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        Long points = doc.getLong("points");
                        if (name != null && points != null) {
                            leaderboardList.add(new LeaderboardEntry(name, points));
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load leaderboard", e);
                    Toast.makeText(getContext(), "Failed to load leaderboard", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkLeaderboardReset() {
        DocumentReference configRef = db.collection("config").document("leaderboard");

        configRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                Timestamp lastReset = document.getTimestamp("lastResetDate");
                if (lastReset != null) {
                    long daysPassed = TimeUnit.MILLISECONDS.toDays(
                            new Date().getTime() - lastReset.toDate().getTime()
                    );

                    long daysUntilReset = 30 - daysPassed;

                    if (daysUntilReset > 0) {
                        countdownTextView.setText("Leaderboard Reset: " + daysUntilReset + " Days");
                        fetchLeaderboard();
                    }
                    else if (daysPassed == 30){
                        countdownTextView.setText("Leaderboard Frozen! Season Over!");
                    }
                    else if (daysPassed > 30) {
                        countdownTextView.setText("Leaderboard reset! Next reset in 30 days.");
                        resetAllUserData(configRef);
                    }
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to fetch leaderboard config", e);
        });
    }

    private void resetAllUserData(DocumentReference configRef) {
        db.collection("users").get().addOnSuccessListener(querySnapshot -> {
            for (DocumentSnapshot userDoc : querySnapshot.getDocuments()) {
                DocumentReference userRef = userDoc.getReference();

                // Reset points
                userRef.update("points", 0)
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to reset points for user " + userRef.getId(), e));

                // Delete meals
                userRef.collection("meals").get()
                        .addOnSuccessListener(mealSnapshot -> {
                            for (DocumentSnapshot doc : mealSnapshot.getDocuments()) {
                                doc.getReference().delete()
                                        .addOnFailureListener(e -> Log.e(TAG, "Failed to delete meal: " + doc.getId(), e));
                            }
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch meals for user " + userRef.getId(), e));

                // Delete workouts
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
            Log.e(TAG, "Failed to fetch users for reset", e);
            Toast.makeText(getContext(), "Failed to reset leaderboard", Toast.LENGTH_SHORT).show();
        });
    }
}
