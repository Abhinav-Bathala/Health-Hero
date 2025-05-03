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

import com.example.myapplication.MealAdapter;
import com.example.myapplication.MealEntry;
import com.example.myapplication.R;
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

public class NutritionFragment extends Fragment {

    private EditText etMealName, etCalories, etNotes;
    private Button btnSubmit, btnFinishDay;
    private TextView tvTotalCalories;
    private RecyclerView recyclerView;
    private MealAdapter mealAdapter;
    private List<MealEntry> mealList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nutrition, container, false);

        etMealName = view.findViewById(R.id.meal_name);
        etCalories = view.findViewById(R.id.calorie_input);
        etNotes = view.findViewById(R.id.extra_notes);
        btnSubmit = view.findViewById(R.id.submit_meal);
        btnFinishDay = view.findViewById(R.id.btnFinishDay);
        tvTotalCalories = view.findViewById(R.id.tv_total_calories);
        recyclerView = view.findViewById(R.id.meal_history_recycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mealAdapter = new MealAdapter(mealList);
        recyclerView.setAdapter(mealAdapter);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            loadMealHistory();
            loadTodayCalories(user.getUid());
            checkIfFinishedAndDisable(user.getUid());

        }
        btnFinishDay.setOnClickListener(v -> {
            if (user != null) {
                evaluateNutritionGoalAndAwardPoints(user.getUid());
            }
        });

        btnSubmit.setOnClickListener(v -> {
            String name = etMealName.getText().toString().trim();
            String caloriesStr = etCalories.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();

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

            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
            MealEntry entry = new MealEntry(name, calories, notes, timestamp);

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
                        awardPoints(user.getUid(), 10);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed to log meal", Toast.LENGTH_SHORT).show()
                    );
        });

        return view;
    }

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
                        if (total != null) {
                            tvTotalCalories.setText("Today's Calories: " + total);
                        } else {
                            tvTotalCalories.setText("Today's Calories: 0");
                        }
                    } else {
                        tvTotalCalories.setText("Today's Calories: 0");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("NutritionFragment", "Failed to load daily calories", e);
                    tvTotalCalories.setText("Error loading calories");
                });
    }

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
                        if (val != null) {
                            currentTotal = val;
                        }
                    }
                    long updatedTotal = currentTotal + newCalories;
                    Map<String, Object> data = new HashMap<>();
                    data.put("totalCalories", updatedTotal);
                    data.put("finishedDay", false); // Only gets overwritten if missing
                    transaction.set(dailyRef, data, SetOptions.merge());
                    return null;
                }).addOnSuccessListener(unused -> loadTodayCalories(uid))
                .addOnFailureListener(e -> Log.e("Firestore", "Daily calorie update failed", e));
    }

    private void awardPoints(String uid, int pointsToAdd) {
        DocumentReference userRef = db.collection("users").document(uid);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);
            long currentPoints = 0;
            if (snapshot.exists() && snapshot.contains("points")) {
                Long val = snapshot.getLong("points");
                if (val != null) {
                    currentPoints = val;
                }
            }
            long updatedPoints = currentPoints + pointsToAdd;
            transaction.update(userRef, "points", updatedPoints);
            return null;
        }).addOnSuccessListener(unused -> {
            Log.d("NutritionFragment", pointsToAdd + " points awarded.");
        }).addOnFailureListener(e -> {
            Log.e("NutritionFragment", "Failed to award points", e);
        });
    }
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

                if (goal.equalsIgnoreCase("cutting") || goal.equalsIgnoreCase("bulking")) {
                    if (calorieDiff <= 100) {
                        awardPoints(uid, 100);
                        Toast.makeText(getContext(), "Awesome! You're within 100 calories of your goal. 100 points awarded!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Outside the 100-calorie target. 0 points awarded.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Fallback to performance-based for other goal types
                    int tdee = userSnap.getLong("tdee") != null ? userSnap.getLong("tdee").intValue() : 0;
                    int goalDelta = Math.abs(tdee - recommendedCalories);
                    int actualDelta = Math.abs(tdee - actualCalories);

                    float performanceRatio = Math.min(1f, (float) actualDelta / goalDelta);
                    int pointsEarned = Math.round(performanceRatio * 100);

                    awardPoints(uid, pointsEarned);
                    Toast.makeText(getContext(), pointsEarned + " points awarded for today!", Toast.LENGTH_SHORT).show();
                }

                // Mark the day as finished either way
                calorieRef.update("finishedDay", true)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("NutritionFragment", "Day marked as finished.");

                            disableInputs();

                        })
                        .addOnFailureListener(e -> Log.e("NutritionFragment", "Failed to mark day as finished", e));
            });
        });
    }
    private void disableInputs() {
        etMealName.setEnabled(false);
        etCalories .setEnabled(false);
        etNotes    .setEnabled(false);
        btnSubmit  .setEnabled(false);
    }

    private void checkIfFinishedAndDisable(String uid) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
        DocumentReference calorieRef = db.collection("users")
                .document(uid)
                .collection("dailyCalories")
                .document(today);
        calorieRef.get().addOnSuccessListener(snap -> {
            Boolean done = snap.getBoolean("finishedDay");
            if (done != null && done) {
                disableInputs();
            }
        });
    }


}



