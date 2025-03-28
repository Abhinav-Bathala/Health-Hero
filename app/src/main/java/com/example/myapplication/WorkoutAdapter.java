package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {
    private List<String> workoutHistoryList;

    public WorkoutAdapter(List<String> workoutHistoryList) {
        this.workoutHistoryList = workoutHistoryList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.workoutText.setText(workoutHistoryList.get(position));
    }

    @Override
    public int getItemCount() {
        return workoutHistoryList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView workoutText;

        ViewHolder(View itemView) {
            super(itemView);
            workoutText = itemView.findViewById(R.id.workoutName);
        }
    }
}
