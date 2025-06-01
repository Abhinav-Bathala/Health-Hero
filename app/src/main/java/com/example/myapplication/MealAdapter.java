package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter class for displaying a list of meal entries in a RecyclerView.
 */
public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    // List to store meal entries
    private List<MealEntry> mealList;

    /**
     * Constructor to initialize the adapter with a list of meal entries.
     *
     * @param mealList List of meals to be displayed
     */
    public MealAdapter(List<MealEntry> mealList) {
        this.mealList = mealList;
    }

    /**
     * Called when the RecyclerView needs a new ViewHolder.
     *
     * @param parent   The parent ViewGroup
     * @param viewType The view type of the new View
     * @return A new MealViewHolder that holds the layout for each meal item
     */
    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for individual meal item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal, parent, false);
        return new MealViewHolder(view);
    }

    /**
     * Called to bind data to a ViewHolder at the specified position.
     *
     * @param holder   The ViewHolder to bind data to
     * @param position The position of the item within the dataset
     */
    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        // Get the meal at the current position
        MealEntry meal = mealList.get(position);

        // Set meal details in the view
        holder.name.setText(meal.getMealName());
        holder.calories.setText(meal.getCalories() + " kcal");
        holder.notes.setText(meal.getNotes());
        holder.timestamp.setText(meal.getTimestamp());

        // Get context for color theming
        Context context = holder.itemView.getContext();

        // Set background and text colors for dark theme
        holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.black));
        holder.name.setTextColor(ContextCompat.getColor(context, R.color.white));
        holder.calories.setTextColor(ContextCompat.getColor(context, R.color.white));
        holder.notes.setTextColor(ContextCompat.getColor(context, R.color.white));
        holder.timestamp.setTextColor(ContextCompat.getColor(context, R.color.grey));
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The number of items
     */
    @Override
    public int getItemCount() {
        return mealList.size();
    }

    /**
     * ViewHolder class to hold view references for a meal item.
     */
    static class MealViewHolder extends RecyclerView.ViewHolder {

        // TextViews for displaying meal information
        TextView name, calories, notes, timestamp;

        /**
         * Constructor to initialize view references.
         *
         * @param itemView The view of the individual meal item
         */
        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvMealName);
            calories = itemView.findViewById(R.id.tvCalories);
            notes = itemView.findViewById(R.id.tvNotes);
            timestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
