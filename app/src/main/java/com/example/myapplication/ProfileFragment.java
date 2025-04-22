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

    private TextView emailTextView, streakText, weightText, calorieText, rankingText, welcomeText, quoteText;
    private Button logoutButton;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Firebase
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // View bindings
        emailTextView = view.findViewById(R.id.user_details);
        logoutButton = view.findViewById(R.id.logout);
        streakText = view.findViewById(R.id.streakText);
        weightText = view.findViewById(R.id.weightText);
        calorieText = view.findViewById(R.id.calorieText);
        rankingText = view.findViewById(R.id.rankingText);
        welcomeText = view.findViewById(R.id.welcometext);
        quoteText = view.findViewById(R.id.quotetext);

        if (user != null) {
            emailTextView.setText(user.getEmail());
            loadWelcomeMessage(user.getUid());
            checkAndUpdateStreak(user.getUid());
            loadFitnessData(user.getUid());
            loadUserRanking(user.getEmail());
        } else {
            startActivity(new Intent(getActivity(), Login.class));
            getActivity().finish();
        }

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), Login.class));
            getActivity().finish();
        });

        return view;
    }

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

            if (lastCheckIn == null || lastCheckIn.compareTo(yesterdayStr) < 0) {
                currentStreak = 1;
                streakIncreased = true;
            } else if (lastCheckIn.equals(yesterdayStr)) {
                currentStreak += 1;
                streakIncreased = true;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("streakCount", currentStreak);
            updates.put("lastCheckInDate", todayStr);
            userRef.update(updates).addOnFailureListener(e -> Log.e("Streak", "Failed to update: " + e.getMessage()));

            streakText.setText("Streak:\n" + currentStreak + " 🔥");

            if (streakIncreased) {
                displayAndSaveQuote(userRef);
            } else if (savedQuote != null && !savedQuote.isEmpty()) {
                quoteText.setText("\"" + savedQuote + "\"");
            } else {
                quoteText.setText("- Keep pushing forward!");
            }

        }).addOnFailureListener(e -> Log.e("Streak", "Failed to load streak data", e));
    }


    private void displayRandomQuote() {
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
                quoteText.setText("\" + selectedQuote + \"");
            }
        } catch (Exception e) {
            Log.e("QuoteError", "Could not load quote", e);
            quoteText.setText("- Stay strong and keep going!");
        }
    }

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
                quoteText.setText("\"" + selectedQuote + "\"");

                // Save the quote to Firestore
                Map<String, Object> quoteUpdate = new HashMap<>();
                quoteUpdate.put("streakQuote", selectedQuote);
                userRef.update(quoteUpdate).addOnFailureListener(e -> Log.e("QuoteSave", "Failed to save quote", e));
            }
        } catch (Exception e) {
            Log.e("QuoteError", "Could not load quote", e);
            quoteText.setText("- Stay strong and keep going!");
        }
    }



    private void loadFitnessData(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.get().addOnSuccessListener(document -> {
            if (document.contains("weight")) {
                weightText.setText("Progress:\n" + document.getDouble("weight") + " lb");
            } else {
                weightText.setText("Progress:\nN/A");
            }

            if (document.contains("recommendedCalories")) {
                calorieText.setText("Calorie Intake:\n" + document.getLong("recommendedCalories") + " kcal");
            } else {
                calorieText.setText("Calorie Intake:\nN/A");
            }
        }).addOnFailureListener(e -> Log.e("FitnessData", "Failed to load fitness data", e));
    }

    private void loadUserRanking(String userEmail) {
        db.collection("users")
                .orderBy("points", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int rank = 1;
                    for (DocumentSnapshot doc : querySnapshot) {
                        if (userEmail.equals(doc.getString("email"))) {
                            rankingText.setText("Ranking:\n#" + rank);
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