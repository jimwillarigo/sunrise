package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RecycleViewAdapter extends RecyclerView.Adapter<RecycleViewAdapter.UserViewHolder> {
    private Context context;
    private List<User> userList;
    private List<User> originalUserList; // Store the original list
    private boolean showingPendingUsers; // Flag to determine which list to display

    public RecycleViewAdapter(Context context, List<User> userList, boolean showingPendingUsers) {
        this.context = context;
        this.userList = userList;
        this.originalUserList = new ArrayList<>(userList); // Copy the original list
        this.showingPendingUsers = showingPendingUsers; // Initialize the flag
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, @SuppressLint("RecyclerView") int position) {
        User user = userList.get(position);
        holder.nameTextView.setText(user.getName());

        // Show or hide buttons based on whether we're showing pending users or not
        if (showingPendingUsers) {
            holder.modifyButton.setVisibility(View.GONE); // Hide Modify button
            holder.approveButton.setVisibility(View.VISIBLE); // Show Approve button
        } else {
            holder.modifyButton.setVisibility(View.VISIBLE); // Show Modify button
            holder.approveButton.setVisibility(View.GONE); // Hide Approve button
        }

        // Set up the delete button click with confirmation dialog
        holder.deleteButton.setOnClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setMessage("Are you sure you want to remove this user?")
                    .setCancelable(false) // Make the dialog not dismissible by clicking outside
                    .setPositiveButton("Yes", (dialogInterface, id) -> {
                        int currentPosition = holder.getAdapterPosition(); // Safely get current adapter position
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            User userToDelete = userList.get(currentPosition);

                            // Delete from the 'users' node
                            DatabaseReference usersReference = FirebaseDatabase.getInstance()
                                    .getReference("users")
                                    .child(userToDelete.getId());

                            // Delete from the 'pendingusers' node
                            DatabaseReference pendingUsersReference = FirebaseDatabase.getInstance()
                                    .getReference("pendingusers")
                                    .child(userToDelete.getId());

                            // Perform deletion for both 'users' and 'pendingusers'
                            usersReference.removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        // Handle success for 'users' deletion
                                        pendingUsersReference.removeValue()
                                                .addOnSuccessListener(aVoid2 -> {
                                                    // Double-check position validity before modifying userList
                                                    if (currentPosition < userList.size()) {
                                                        userList.remove(currentPosition);
                                                        notifyItemRemoved(currentPosition);
                                                        notifyItemRangeChanged(currentPosition, userList.size());
                                                    }
                                                    // Refresh the RecyclerView after both deletions
                                                    notifyDataSetChanged(); // Refresh the entire RecyclerView
                                                    Toast.makeText(context, "User removed", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    // Handle failure for 'pendingusers' deletion
                                                    Log.e("FirebaseError", "Failed to delete from pendingusers: " + e.getMessage());
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        // Handle failure for 'users' deletion
                                        Log.e("FirebaseError", "Failed to delete from users: " + e.getMessage());
                                    });
                        }
                    })
                    .setNegativeButton("No", (dialogInterface, id) -> dialogInterface.dismiss())
                    .create();

            dialog.show();

            // Set the text color for both buttons
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
        });

        // Set up the modify button click
        holder.modifyButton.setOnClickListener(v -> {
            // Pass the user data to the AdminModify activity
            Intent intent = new Intent(context, adminmodify.class);
            intent.putExtra("userId", user.getId()); // Pass the user ID
            intent.putExtra("userName", user.getName()); // Pass the user name if needed

            // Start the AdminModify activity
            context.startActivity(intent);
        });


        // Set up the approve button click
        holder.approveButton.setOnClickListener(v -> {
            // Show confirmation dialog
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("Approve User")
                    .setMessage("Are you sure you want to approve this user?")
                    .setPositiveButton("Yes", (dialog1, which) -> {
                        // Implement approve logic for pending users
                        DatabaseReference pendingUsersRef = FirebaseDatabase.getInstance().getReference("pendingusers").child(user.getId());
                        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(user.getId());

                        // Fetch the user data from pendingusers
                        pendingUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    // Transfer the user data to "users"
                                    usersRef.setValue(dataSnapshot.getValue())
                                            .addOnSuccessListener(aVoid -> {
                                                // Remove the user from "pendingusers"
                                                pendingUsersRef.removeValue()
                                                        .addOnSuccessListener(aVoid1 -> {
                                                            Toast.makeText(context, "User Approved", Toast.LENGTH_SHORT).show();

                                                            // Update the list after approval
                                                            if (userList != null && !userList.isEmpty() && position >= 0 && position < userList.size()) {
                                                                userList.remove(position);
                                                                notifyItemRemoved(position);
                                                                notifyItemRangeChanged(position, userList.size());
                                                            } else {
                                                                Log.e("ApprovalError", "Attempted to remove item from an empty or invalid list");
                                                            }
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e("FirebaseError", "Failed to remove user from pendingusers: " + e.getMessage());
                                                        });
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("FirebaseError", "Failed to save user to users: " + e.getMessage());
                                            });
                                } else {
                                    Log.e("FirebaseError", "No data found for user in pendingusers.");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.e("FirebaseError", "Failed to fetch user from pendingusers: " + databaseError.getMessage());
                            }
                        });
                    })
                    .setNegativeButton("No", null) // Dismiss dialog on "No"
                    .show();

            // Set text color for the positive and negative buttons
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
        });




    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // Method to filter the list
    public void filterList(List<User> filteredList) {
        userList = filteredList;
        notifyDataSetChanged();
    }

    // Method to update the showingPendingUsers flag and notify changes
    public void setShowingPendingUsers(boolean showingPendingUsers) {
        this.showingPendingUsers = showingPendingUsers;
        notifyDataSetChanged();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        Button deleteButton;
        Button modifyButton;
        Button approveButton; // Added the approve button

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.text_name);
            deleteButton = itemView.findViewById(R.id.button_delete);
            modifyButton = itemView.findViewById(R.id.button_modify);
            approveButton = itemView.findViewById(R.id.button_approve); // Initialize approve button
        }
    }
}
