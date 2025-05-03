package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class adminLogin extends AppCompatActivity {

    EditText loginUsername, logInPassword;
    Button loginButton;
    TextView signupRedirectText, adminRedirectText, error_msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        // Check if user is already logged in
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if (isLoggedIn) {
            // If already logged in, navigate to admindashboard
            Intent intent = new Intent(adminLogin.this, admindashboard.class);
            startActivity(intent);
            finish(); // Close LoginActivity
            return;
        }

        loginUsername = findViewById(R.id.login_username);
        logInPassword = findViewById(R.id.login_password);
        adminRedirectText = findViewById(R.id.adminRedirectText);
        loginButton = findViewById(R.id.login_button);
        error_msg = findViewById(R.id.error_msg);

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
        adminRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(adminLogin.this, Login.class);
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

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("admin");
        Query checkUserDatabase = reference.orderByChild("username").equalTo(userUsername);

        checkUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Username exists
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String passwordFromDB = userSnapshot.child("password").getValue(String.class);
                        if (passwordFromDB != null && passwordFromDB.equals(userPassword)) {
                            // Successful login, store session
                            String nameFromDB = userSnapshot.child("name").getValue(String.class);
                            String userId = userSnapshot.getKey(); // This gets the userId (unique key in Firebase)
                            saveLoginSession(nameFromDB, userId);  // Save userId

                            // Start admindashboard
                            Intent intent = new Intent(adminLogin.this, admindashboard.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);  // Clear back stack
                            startActivity(intent);
                            return;  // Exit after successful login
                        }
                    }
                    // Password mismatch
                    showError("Invalid credentials. Please try again.");
                    logInPassword.requestFocus();
                } else {
                    // Username does not exist
                    showError("Account does not exist.");
                    loginUsername.requestFocus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error if any
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
        editor.putBoolean("isLoggedIn", true);  // Save the login state
        editor.putString("userName", name);     // Save the user's name
        editor.putString("userId", userId);     // Save the admin userId
        editor.apply();  // Apply changes
    }
}
