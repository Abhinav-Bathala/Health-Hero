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

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {

    public interface OnWorkoutDeleteListener {
        void onDelete(WorkoutFragment.WorkoutEntry entry, int position);
    }

    private List<WorkoutFragment.WorkoutEntry> workoutHistoryList;
    private OnWorkoutDeleteListener deleteListener;

    // Cooldown variables
    private long lastDeleteClickTime = 0;
    private static final long DELETE_COOLDOWN_MS = 500; // 0.5 seconds

    public WorkoutAdapter(List<WorkoutFragment.WorkoutEntry> workoutHistoryList, OnWorkoutDeleteListener deleteListener) {
        this.workoutHistoryList = workoutHistoryList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutFragment.WorkoutEntry entry = workoutHistoryList.get(position);
        holder.workoutText.setText(entry.getWorkout());

        holder.deleteButton.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastDeleteClickTime < DELETE_COOLDOWN_MS) {
                Toast.makeText(v.getContext(), "Please wait before deleting again", Toast.LENGTH_SHORT).show();
                return;
            }

            lastDeleteClickTime = currentTime;

            if (deleteListener != null) {
                deleteListener.onDelete(entry, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return workoutHistoryList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView workoutText;
        ImageButton deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            workoutText = itemView.findViewById(R.id.workoutName);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
