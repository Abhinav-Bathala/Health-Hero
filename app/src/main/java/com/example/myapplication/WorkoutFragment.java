package com.example.myapplication;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WorkoutFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WorkoutFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private Spinner spinner1, spinner2, spinner3;  // Added spinner3 for sets

    private ArrayList<String> workoutArray;
    private ArrayList<Integer> repsArray;
    private ArrayList<Integer> setsArray;  // Added setsArray

    public WorkoutFragment() {
        // Required empty public constructor
    }

    public static WorkoutFragment newInstance(String param1, String param2) {
        WorkoutFragment fragment = new WorkoutFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // Initialize and populate workoutArray with a placeholder
        workoutArray = new ArrayList<>();
        workoutArray.add("Select a workout"); // Placeholder item
        workoutArray.add("Bench Press");
        workoutArray.add("Squats");
        workoutArray.add("Deadlifts");
        workoutArray.add("Pull-ups");
        workoutArray.add("Lunges");
        workoutArray.add("Push-ups");
        workoutArray.add("Planks");
        workoutArray.add("Jump Rope");

        // Initialize and populate repsArray with a placeholder
        repsArray = new ArrayList<>();
        repsArray.add(0); // Placeholder item
        repsArray.add(5);
        repsArray.add(8);
        repsArray.add(10);
        repsArray.add(12);
        repsArray.add(15);
        repsArray.add(20);

        // Initialize and populate setsArray with a placeholder
        setsArray = new ArrayList<>();
        setsArray.add(0); // Placeholder item
        setsArray.add(3);
        setsArray.add(4);
        setsArray.add(5);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_workout, container, false);

        // Initialize the spinners
        spinner1 = view.findViewById(R.id.workoutSelector);
        spinner2 = view.findViewById(R.id.repsSelector);
        spinner3 = view.findViewById(R.id.setsSelector);

        // Set up the ArrayAdapter for workoutArray
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, workoutArray);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner1.setAdapter(adapter1);
        spinner1.setSelection(0, false); // Set default to placeholder without triggering listener

        // Set up the ArrayAdapter for repsArray
        ArrayAdapter<Integer> adapter2 = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, repsArray);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(adapter2);
        spinner2.setSelection(0, false); // Default to placeholder

        // Set up the ArrayAdapter for setsArray
        ArrayAdapter<Integer> adapter3 = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, setsArray);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner3.setAdapter(adapter3);
        spinner3.setSelection(0, false); // Default to placeholder

        return view;
    }
}