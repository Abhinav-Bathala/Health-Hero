package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {
    private List<MealEntry> mealList;

    public MealAdapter(List<MealEntry> mealList) {
        this.mealList = mealList;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        MealEntry meal = mealList.get(position);
        holder.name.setText(meal.getMealName());
        holder.calories.setText(meal.getCalories() + " kcal");
        holder.notes.setText(meal.getNotes());
        holder.timestamp.setText(meal.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return mealList.size();
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        TextView name, calories, notes, timestamp;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvMealName);
            calories = itemView.findViewById(R.id.tvCalories);
            notes = itemView.findViewById(R.id.tvNotes);
            timestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}

