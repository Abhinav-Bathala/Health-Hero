package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment responsible for managing user nutrition tracking.
 * Handles meal logging, calorie tracking, and daily goal evaluation.
 */
public class NutritionFragment extends Fragment {

    // Input fields
    private EditText etMealName, etCalories, etNotes;

    // Buttons
    private Button btnSubmit, btnFinishDay;

    // TextView to display total calories
    private TextView tvTotalCalories;

    // RecyclerView and Adapter for displaying meal history
    private RecyclerView recyclerView;
    private MealAdapter mealAdapter;

    // List to store meal entries
    private List<MealEntry> mealList = new ArrayList<>();

    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseUser user;

    /**
     * Inflates the layout and sets up all UI components and Firebase logic.
     *
     * @param inflater           The LayoutInflater object
     * @param container          The ViewGroup container
     * @param savedInstanceState Bundle with saved state
     * @return The root view of the fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nutrition, container, false);

        // Initialize UI elements
        etMealName = view.findViewById(R.id.meal_name);
        etCalories = view.findViewById(R.id.calorie_input);
        etNotes = view.findViewById(R.id.extra_notes);
        btnSubmit = view.findViewById(R.id.submit_meal);
        btnFinishDay = view.findViewById(R.id.btnFinishDay);
        tvTotalCalories = view.findViewById(R.id.tv_total_calories);
        recyclerView = view.findViewById(R.id.meal_history_recycler);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mealAdapter = new MealAdapter(mealList);
        recyclerView.setAdapter(mealAdapter);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Load data if user is authenticated
        if (user != null) {
            loadMealHistory();
            loadTodayCalories(user.getUid());
            checkIfFinishedAndDisable(user.getUid());
        }

        // Handle finishing day
        btnFinishDay.setOnClickListener(v -> {
            if (user != null) {
                evaluateNutritionGoalAndAwardPoints(user.getUid());
            }
        });

        // Handle meal submission
        btnSubmit.setOnClickListener(v -> {
            String name = etMealName.getText().toString().trim();
            String caloriesStr = etCalories.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();

            // Validate input
            if (name.isEmpty() || caloriesStr.isEmpty()) {
                Toast.makeText(getContext(), "Meal name and calories are required", Toast.LENGTH_SHORT).show();
                return;
            }

            int calories;
            try {
                calories = Integer.parseInt(caloriesStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Calories must be a number", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create timestamp and meal entry
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
            MealEntry entry = new MealEntry(name, calories, notes, timestamp);

            // Store in Firestore
            db.collection("users")
                    .document(user.getUid())
                    .collection("meals")
                    .add(entry)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(getContext(), "Meal logged!", Toast.LENGTH_SHORT).show();
                        mealList.add(0, entry);
                        mealAdapter.notifyItemInserted(0);
                        recyclerView.scrollToPosition(0);

                        etMealName.setText("");
                        etCalories.setText("");
                        etNotes.setText("");

                        updateDailyCalories(user.getUid(), calories);
                        awardPoints(user.getUid(), 1);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed to log meal", Toast.LENGTH_SHORT).show()
                    );
        });

        return view;
    }

    /**
     * Loads meal history from Firestore into the meal list.
     */
    private void loadMealHistory() {
        db.collection("users")
                .document(user.getUid())
                .collection("meals")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    mealList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        MealEntry entry = doc.toObject(MealEntry.class);
                        mealList.add(entry);
                    }
                    mealAdapter.notifyDataSetChanged();
                });
    }

    /**
     * Loads today's total calorie count from Firestore and updates the TextView.
     *
     * @param uid The user's UID
     */
    private void loadTodayCalories(String uid) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("users")
                .document(uid)
                .collection("dailyCalories")
                .document(today)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Long total = snapshot.getLong("totalCalories");
                        tvTotalCalories.setText("Today's Calories: " + (total != null ? total : 0));
                    } else {
                        tvTotalCalories.setText("Today's Calories: 0");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("NutritionFragment", "Failed to load daily calories", e);
                    tvTotalCalories.setText("Error loading calories");
                });
    }

    /**
     * Updates the user's total daily calories in Firestore.
     *
     * @param uid         The user's UID
     * @param newCalories Calories to add to the total
     */
    private void updateDailyCalories(String uid, int newCalories) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DocumentReference dailyRef = db.collection("users")
                .document(uid)
                .collection("dailyCalories")
                .document(today);

        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(dailyRef);
                    long currentTotal = 0;
                    if (snapshot.exists() && snapshot.contains("totalCalories")) {
                        Long val = snapshot.getLong("totalCalories");
                        if (val != null) currentTotal = val;
                    }
                    long updatedTotal = currentTotal + newCalories;
                    Map<String, Object> data = new HashMap<>();
                    data.put("totalCalories", updatedTotal);
                    data.put("finishedDay", false);
                    transaction.set(dailyRef, data, SetOptions.merge());
                    return null;
                }).addOnSuccessListener(unused -> loadTodayCalories(uid))
                .addOnFailureListener(e -> Log.e("Firestore", "Daily calorie update failed", e));
    }

    /**
     * Awards points to the user in Firestore.
     *
     * @param uid         The user's UID
     * @param pointsToAdd Number of points to add
     */
    private void awardPoints(String uid, int pointsToAdd) {
        DocumentReference userRef = db.collection("users").document(uid);

        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(userRef);
                    long currentPoints = 0;
                    if (snapshot.exists() && snapshot.contains("points")) {
                        Long val = snapshot.getLong("points");
                        if (val != null) currentPoints = val;
                    }
                    long updatedPoints = currentPoints + pointsToAdd;
                    transaction.update(userRef, "points", updatedPoints);
                    return null;
                }).addOnSuccessListener(unused -> Log.d("NutritionFragment", pointsToAdd + " points awarded."))
                .addOnFailureListener(e -> Log.e("NutritionFragment", "Failed to award points", e));
    }

    /**
     * Evaluates whether the user met their nutrition goal and awards bonus points if applicable.
     * Also marks the day as finished.
     *
     * @param uid The user's UID
     */
    private void evaluateNutritionGoalAndAwardPoints(String uid) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DocumentReference userRef = db.collection("users").document(uid);
        DocumentReference calorieRef = userRef.collection("dailyCalories").document(today);

        calorieRef.get().addOnSuccessListener(calorieSnap -> {
            if (!calorieSnap.exists()) return;

            Boolean alreadyFinished = calorieSnap.getBoolean("finishedDay");
            if (alreadyFinished != null && alreadyFinished) {
                Toast.makeText(getContext(), "You’ve already finished your day today.", Toast.LENGTH_SHORT).show();
                return;
            }

            userRef.get().addOnSuccessListener(userSnap -> {
                if (!userSnap.exists()) return;

                String goal = userSnap.getString("goal");
                Long recommendedCaloriesLong = userSnap.getLong("recommendedCalories");

                if (goal == null || recommendedCaloriesLong == null) {
                    Toast.makeText(getContext(), "No goal set. Set a goal to earn nutrition-based points.", Toast.LENGTH_LONG).show();
                    return;
                }

                int recommendedCalories = recommendedCaloriesLong.intValue();
                Long totalCaloriesLong = calorieSnap.getLong("totalCalories");
                if (totalCaloriesLong == null) return;

                int actualCalories = totalCaloriesLong.intValue();
                int calorieDiff = Math.abs(actualCalories - recommendedCalories);

                // Award points if within 100 calories of goal
                if (goal.equalsIgnoreCase("cutting") || goal.equalsIgnoreCase("bulking")) {
                    if (calorieDiff <= 100) {
                        awardPoints(uid, 100);
                        Toast.makeText(getContext(), "Awesome! You're within 100 calories of your goal. 100 points awarded!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Outside the 100-calorie target. 0 points awarded.", Toast.LENGTH_SHORT).show();
                    }
                }

                calorieRef.update("finishedDay", true)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("NutritionFragment", "Day marked as finished.");
                            disableInputs();
                        })
                        .addOnFailureListener(e -> Log.e("NutritionFragment", "Failed to mark day as finished", e));
            });
        });
    }

    /**
     * Disables all input fields and the submit button to prevent further input.
     */
    private void disableInputs() {
        etMealName.setEnabled(false);
        etCalories.setEnabled(false);
        etNotes.setEnabled(false);
        btnSubmit.setEnabled(false);
    }

    /**
     * Checks if the user has already finished their day and disables inputs accordingly.
     *
     * @param uid The user's UID
     */
    private void checkIfFinishedAndDisable(String uid) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DocumentReference calorieRef = db.collection("users")
                .document(uid)
                .collection("dailyCalories")
                .document(today);

        calorieRef.get().addOnSuccessListener(snap -> {
            Boolean done = snap.getBoolean("finishedDay");
            if (done != null && done) {
                disableInputs();
                btnFinishDay.setEnabled(false);
            }
        });
    }
}
