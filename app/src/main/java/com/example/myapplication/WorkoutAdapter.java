package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * RecyclerView Adapter to display a list of workout history entries
 * with the ability to delete individual entries.
 */
public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {

    /**
     * Interface to notify when a workout entry is deleted.
     */
    public interface OnWorkoutDeleteListener {
        /**
         * Called when a workout entry is deleted.
         *
         * @param entry The workout entry that was deleted.
         * @param position The position of the entry in the list.
         */
        void onDelete(WorkoutFragment.WorkoutEntry entry, int position);
    }

    // List holding workout history entries to display
    private List<WorkoutFragment.WorkoutEntry> workoutHistoryList;
    // Listener for delete events
    private OnWorkoutDeleteListener deleteListener;

    // Timestamp of the last delete click for cooldown logic
    private long lastDeleteClickTime = 0;
    // Minimum cooldown time between deletes (in milliseconds)
    private static final long DELETE_COOLDOWN_MS = 500; // 0.5 seconds

    /**
     * Constructs the WorkoutAdapter with the workout history list and a delete listener.
     *
     * @param workoutHistoryList List of workout history entries to display.
     * @param deleteListener Listener to handle delete actions on entries.
     */
    public WorkoutAdapter(List<WorkoutFragment.WorkoutEntry> workoutHistoryList, OnWorkoutDeleteListener deleteListener) {
        this.workoutHistoryList = workoutHistoryList;
        this.deleteListener = deleteListener;
    }

    /**
     * Inflates the item layout and returns a ViewHolder for it.
     *
     * @param parent The parent ViewGroup.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds the inflated view.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds the workout data to the views for the specified position.
     *
     * @param holder The ViewHolder to bind data to.
     * @param position The position of the item in the data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutFragment.WorkoutEntry entry = workoutHistoryList.get(position);
        // Set workout name text
        holder.workoutText.setText(entry.getWorkout());

        // Set click listener for the delete button
        holder.deleteButton.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            // Check cooldown to prevent rapid repeated deletes
            if (currentTime - lastDeleteClickTime < DELETE_COOLDOWN_MS) {
                Toast.makeText(v.getContext(), "Please wait before deleting again", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update last click time to current
            lastDeleteClickTime = currentTime;

            // Notify listener if set about delete action
            if (deleteListener != null) {
                deleteListener.onDelete(entry, position);
            }
        });
    }

    /**
     * Returns the total number of workout entries.
     *
     * @return Number of items in the workout history list.
     */
    @Override
    public int getItemCount() {
        return workoutHistoryList.size();
    }

    /**
     * ViewHolder class holds references to the views for each workout item.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        // TextView displaying workout name
        TextView workoutText;
        // Button to trigger delete action
        ImageButton deleteButton;

        /**
         * ViewHolder constructor finds and binds views by their IDs.
         *
         * @param itemView The item view inflated for the list entry.
         */
        ViewHolder(View itemView) {
            super(itemView);
            workoutText = itemView.findViewById(R.id.workoutName);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
