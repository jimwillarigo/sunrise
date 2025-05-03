package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class addUser extends AppCompatActivity {

    EditText signupName, signupUsername, signupPassword;
    TextView LoginRedirectText;
    Button signupButton;
    FirebaseDatabase database;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);

        signupName = findViewById(R.id.signup_name);
        signupUsername = findViewById(R.id.signup_username);
        signupPassword = findViewById(R.id.signup_password);

        signupButton = findViewById(R.id.signup_button);
        LoginRedirectText = findViewById(R.id.LoginRedirectText);

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = signupName.getText().toString().trim();
                String username = signupUsername.getText().toString().trim();
                String password = signupPassword.getText().toString().trim();

                if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(addUser.this, "All fields are required", Toast.LENGTH_SHORT).show();
                } else if (password.length() < 8) {
                    // Password too short
                    Toast.makeText(addUser.this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
                } else {
                    // Encrypt the password before saving
                    String encryptedPassword = encryptPassword(password);

                    if (encryptedPassword != null) {
                        database = FirebaseDatabase.getInstance();
                        // Reference to the 'pendingusers' node to check if the username already exists
                        DatabaseReference pendingUsersReference = database.getReference("pendingusers");

                        // Check if username already exists in 'pendingusers' node
                        pendingUsersReference.orderByChild("username").equalTo(username)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            // Username already exists in 'pendingusers' node
                                            Toast.makeText(addUser.this, "Username already exists in the pending users list", Toast.LENGTH_SHORT).show();
                                        } else {
                                            // Username is unique; proceed with signup
                                            DatabaseReference usersReference = database.getReference("users");

                                            // Check if username already exists in 'users' node
                                            usersReference.orderByChild("username").equalTo(username)
                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                                            if (dataSnapshot.exists()) {
                                                                // Username already exists in 'users' node
                                                                Toast.makeText(addUser.this, "Username already exists", Toast.LENGTH_SHORT).show();
                                                            } else {
                                                                // Username is unique; proceed with signup
                                                                reference = database.getReference("pendingusers");  // Reference to 'pendingusers' node
                                                                HelperClass helperClass = new HelperClass(name, username, encryptedPassword);

                                                                // Push data with a unique ID
                                                                String uniqueId = reference.push().getKey();
                                                                reference.child(uniqueId).setValue(helperClass)
                                                                        .addOnSuccessListener(aVoid -> {
                                                                            Toast.makeText(addUser.this, "Account under review, please wait for approval", Toast.LENGTH_SHORT).show();
                                                                            Intent intent = new Intent(addUser.this, admindashboard.class);
                                                                            startActivity(intent);
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            Toast.makeText(addUser.this, "Signup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                        });
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(DatabaseError databaseError) {
                                                            Toast.makeText(addUser.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        Toast.makeText(addUser.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(addUser.this, "Password encryption failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        LoginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(addUser.this, Login.class);
                startActivity(intent);
            }
        });
    }

    // Encrypt password using SHA-256
    private String encryptPassword(String password) {
        try {
            // Create MessageDigest instance for SHA-256
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            // Add password bytes to digest
            byte[] hashedBytes = messageDigest.digest(password.getBytes());

            // Convert byte array to hexadecimal format
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();  // Return hashed password
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;  // Return null if hashing fails
        }
    }

    // Email validation function (not used in this code, but might be useful)
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
