package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class conOne extends Fragment {

    private EditText editTextName, editTextUsername, editTextPassword;
    private Button saveButton, uploadButton, forgotpass;
    private ImageView profileImageView;
    private String userId;

    private static final int PICK_IMAGE_REQUEST = 1; // Image picker request code
    private Uri imageUri; // Uri for the selected image

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conone, container, false);

        // Initialize views
        editTextName = view.findViewById(R.id.editTextName);
        editTextUsername = view.findViewById(R.id.editTextUsername);
        forgotpass = view.findViewById(R.id.forgotpass);
        saveButton = view.findViewById(R.id.buttonSave);
        uploadButton = view.findViewById(R.id.buttonUpload);
        profileImageView = view.findViewById(R.id.imageViewProfile); // Ensure this exists in your layout

        // Check if the user is logged in
        checkUserSession();

        // Retrieve user ID and data
        userId = getUserId(); // Retrieve the logged-in user ID
        retrieveUserData();

        // Set onClickListener for saveButton
        saveButton.setOnClickListener(v -> saveData());

        // Set onClickListener for uploadButton
        uploadButton.setOnClickListener(v -> openImagePicker());

        // Set onClickListener for forgotpass button
        forgotpass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(requireContext(), forgotpass.class);
                startActivity(intent);
            }
        });

        return view;
    }


    private void checkUserSession() {
        // Check User login status
        SharedPreferences userPreferences = getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        boolean isUserLoggedIn = userPreferences.getBoolean("isUserLoggedIn", false);

        // Check Admin login status
        SharedPreferences adminPreferences = getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        boolean isAdminLoggedIn = adminPreferences.getBoolean("isLoggedIn", false);  // Assuming isLoggedIn is for admin

        if (!isUserLoggedIn && !isAdminLoggedIn) {
            // If neither user nor admin is logged in
            Toast.makeText(getActivity(), "User or Admin not logged in. Please log in.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), Login.class);
            startActivity(intent);
            getActivity().finish(); // Close current activity
        }
    }

    private String getUserId() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        return sharedPreferences.getString("userId", ""); // Retrieve userId from the session
    }

    private void retrieveUserData() {
        // Check if user or admin is logged in
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", "");  // Get userId from session
        boolean isAdminLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);  // Check if admin is logged in

        if (isAdminLoggedIn) {
            forgotpass.setEnabled(false);
            forgotpass.setAlpha(0.5f);
            Log.d("UserSession", "Retrieved userId: " + userId);  // Log the userId to check

            if (userId.isEmpty()) {
                // Handle case where userId is not found
                Log.d("UserSession", "userId is empty");
                return;
            }

            // Retrieve admin data
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("admin").child(userId);

            Log.d("DatabaseRef", "Retrieving admin data for adminId: " + userId);

            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String username = snapshot.child("username").getValue(String.class);
                        String profilePictureUrl = snapshot.child("profilePicture").getValue(String.class);

                        Log.d("AdminData", "Name: " + name + ", Username: " + username);

                        // Set the values to the EditText fields
                        editTextName.setText(name);
                        editTextUsername.setText(username);

                        // Load the profile picture if exists
                        if (profilePictureUrl != null) {
                            Glide.with(getActivity()).load(profilePictureUrl).into(profileImageView);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Admin data not found.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getActivity(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Retrieve user data
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
                            Glide.with(getActivity()).load(profilePictureUrl).into(profileImageView);
                        }
                    } else {
                        Toast.makeText(getActivity(), "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getActivity(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void saveData() {
        String name = editTextName.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();

        if (name.isEmpty() || username.isEmpty()) {
            Toast.makeText(getActivity(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the logged-in user is an admin
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        boolean isAdminLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

        DatabaseReference databaseReference;

        if (isAdminLoggedIn) {
            // If the logged-in user is an admin, save data under the "admin" node
            databaseReference = FirebaseDatabase.getInstance().getReference("admin").child(userId);
        } else {
            // If the logged-in user is a regular user, save data under the "users" node
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId);
        }

        // Save the data to the appropriate node
        databaseReference.child("name").setValue(name);
        databaseReference.child("username").setValue(username)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Data updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Failed to update data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            uploadImage();
        }
    }

    private void uploadImage() {
        if (imageUri != null) {
            StorageReference storageReference = FirebaseStorage.getInstance().getReference("profile_pictures")
                    .child(userId + ".jpg");

            storageReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String imageUrl = uri.toString();
                                updateProfilePicture(imageUrl);
                            }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(getActivity(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateProfilePicture(String imageUrl) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId);
        databaseReference.child("profilePicture").setValue(imageUrl)
                .addOnSuccessListener(aVoid -> {
                    Glide.with(getActivity()).load(imageUrl).into(profileImageView);
                    Toast.makeText(getActivity(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Failed to update profile picture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
