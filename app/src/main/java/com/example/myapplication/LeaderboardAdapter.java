package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter class for populating the RecyclerView that displays leaderboard entries.
 * Each item shows a user's rank, email (or name), and point total.
 */
public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    // List holding the leaderboard data entries
    private List<LeaderboardEntry> leaderboardList;

    /**
     * Constructor to initialize the adapter with a list of leaderboard entries.
     *
     * @param leaderboardList A list of LeaderboardEntry objects to display
     */
    public LeaderboardAdapter(List<LeaderboardEntry> leaderboardList) {
        this.leaderboardList = leaderboardList;
    }

    /**
     * ViewHolder class that holds references to the views in each RecyclerView item.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankText, emailText, pointsText;

        /**
         * Constructor that binds the view components from the leaderboard item layout.
         *
         * @param view The root view of the leaderboard item layout
         */
        public ViewHolder(View view) {
            super(view);
            rankText = view.findViewById(R.id.rankText);
            emailText = view.findViewById(R.id.emailText);
            pointsText = view.findViewById(R.id.pointsText);
        }
    }

    /**
     * Called when the RecyclerView needs a new ViewHolder to represent an item.
     *
     * @param parent   The ViewGroup into which the new view will be added
     * @param viewType The view type of the new view
     * @return A new ViewHolder that holds the view for the leaderboard item
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the leaderboard item layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Called by RecyclerView to display data at a specified position.
     * Updates the contents of the ViewHolder to reflect the item at that position.
     *
     * @param holder   The ViewHolder to update
     * @param position The position of the item in the data set
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the leaderboard entry at the current position
        LeaderboardEntry entry = leaderboardList.get(position);
        // Set rank as position + 1 (since position starts at 0)
        holder.rankText.setText(String.valueOf(position + 1));
        // Set the user's email and points
        holder.emailText.setText(entry.getEmail());
        holder.pointsText.setText("Points: " + entry.getPoints());
    }

    /**
     * Returns the total number of leaderboard entries.
     *
     * @return The size of the leaderboard list
     */
    @Override
    public int getItemCount() {
        return leaderboardList.size();
    }
}
