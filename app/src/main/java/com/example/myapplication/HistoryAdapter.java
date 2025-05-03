package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HistoryAdapter extends ArrayAdapter<ClassificationHistory> {
    private Context context;
    private boolean isRecycleBin = false;
    private ArrayList<ClassificationHistory> historyList;
    private String userID;

    public HistoryAdapter(Context context, ArrayList<ClassificationHistory> historyList, String userID) {
        super(context, R.layout.history_item, historyList);
        this.context = context;
        this.historyList = historyList != null ? historyList : new ArrayList<>();
        this.userID = userID;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.history_item, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.text_view_history_item);
        ImageView imageView = convertView.findViewById(R.id.image_view_history_item);
        TextView statusTextView = convertView.findViewById(R.id.status_text_view);

        ClassificationHistory history = historyList.get(position);
        textView.setText(history.toString());

        Glide.with(context)
                .load(history.getImageUrl())
                .into(imageView);

        String status = history.getConfidence() >= 0.90f ? "Done" : "Failed";
        statusTextView.setText(status);

        // Update color based on status
        if (status.equals("Failed")) {
            statusTextView.setTextColor(Color.parseColor("#D32F2F"));
        } else {
            statusTextView.setTextColor(Color.parseColor("#388E3C"));
        }

        // Check if the item is in the recycle bin
        DatabaseReference recycleBinRef = FirebaseDatabase.getInstance().getReference("historyrecyclebin").child(userID).child("history").child(history.getId());
        View finalConvertView = convertView;
        recycleBinRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isInRecycleBin = snapshot.exists();  // Check if the history item exists in the recycle bin

                // Handle long click to recover or move to recycle bin
                finalConvertView.setOnLongClickListener(v -> {
                    ProgressBar progressBar = finalConvertView.findViewById(R.id.progress_bar);
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(0);

                    Handler handler = new Handler();
                    final int[] progress = {0};
                    final boolean[] isPressed = {true};

                    Runnable progressRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (isPressed[0]) {
                                progress[0] += 1;
                                progressBar.setProgress(progress[0]);

                                if (progress[0] < 100) {
                                    handler.postDelayed(this, 10);
                                } else {
                                    progressBar.setVisibility(View.GONE);

                                    if (isInRecycleBin) {
                                        // Recycle bin recovery logic
                                        AlertDialog dialog = new AlertDialog.Builder(context)
                                                .setTitle("Confirm Recovery")
                                                .setMessage("Do you want to restore this item from the recycle bin?")
                                                .setPositiveButton("Yes", (dialog1, which) -> {
                                                    // Call the recoverHistory method for recovery
                                                    recoverHistory(history.getId(), position);
                                                })
                                                .setNegativeButton("No", (dialog12, which) -> dialog12.dismiss())
                                                .create();

                                        dialog.show();
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                                    } else {
                                        // Regular history dialog (move to recycle bin)
                                        AlertDialog dialog = new AlertDialog.Builder(context)
                                                .setTitle("Confirm Move")
                                                .setMessage("Do you want to move this item to the recycle bin?")
                                                .setPositiveButton("Yes", (dialog1, which) -> {
                                                    moveItemToRecycleBin(history.getId());
                                                    historyList.remove(position);
                                                    notifyDataSetChanged();
                                                })
                                                .setNegativeButton("No", (dialog12, which) -> dialog12.dismiss())
                                                .create();

                                        dialog.show();
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                                    }
                                }
                            } else {
                                progressBar.setVisibility(View.GONE);
                            }
                        }
                    };

                    handler.post(progressRunnable);

                    v.setOnTouchListener((view, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                            isPressed[0] = false;
                            progress[0] = 0;
                            handler.removeCallbacks(progressRunnable);
                            progressBar.setVisibility(View.GONE);
                        }
                        return false;
                    });

                    return true;
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HistoryAdapter", "Failed to check recycle bin status", error.toException());
            }
        });

        return convertView;
    }


    // Method to recover history from recycle bin
    private void recoverHistory(String historyID, int position) {
        DatabaseReference recycleBinRef = FirebaseDatabase.getInstance().getReference("historyrecyclebin").child(userID).child("history").child(historyID);
        DatabaseReference userHistoryRef = FirebaseDatabase.getInstance().getReference("users").child(userID).child("history").child(historyID);

        // Get the history item from the recycle bin
        recycleBinRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Retrieve the data from the recycle bin
                    Map<String, Object> historyData = (Map<String, Object>) snapshot.getValue();

                    if (historyData != null) {
                        // Set fields to null or empty as needed
                        historyData.put("startDate", null);
                        historyData.put("expiryDate", null);
                        // Add any other fields you need to set to null/empty

                        // Restore the history item to the user's history
                        userHistoryRef.setValue(historyData)
                                .addOnSuccessListener(aVoid -> {
                                    // Successfully restored to user's history
                                    Toast.makeText(context, "Item recovered successfully", Toast.LENGTH_SHORT).show();
                                    // Remove from the recycle bin
                                    recycleBinRef.removeValue()
                                            .addOnSuccessListener(aVoid1 -> {
                                                // Successfully removed from the recycle bin
                                                historyList.add(position, snapshot.getValue(ClassificationHistory.class)); // Add it back to the history list
                                                notifyDataSetChanged();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    // Handle failure
                                    Toast.makeText(context, "Failed to recover item", Toast.LENGTH_SHORT).show();
                                });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HistoryAdapter", "Failed to retrieve item from recycle bin", error.toException());
            }
        });
    }


    // Method to move the item to the "historyrecyclebin" node under the user ID
    private void moveItemToRecycleBin(String historyID) {
        DatabaseReference userHistoryRef = FirebaseDatabase.getInstance().getReference("users").child(userID).child("history").child(historyID);
        DatabaseReference recycleBinRef = FirebaseDatabase.getInstance().getReference("historyrecyclebin").child(userID).child("history").child(historyID);

        // Move the item to the recycle bin
        userHistoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get the current time to set as start date
                    long currentTime = System.currentTimeMillis();

                    // Calculate the expiry time (current time + 10 seconds)
                    long expiryTime = currentTime + 10000;  // 10 seconds = 10000 milliseconds

                    // Prepare the item to include timestamps
                    Map<String, Object> historyWithTimestamp = new HashMap<>();
                    historyWithTimestamp.put("startDate", currentTime);  // Set start date (current time)
                    historyWithTimestamp.put("expiryDate", expiryTime);  // Set expiry date (current time + 10 seconds)
                    historyWithTimestamp.putAll((Map<? extends String, ?>) snapshot.getValue());  // Add original history item data

                    // Copy the history item to the recycle bin with timestamps
                    recycleBinRef.setValue(historyWithTimestamp)
                            .addOnSuccessListener(aVoid -> {
                                // Successfully moved to recycle bin
                                Toast.makeText(context, "Item moved to recycle bin", Toast.LENGTH_SHORT).show();

                                // Remove the item from the original history node
                                userHistoryRef.removeValue()
                                        .addOnSuccessListener(aVoid1 -> Log.d("HistoryAdapter", "Item successfully removed from original history"))
                                        .addOnFailureListener(e -> Log.e("HistoryAdapter", "Failed to remove item from original history", e));
                            })
                            .addOnFailureListener(e -> {
                                // Handle failure
                                Toast.makeText(context, "Failed to move item to recycle bin", Toast.LENGTH_SHORT).show();
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HistoryAdapter", "Failed to retrieve item from history", error.toException());
            }
        });
    }
}
