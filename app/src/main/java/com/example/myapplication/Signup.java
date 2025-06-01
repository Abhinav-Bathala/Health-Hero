package com.example.myapplication;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.credentials.CredentialManager;
import android.credentials.GetCredentialRequest;
import android.nfc.Tag;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Signup extends AppCompatActivity {

    // Input fields for user email, password, and name
    TextInputEditText editTextEmail, editTextPassword, editTextName;
    // Button to trigger registration
    Button buttonReg;
    // Firebase Authentication instance
    FirebaseAuth mAuth;
    // TextView for switching to login screen
    TextView textView;

    // Firestore database instance
    FirebaseFirestore fStore;
    // Stores the current user's ID after registration
    String userID;

    /**
     * Called when the activity is becoming visible to the user.
     * Checks if a user is already logged in, and if so, navigates to the Login activity.
     */
    @Override
    public void onStart() {
        super.onStart();
        // Check if a user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            // If logged in, navigate to Login activity and finish current one
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();

        }
    }

     /**
     * Called when the activity is first created.
     * Sets up UI elements, initializes Firebase instances,
     * configures button listeners for signup and navigation,
     * and handles user registration logic.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable edge-to-edge display for the activity
        EdgeToEdge.enable(this);
        // Set the layout for the signup activity
        setContentView(R.layout.activity_signup);
        // Adjust padding for system bars (status/navigation bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });

        // Find and setup back button to return to MainActivity
        Button backbutton_signup = findViewById(R.id.backbutton_signup);
        backbutton_signup.setOnClickListener(view -> {
            Intent intent = new Intent(Signup.this, MainActivity.class);
            startActivity(intent);
        });

        // Initialize FirebaseAuth and Firestore instances
        mAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        // Bind UI elements to variables
        editTextEmail = findViewById(R.id.email_signup);
        editTextPassword = findViewById(R.id.password_signup);
        editTextName = findViewById(R.id.name_signup);
        buttonReg = findViewById(R.id.signup_signup);
        textView = findViewById(R.id.createtxt_signup);

        // Set click listener for TextView to navigate to Login activity
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });

        // Set click listener for registration button
        buttonReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get input values from text fields
                String email, password, name;
                email = String.valueOf(editTextEmail.getText());
                password = String.valueOf(editTextPassword.getText());
                name = String.valueOf(editTextName.getText());

                // Check if email is empty, show message if true
                if (TextUtils.isEmpty(email)){
                    Toast.makeText(Signup.this, "Enter Email: ", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Check if password is empty, show message if true
                if (TextUtils.isEmpty(password)){
                    Toast.makeText(Signup.this, "Enter Password: ", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Check if name is empty, show message if true
                if (TextUtils.isEmpty(name)){
                    Toast.makeText(Signup.this, "Enter Name: ", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Attempt to create user with email and password in FirebaseAuth
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            /**
                             * Called when the createUserWithEmailAndPassword task completes.
                             * If successful, creates a Firestore user document and navigates to the Home activity.
                             * Otherwise, shows an authentication failure message.
                             *
                             * @param task The task containing result of the user creation attempt.
                             */
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Notify user that account was created successfully
                                    Toast.makeText(Signup.this, "Account Created.",
                                            Toast.LENGTH_SHORT).show();
                                    // Get the new user's unique ID
                                    userID = mAuth.getCurrentUser().getUid();
                                    // Reference to the Firestore document for this user
                                    DocumentReference documentReference = fStore.collection("users").document(userID);
                                    // Create a map of user info to save in Firestore
                                    Map<String, Object> user = new HashMap<>();
                                    user.put("email", email);
                                    user.put("points", 0);
                                    user.put("name", name);

                                    // Save user info in Firestore document
                                    documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            // Log success message on profile creation
                                            Log.d(TAG, "onSuccess: user profile is created for" + userID);
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Log failure message on profile creation
                                            Log.d(TAG, "onFailure: " + e.toString());
                                        }
                                    });
                                    // Navigate to Home activity and finish signup activity
                                    Intent intent = new Intent(getApplicationContext(), Home.class);
                                    startActivity(intent);
                                    finish();

                                }

                                else {
                                    // If sign in fails, show an error message to the user
                                    Toast.makeText(Signup.this, "Authentication Failed.",
                                            Toast.LENGTH_SHORT).show();

                                }
                            }
                        });

            }
        });

    }
}
