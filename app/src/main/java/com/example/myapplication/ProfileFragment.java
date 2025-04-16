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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class ProfileFragment extends Fragment {


    private TextView emailTextView, streakText, weightText, calorieText, rankingText, welcomeText;
    private Button logoutButton;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);


        // Firebase setup
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();


        // View binding
        emailTextView = view.findViewById(R.id.user_details);
        logoutButton = view.findViewById(R.id.logout);
        streakText = view.findViewById(R.id.streakText);
        weightText = view.findViewById(R.id.weightText);
        calorieText = view.findViewById(R.id.calorieText);
        rankingText = view.findViewById(R.id.rankingText);
        welcomeText= view.findViewById(R.id.welcometext);


        if (user != null) {
            emailTextView.setText(user.getEmail());
            loadWelcomeMessage(user.getUid());
            checkAndUpdateStreak(user.getUid());
            loadFitnessData(user.getUid());
            loadUserRanking(user.getEmail());
        } else {
            Intent intent = new Intent(getActivity(), Login.class);
            startActivity(intent);
            getActivity().finish();
        }


        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), Login.class);
            startActivity(intent);
            getActivity().finish();
        });


        return view;
    }
    private void loadWelcomeMessage(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String name = document.getString("name");
                if (name != null && !name.isEmpty()) {
                    welcomeText.setText("Welcome, " + name + "!");
                } else {
                    welcomeText.setText("Welcome!");
                }
            } else {
                welcomeText.setText("Welcome!");
            }
        }).addOnFailureListener(e -> {
            welcomeText.setText("Welcome!");
            Log.e("ProfileFragment", "Failed to load user name", e);
        });
    }


    private void checkAndUpdateStreak(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                int currentStreak = document.contains("streakCount") ? document.getLong("streakCount").intValue() : 0;
                String lastCheckIn = document.getString("lastCheckInDate");


                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String todayStr = sdf.format(new Date());


                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, -1);
                String yesterdayStr = sdf.format(cal.getTime());


                if (lastCheckIn == null || lastCheckIn.compareTo(yesterdayStr) < 0) {
                    currentStreak = 1;
                } else if (lastCheckIn.equals(yesterdayStr)) {
                    currentStreak += 1;
                }


                Map<String, Object> updates = new HashMap<>();
                updates.put("streakCount", currentStreak);
                updates.put("lastCheckInDate", todayStr);
                userRef.update(updates)
                        .addOnFailureListener(e -> Log.e("Streak", "Failed to update: " + e.getMessage()));


                streakText.setText("Daily Streak: " + currentStreak + " 🔥");
            } else {
                String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                Map<String, Object> initData = new HashMap<>();
                initData.put("streakCount", 1);
                initData.put("lastCheckInDate", todayStr);
                userRef.set(initData);
                streakText.setText("Daily Streak: 1 🔥");
            }
        });
    }


    private void loadFitnessData(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                if (document.contains("weight")) {
                    double weight = document.getDouble("weight");
                    weightText.setText("Weight: " + weight + " kg");
                } else {
                    weightText.setText("Weight: N/A");
                }


                if (document.contains("recommendedCalories")) {
                    long calories = document.getLong("recommendedCalories");
                    calorieText.setText("Recommended Intake: " + calories + " kcal/day");
                } else {
                    calorieText.setText("Recommended Intake: N/A");
                }
            } else {
                weightText.setText("Weight: N/A");
                calorieText.setText("Recommended Intake: N/A");
            }
        }).addOnFailureListener(e -> {
            weightText.setText("Weight: N/A");
            calorieText.setText("Recommended Intake: N/A");
            Log.e("ProfileFragment", "Error loading fitness data", e);
        });
    }


    private void loadUserRanking(String userEmail) {
        db.collection("users")
                .orderBy("points", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int rank = 1;
                    boolean found = false;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String email = doc.getString("email");
                        if (email != null && email.equals(userEmail)) {
                            rankingText.setText("Ranking: #" + rank);
                            found = true;
                            break;
                        }
                        rank++;
                    }
                    if (!found) {
                        rankingText.setText("Ranking: N/A");
                    }
                })
                .addOnFailureListener(e -> {
                    rankingText.setText("Ranking: N/A");
                    Log.e("ProfileFragment", "Error loading user ranking", e);
                });
    }
}
