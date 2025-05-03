package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class userforgotpass extends AppCompatActivity {

    private EditText editTextName, editTextUsername, editTextPassword;
    private Button saveButton, uploadButton;
    private ImageView profileImageView;
    private String userId;

    private static final int PICK_IMAGE_REQUEST = 1; // Image picker request code
    private Uri imageUri; // Uri for the selected image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userforgotpass); // Change to your activity layout

        // Initialize views
        editTextName = findViewById(R.id.editTextName);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        saveButton = findViewById(R.id.buttonSave);
        uploadButton = findViewById(R.id.buttonUpload);
        profileImageView = findViewById(R.id.imageViewProfile);

        // Retrieve user ID from Intent or other method
        userId = getIntent().getStringExtra("userId"); // Pass the user ID directly through intent
        if (userId == null) {
            Toast.makeText(this, "User ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
        }

        retrieveUserData();

        // Set onClickListener for saveButton
        saveButton.setOnClickListener(v -> saveData());

        // Set onClickListener for uploadButton
        uploadButton.setOnClickListener(v -> openImagePicker());
    }

    private void retrieveUserData() {
        if (userId.isEmpty()) {
            Toast.makeText(this, "User ID is empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId);
        Log.d("DatabaseRef", "Retrieving data for userId: " + userId);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String username = snapshot.child("username").getValue(String.class);
                    String profilePictureUrl = snapshot.child("profilePicture").getValue(String.class);

                    Log.d("UserData", "Name: " + name + ", Username: " + username);

                    // Set the values to the EditText fields
                    editTextName.setText(name);
                    editTextUsername.setText(username);

                    // Load the profile picture if exists
                    if (profilePictureUrl != null) {
                        Glide.with(userforgotpass.this).load(profilePictureUrl).into(profileImageView);
                    }
                } else {
                    Toast.makeText(userforgotpass.this, "User data not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(userforgotpass.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveData() {
        String name = editTextName.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Encrypt the password before saving
        String encryptedPassword = encryptPassword(password);

        if (encryptedPassword != null) {
            // Proceed with saving the data to Firebase
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId);
            databaseReference.child("name").setValue(name);
            databaseReference.child("password").setValue(encryptedPassword);  // Save the encrypted password
            databaseReference.child("username").setValue(username)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Password successfully changed", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(userforgotpass.this, Login.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to change password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Password encryption failed", Toast.LENGTH_SHORT).show();
        }
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


    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            uploadImageToFirebase(imageUri);
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        // Show a toast message indicating the upload is starting
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

        // Get a reference to Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("profile_pictures").child(userId + ".jpg");

        // Upload the image to Firebase Storage
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String profilePictureUrl = uri.toString();

                        // Update the user's profile picture URL in the Realtime Database
                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId);
                        databaseReference.child("profilePicture").setValue(profilePictureUrl);

                        // Display the uploaded image in the ImageView
                        Glide.with(userforgotpass.this).load(profilePictureUrl).into(profileImageView);

                        Toast.makeText(this, "Upload successful", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
