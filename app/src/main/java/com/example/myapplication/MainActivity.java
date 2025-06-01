package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Main splash screen activity shown on app launch.
 * Redirects to Home if already logged in, otherwise shows options to log in or sign up.
 */
public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    /**
     * Called when the activity is starting.
     * Checks login status and sets up the splash screen with login and signup buttons.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        // Skip splash screen if user is already logged in
        if (user != null) {
            Intent intent = new Intent(MainActivity.this, Home.class); // <-- change if needed
            startActivity(intent);
            finish(); // closes splash activity so user can’t return with back button
            return;
        }

        // If not logged in, continue showing the splash layout
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Handle system UI insets (e.g., notch or status bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Navigate to Login activity when login button is clicked
        Button loginButton = findViewById(R.id.login_splash);
        loginButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, Login.class);
            startActivity(intent);
        });

        // Navigate to Signup activity when signup button is clicked
        Button signupButton = findViewById(R.id.signup_splash);
        signupButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, Signup.class);
            startActivity(intent);
        });
    }
}
