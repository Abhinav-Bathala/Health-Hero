package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {

    // UI elements
    TextInputEditText editTextEmail, editTextPassword;
    Button buttonLogin;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    TextView textView;
    Button backbutton_login;

    @Override
    public void onStart() {
        super.onStart();

        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            // If signed in, navigate directly to Home screen
            Intent intent = new Intent(getApplicationContext(), Home.class);
            startActivity(intent);
            finish();
        }
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth instance
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        editTextEmail = findViewById(R.id.email_login);
        editTextPassword = findViewById(R.id.password_login);
        buttonLogin = findViewById(R.id.login_login);
        progressBar = findViewById(R.id.progressBar_login);
        textView = findViewById(R.id.logintxt_login);

        // Set listener for text view (currently navigates to Home)
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Home.class);
                startActivity(intent);
                finish();
            }
        });

        // Set listener for back button to navigate to MainActivity
        backbutton_login = findViewById(R.id.backbutton_login);
        backbutton_login.setOnClickListener(view -> {
            Intent intent = new Intent(Login.this, MainActivity.class);
            startActivity(intent);
        });

        // Set listener for login button
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show progress bar
                progressBar.setVisibility(View.VISIBLE);

                // Get user input
                String email, password;
                email = String.valueOf(editTextEmail.getText());
                password = String.valueOf(editTextPassword.getText());

                // Validate input fields
                if (TextUtils.isEmpty(email)){
                    Toast.makeText(Login.this, "Enter Email: ", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)){
                    Toast.makeText(Login.this, "Enter Password: ", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Attempt to sign in with email and password
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                // Hide progress bar
                                progressBar.setVisibility(View.GONE);

                                if (task.isSuccessful()) {
                                    // Login success
                                    Log.d("FirebaseAuth", "Login successful: " + email);
                                    Toast.makeText(Login.this, "Login Successful.",
                                            Toast.LENGTH_SHORT).show();

                                    // Navigate to Home activity
                                    Intent intent = new Intent(getApplicationContext(), Home.class);
                                    startActivity(intent);
                                    finish();
                                }
                                else {
                                    // Login failed
                                    Exception e = task.getException();
                                    Log.e("FirebaseAuth", "Login Failed: " + (e != null ? e.getMessage() : "Unknown error"), e);
                                    Toast.makeText(Login.this, "Login Failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        // Handle system UI insets (e.g., for notch or system bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
