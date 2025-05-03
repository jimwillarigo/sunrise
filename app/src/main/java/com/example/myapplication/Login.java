package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Login extends AppCompatActivity {

    EditText loginUsername, logInPassword;
    Button loginButton;
    TextView signupRedirectText, adminRedirectText, error_msg, forgotpass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Check if user is already logged in
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isUserLoggedIn", false);
        if (isLoggedIn) {
            // If already logged in, navigate to MainActivity
            Intent intent = new Intent(Login.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close LoginActivity
            return;
        }

        loginUsername = findViewById(R.id.login_username);
        logInPassword = findViewById(R.id.login_password);
        signupRedirectText = findViewById(R.id.SignupRedirectText);
        adminRedirectText = findViewById(R.id.adminRedirectText);
        loginButton = findViewById(R.id.login_button);
        error_msg = findViewById(R.id.error_msg);
        forgotpass = findViewById(R.id.forgotpass);

        // Set onClickListener for loginButton
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validateUsername() || !validatePassword()) {
                    return;  // Exit early if validation fails
                } else {
                    checkUser();  // Proceed with checking user credentials
                }
            }
        });

        // Set onClickListener for signupRedirectText
        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, Signup.class);
                startActivity(intent);
            }
        });

        // Set onClickListener for forgotpass
        forgotpass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, forgotpass.class);
                startActivity(intent);
            }
        });

        // Set onClickListener for adminRedirectText
        adminRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, adminLogin.class);
                startActivity(intent);
            }
        });
    }

    // Validate username input
    public Boolean validateUsername() {
        String val = loginUsername.getText().toString();
        if (val.isEmpty()) {
            loginUsername.setError("Username field is empty");
            return false;
        } else {
            loginUsername.setError(null);
            return true;
        }
    }

    // Validate password input
    public Boolean validatePassword() {
        String val = logInPassword.getText().toString();
        if (val.isEmpty()) {
            logInPassword.setError("Password field is empty");
            return false;
        } else {
            logInPassword.setError(null);
            return true;
        }
    }

    // Check if the user exists and credentials match
    public void checkUser() {
        String userUsername = loginUsername.getText().toString().trim();
        String userPassword = logInPassword.getText().toString().trim();

        // Hash the entered password before checking
        String hashedPassword = hashPassword(userPassword);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        Query checkUserDatabase = reference.orderByChild("username").equalTo(userUsername);

        checkUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Username exists in "users" node
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String passwordFromDB = userSnapshot.child("password").getValue(String.class);
                        if (passwordFromDB != null && passwordFromDB.equals(hashedPassword)) {
                            // Successful login, store session
                            String nameFromDB = userSnapshot.child("name").getValue(String.class);
                            String userId = userSnapshot.getKey(); // Get the user ID
                            saveLoginSession(nameFromDB, userId); // Pass the userId to saveLoginSession

                            // Start MainActivity
                            Intent intent = new Intent(Login.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);  // Clear back stack
                            startActivity(intent);
                            return;  // Exit after successful login
                        }
                    }
                    // Password mismatch
                    showError("Invalid credentials. Please try again.");
                    logInPassword.requestFocus();
                } else {
                    // Username does not exist in "users" node
                    checkPendingUser(userUsername, hashedPassword);  // Check "pendinguser" node
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database error
                showError("Database error: " + error.getMessage());
            }
        });
    }

    private void checkPendingUser(String userUsername, String hashedPassword) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("pendingusers");
        Query checkPendingUserDatabase = reference.orderByChild("username").equalTo(userUsername);

        checkPendingUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Username exists in "pendingusers" node
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String dbUsername = userSnapshot.child("username").getValue(String.class);
                        String passwordFromDB = userSnapshot.child("password").getValue(String.class);

                        // Check if the username and password match
                        if (dbUsername != null && dbUsername.equals(userUsername) && passwordFromDB != null && passwordFromDB.equals(hashedPassword)) {
                            // Account is under review
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(Login.this, "Your account is under review, Please wait for admin approval.", Toast.LENGTH_SHORT).show();
                                }
                            });
                            return;  // Exit after showing the message
                        }
                    }
                    // If no matching username and password were found, show error
                    showError("Invalid credentials. Please try again.");
                    logInPassword.requestFocus();
                } else {
                    // Username does not exist in "pendingusers" node
                    showError("User does not exist.");
                    loginUsername.requestFocus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database error
                showError("Database error: " + error.getMessage());
            }
        });
    }

    // Show error message
    private void showError(String message) {
        error_msg.setText(message);
        error_msg.setVisibility(View.VISIBLE);  // Show the error message
    }

    // Save user session after successful login
    private void saveLoginSession(String name, String userId) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isUserLoggedIn", true);
        editor.putString("userName", name);     // Save the user's name
        editor.putString("userId", userId);     // Save the user's ID
        editor.apply();  // Apply changes
    }

    // Method to hash the password using SHA-256
    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
