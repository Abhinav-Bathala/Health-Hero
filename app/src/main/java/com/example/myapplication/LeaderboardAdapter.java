package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Adapter class for the RecyclerView displaying leaderboard entries
public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    // List holding the leaderboard data entries
    private List<LeaderboardEntry> leaderboardList;
    // Constructor to initialize the adapter with the leaderboard list
    public LeaderboardAdapter(List<LeaderboardEntry> leaderboardList) {
        this.leaderboardList = leaderboardList;
    }
    // ViewHolder class holds references to the views in each item of the RecyclerView
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankText, emailText, pointsText;
        // Constructor binds the view components from the item layout
        public ViewHolder(View view) {
            super(view);
            rankText = view.findViewById(R.id.rankText);
            emailText = view.findViewById(R.id.emailText);
            pointsText = view.findViewById(R.id.pointsText);
        }
    }
    // Called when the RecyclerView needs a new ViewHolder
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the leaderboard item layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }
    // Called to bind data to the views in each ViewHolder
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the leaderboard entry at the current position
        LeaderboardEntry entry = leaderboardList.get(position);
        // Set rank as position + 1 (since position starts at 0)
        holder.rankText.setText(String.valueOf(position + 1));
        // Set the user's NAME and points
        holder.emailText.setText(entry.getEmail());
        holder.pointsText.setText("Points: " + entry.getPoints());
    }
    // Returns the total number of items in the list
    @Override
    public int getItemCount() {
        return leaderboardList.size();
    }
}
