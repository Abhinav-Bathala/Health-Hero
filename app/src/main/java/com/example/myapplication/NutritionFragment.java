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
    private Button btnSubmit;
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
        }

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
                    transaction.set(dailyRef, Collections.singletonMap("totalCalories", updatedTotal), SetOptions.merge());
                    return null;
                }).addOnSuccessListener(unused -> loadTodayCalories(uid))
                .addOnFailureListener(e -> Log.e("Firestore", "Daily calorie update failed", e));
    }
}
