package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class forgotpass extends AppCompatActivity {

    private EditText etUsername;
    private TextView pleasesel, tapsearch;
    private Button btnSearch;
    private RecyclerView rvImages;
    private ImageAdapter imageAdapter;
    private DatabaseReference dbRef;
    private Button confirmButton; // The button that appears when the real image is selected
    private String userId; // Declare userId variable
    private ImageView verifiedlogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgotpass);

        tapsearch = findViewById(R.id.tapsearch);
        verifiedlogo = findViewById(R.id.verifiedlogo);
        pleasesel = findViewById(R.id.pleasesel);
        etUsername = findViewById(R.id.etUsername);
        btnSearch = findViewById(R.id.btnSearch);
        rvImages = findViewById(R.id.rvImages);
        confirmButton = findViewById(R.id.btnConfirm); // Your button to show when the real image is selected

        // Set up RecyclerView with GridLayoutManager
        rvImages.setLayoutManager(new GridLayoutManager(this, 3)); // Grid layout with 3 columns
        imageAdapter = new ImageAdapter(this, new ArrayList<>(), "",userId); // Pass empty list initially
        rvImages.setAdapter(imageAdapter);

        dbRef = FirebaseDatabase.getInstance().getReference("users");

        btnSearch.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
                return;
            }
            searchUsername(username);
            Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show();
        });
    }

    private void searchUsername(String username) {
        dbRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        userId = userSnapshot.getKey(); // Save the userId
                        fetchUserHistory(userId); // Fetch user history after finding the user
                        pleasesel.setVisibility(View.VISIBLE);
                        tapsearch.setVisibility(View.VISIBLE);
                        etUsername.setEnabled(false);
                        etUsername.setAlpha(0.5f);

                        // Pass userId to the ImageAdapter and show images
                        imageAdapter = new ImageAdapter(forgotpass.this, new ArrayList<>(), imageAdapter.toString(), userId);
                        rvImages.setAdapter(imageAdapter);

                        // No need to update attempt count here; it should happen in showConfirmationDialog

                        return;
                    }
                } else {
                    Toast.makeText(forgotpass.this, "Username not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(forgotpass.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void fetchUserHistory(String userId) {
        dbRef.child(userId).child("history").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<String> images = new ArrayList<>();
                    for (DataSnapshot historySnapshot : dataSnapshot.getChildren()) {
                        String imageUrl = historySnapshot.child("imageUrl").getValue(String.class);
                        if (imageUrl != null) {
                            images.add(imageUrl);
                        }
                    }

                    if (!images.isEmpty()) {
                        // Select a random image as the real image
                        Random random = new Random();
                        String realImage = images.get(random.nextInt(images.size())); // Randomly select an image

                        // Fetch decoy images and update the adapter only after processing is done
                        fetchDecoyImages(realImage, new DecoyImagesCallback() {
                            @Override
                            public void onDecoyImagesFetched(List<String> finalImages) {
                                runOnUiThread(() -> imageAdapter.updateImages(finalImages, realImage));
                                showConfirmationButton(); // Show the confirmation button after images are fetched
                            }
                        });
                    }
                } else {
                    Toast.makeText(forgotpass.this, "No history found for this user", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(forgotpass.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void showConfirmationButton() {
        // Ensure that the views are initialized correctly before use
        confirmButton.setVisibility(View.VISIBLE); // Show the confirmation button
        verifiedlogo.setVisibility(View.VISIBLE);
        btnSearch.setEnabled(false);
        btnSearch.setAlpha(0.5f);
        tapsearch.setVisibility(View.GONE);
        rvImages.setVisibility(View.GONE); // Hide the images (RecyclerView)
        pleasesel.setVisibility(View.GONE); // Hide the 'Please select' text

        // Set onClickListener for confirmButton
        confirmButton.setOnClickListener(v -> {
            // Create an Intent to navigate to userforgotpass activity
            Intent intent = new Intent(forgotpass.this, userforgotpass.class);
            intent.putExtra("userId", userId); // Pass the userId here
            startActivity(intent); // Start the activity
        });
    }

    private void fetchDecoyImages(String realImage, DecoyImagesCallback decoyImagesCallback) {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> allImages = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    DataSnapshot historySnapshot = userSnapshot.child("history");
                    for (DataSnapshot history : historySnapshot.getChildren()) {
                        String imageUrl = history.child("imageUrl").getValue(String.class);
                        if (imageUrl != null) {
                            allImages.add(imageUrl);
                        }
                    }
                }

                if (allImages.size() >= 9) {
                    allImages.remove(realImage); // Remove the real image to avoid duplication
                    Collections.shuffle(allImages);

                    List<String> finalImages = new ArrayList<>(allImages.subList(0, Math.min(allImages.size(), 8))); // Safely get the first 8 images, or fewer if there are less than 8
                    finalImages.add(realImage); // Add the real image to the list
                    Collections.shuffle(finalImages); // Shuffle the list

                    // Pass both finalImages and realImage to the updateImages method
                    imageAdapter.updateImages(finalImages, realImage);

                } else {
                    Toast.makeText(forgotpass.this, "Not enough decoy images available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(forgotpass.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
