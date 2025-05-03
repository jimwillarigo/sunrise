package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<String> images;
    private Context context;
    private String realImage; // Store the real image URL for comparison
    private String userId;

    public ImageAdapter(Context context, List<String> images, String realImage, String userId) {
        this.context = context;
        this.images = images;
        this.realImage = realImage;
        this.userId = userId;  // Now you're correctly assigning the passed userId
    }


    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = images.get(position);
        Glide.with(context).load(imageUrl).into(holder.imageView);

        // Here, pass the image URL and user identifier (you could pass email or userID from your database)
        String userIdentifier = "someUserIdentifier";  // e.g., get from your database or image-related data

        holder.itemView.setOnClickListener(v -> {
            showConfirmationDialog(imageUrl, userIdentifier); // Pass clicked image URL and identifier
        });
    }


    @Override
    public int getItemCount() {
        return images.size();
    }

    private void showConfirmationDialog(String clickedImage, String userIdentifier) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setMessage("Are you sure this image is yours?")
                .setPositiveButton("Yes", (dialogInterface, which) -> {
                    if (clickedImage.equals(realImage)) {
                        // Show a certain button when the real image is selected
                        ((forgotpass) context).showConfirmationButton();
                        Toast.makeText(context, "Successfully verified", Toast.LENGTH_SHORT).show();

                        // Update the database with userIdentifier and set attemptCount to 0 (correct image)
                        updateVerificationStatus(userIdentifier, false); // Indicating success (no mismatch)
                    } else {
                        // Redirect to LoginActivity if the image mismatched
                        Intent intent = new Intent(context, Login.class);
                        Toast.makeText(context, "Incorrect Image", Toast.LENGTH_SHORT).show();

                        // Update the database with userIdentifier and increment attemptCount (image mismatch)
                        updateVerificationStatus(userIdentifier, true); // Indicating mismatch

                        context.startActivity(intent);
                    }
                })
                .setNegativeButton("No", null)
                .create(); // Create the AlertDialog instance

        dialog.show(); // Show the dialog before accessing buttons

        // Change the button text colors
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(context.getResources().getColor(android.R.color.white));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(context.getResources().getColor(android.R.color.white));
    }



    public void updateVerificationStatus(String userIdentifier, boolean isMismatch) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        // Update the path to include "users/{userIdentifier}"
        DatabaseReference userRef = databaseReference.child("users").child(userId);

        // Check if "attempt" node exists
        userRef.child("attempt").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int currentAttemptCount = 0; // Default value if "attempt" doesn't exist

                // If "attempt" exists, get the current value
                if (dataSnapshot.exists()) {
                    currentAttemptCount = dataSnapshot.getValue(Integer.class);
                }

                if (isMismatch) {
                    // Increment the attempt count if it's a mismatch
                    if (currentAttemptCount < 2) {
                        int newAttemptCount = currentAttemptCount + 1;
                        userRef.child("attempt").setValue(newAttemptCount);

                        // Show toast messages based on the attempt count
                        if (newAttemptCount == 1) {
                            Toast.makeText(context, "Two more tries left", Toast.LENGTH_SHORT).show();
                        } else if (newAttemptCount == 2) {
                            Toast.makeText(context, "One more try left", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Lock the account and move data to "pendingusers"
                        Toast.makeText(context, "Your account has been locked. Please wait for admin approval", Toast.LENGTH_SHORT).show();
                        userRef.child("attempt").setValue(0);

                        DatabaseReference pendingUsersRef = databaseReference.child("pendingusers").child(userId);

                        // Copy the entire user data to "pendingusers"
                        userRef.get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                DataSnapshot userData = task.getResult();

                                // Set all the user data to "pendingusers"
                                pendingUsersRef.setValue(userData.getValue(), (databaseError, databaseReference1) -> {
                                    if (databaseError == null) {
                                        Log.e("User Moved", "All user data moved to pendingusers.");

                                        // Remove the entire user data from "users" node
                                        userRef.removeValue((databaseError1, databaseReference12) -> {
                                            if (databaseError1 == null) {
                                                Log.e("User Removed", "User data removed from users.");
                                            } else {
                                                Log.e("FirebaseError", "Error removing user: " + databaseError1.getMessage());
                                            }
                                        });
                                    } else {
                                        Log.e("FirebaseError", "Error moving user data: " + databaseError.getMessage());
                                    }
                                });
                            } else {
                                Log.e("FirebaseError", "Error fetching user data: " + task.getException().getMessage());
                            }
                        });
                    }
                } else {
                    // Reset the attempt count to 0 if the verification is successful
                    userRef.child("attempt").setValue(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors here
                Log.e("FirebaseError", "Error: " + databaseError.getMessage());
            }
        });
    }







    public void updateImages(List<String> newImages, String realImage) {
        this.images = newImages;
        this.realImage = realImage; // Update the real image URL
        notifyDataSetChanged();
    }

    public class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView); // Assuming the ImageView ID in the layout is "imageView"
        }
    }
}


