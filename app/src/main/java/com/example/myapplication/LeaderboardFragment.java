package com.example.myapplication;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;


public class LeaderboardFragment extends Fragment {

    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private List<LeaderboardEntry> leaderboardList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        recyclerView = view.findViewById(R.id.leaderboardRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LeaderboardAdapter(leaderboardList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        fetchLeaderboard();

        return view;
    }

    private void fetchLeaderboard() {
        db.collection("users")
                .orderBy("points", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    leaderboardList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        Long points = doc.getLong("points");
                        if (name != null && points != null) {
                            leaderboardList.add(new LeaderboardEntry(name, points));
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load leaderboard", Toast.LENGTH_SHORT).show();
                });
    }
}