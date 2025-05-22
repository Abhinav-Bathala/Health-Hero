package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<LeaderboardEntry> leaderboardList;

    public LeaderboardAdapter(List<LeaderboardEntry> leaderboardList) {
        this.leaderboardList = leaderboardList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankText, emailText, pointsText;

        public ViewHolder(View view) {
            super(view);
            rankText = view.findViewById(R.id.rankText);
            emailText = view.findViewById(R.id.emailText);
            pointsText = view.findViewById(R.id.pointsText);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardEntry entry = leaderboardList.get(position);
        holder.rankText.setText(String.valueOf(position + 1));
        holder.emailText.setText(entry.getEmail());
        holder.pointsText.setText("Points: " + entry.getPoints());
    }

    @Override
    public int getItemCount() {
        return leaderboardList.size();
    }
}
