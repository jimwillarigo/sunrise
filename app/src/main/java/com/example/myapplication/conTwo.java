package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class conTwo extends Fragment {
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 10;
    private List<ClassificationHistory> paginatedHistoryList = new ArrayList<>();

    private ListView listView;
    private DatabaseReference databaseReference;
    private ArrayList<ClassificationHistory> historyList;
    private ArrayList<ClassificationHistory> originalHistoryList;  // Ensure this is initialized
    private HistoryAdapter historyAdapter;
    private TextView myTextView, hisindicator;
    private Button clearHistoryButton, recoverButton; // Clear History button
    private Button allButton; // All button
    private FirebaseStorage firebaseStorage;
    private ArrayList<String> allAvailableDates = new ArrayList<>();
    LinearLayout linearLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contwo, container, false);

        // Initialize the ListView, TextView, and Buttons
        linearLayout = view.findViewById(R.id.listviewbg);
        listView = view.findViewById(R.id.history_list_view);
        myTextView = view.findViewById(R.id.my_text_view_id);
        clearHistoryButton = view.findViewById(R.id.delhisbutton);
        recoverButton = view.findViewById(R.id.recoverhis);
        allButton = view.findViewById(R.id.all_button); // Initialize All button
        historyList = new ArrayList<>();
        hisindicator = view.findViewById(R.id.hisindicator);
        originalHistoryList = new ArrayList<>(); // Initialize the list here

        firebaseStorage = FirebaseStorage.getInstance(); // Initialize Firebase Storage


        // Get user ID from SharedPreferences
        String userId = getActivity().getSharedPreferences("UserSession", getContext().MODE_PRIVATE).getString("userId", "");
        monitorExpiredItems(userId);

        if (userId.isEmpty()) {
            Log.e("conTwo", "User ID is null or empty");
            return view;
        }

        // Set up the database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("history");

        // Fetch history data from Firebase
        fetchHistoryData(userId);

        // Set up the Clear History button's OnClickListener
        clearHistoryButton.setOnClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setTitle("Clear History")
                    .setMessage("Are you sure you want to clear all history?")
                    .setPositiveButton("Yes", (dialog1, id) -> clearHistory(userId))
                    .setNegativeButton("No", null)
                    .create(); // Create the dialog object

            dialog.show(); // Show the dialog

            // Set the text color for the buttons after the dialog is shown
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
        });

        // Set up the All button's OnClickListener
        allButton.setOnClickListener(v -> displayAvailableDates(allButton));
        recoverButton.setOnClickListener(v -> recoverHistory(userId));

        return view;
    }

    private void fetchHistoryData(String userId) {
        // Assume databaseReference is initialized properly
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                historyList.clear();
                originalHistoryList.clear(); // Ensure originalHistoryList is cleared before using it

                boolean hasData = false; // Flag to check if there's valid data

                // Populate originalHistoryList with all the data from Firebase
                for (DataSnapshot historySnapshot : dataSnapshot.getChildren()) {
                    ClassificationHistory history = historySnapshot.getValue(ClassificationHistory.class);
                    if (history != null) {
                        historyList.add(history);
                        originalHistoryList.add(history);
                        hasData = true; // Set flag to true if there's at least one valid history
                    }
                }

                // Check if there is no data and disable/low opacity the button
                if (!hasData) {
                    clearHistoryButton.setAlpha(0.5f);
                    clearHistoryButton.setEnabled(false);
                } else {
                    clearHistoryButton.setAlpha(1.0f); // Set back to normal opacity
                    clearHistoryButton.setEnabled(true); // Enable the button if there's data
                }

                // Sort the history list by timestamp in descending order, handle null timestamps
                Collections.sort(historyList, (o1, o2) -> {
                    if (o1.getTimestamp() == null && o2.getTimestamp() == null) {
                        return 0; // Both timestamps are null, so they're considered equal
                    } else if (o1.getTimestamp() == null) {
                        return 1; // Null timestamps are considered "less" than non-null ones
                    } else if (o2.getTimestamp() == null) {
                        return -1; // Null timestamps are considered "less" than non-null ones
                    }
                    // Compare non-null timestamps
                    return o2.getTimestamp().compareTo(o1.getTimestamp());
                });


                // Load the data for the current page
                loadPage(currentPage);

                // Set up the adapter to display the paginated data
                if (getActivity() != null) {
                    historyAdapter = new HistoryAdapter((Context) getActivity(), (ArrayList<ClassificationHistory>) paginatedHistoryList, userId);
                    listView.setAdapter(historyAdapter);
                }

                // Set up pagination buttons
                setupPaginationButtons();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), "Failed to load history: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void loadPage(int page) {
        // Clear the paginated list before adding new data
        paginatedHistoryList.clear();

        // Calculate the start index and the end index for the current page
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, historyList.size());

        // Add the items for the current page to the paginated list
        for (int i = start; i < end; i++) {
            paginatedHistoryList.add(historyList.get(i));
        }
    }

    private void setupPaginationButtons() {
        View view = getView();
        if (view == null) return;  // Prevent NullPointerException if the view is not yet created
        Button btnNext = getView().findViewById(R.id.btnNext);
        Button btnPrevious = getView().findViewById(R.id.btnPrevious);
        TextView pageCounter = getView().findViewById(R.id.pagecounter);  // Reference to the TextView

        // Set the "Previous" button state and alpha based on the current page
        boolean isPreviousEnabled = currentPage > 0;
        btnPrevious.setEnabled(isPreviousEnabled);
        btnPrevious.setAlpha(isPreviousEnabled ? 1.0f : 0.5f);  // Full opacity if enabled, 50% opacity if disabled

        // Disable the "Next" button if there are no more pages
        boolean isNextEnabled = (currentPage + 1) * ITEMS_PER_PAGE < historyList.size();
        btnNext.setEnabled(isNextEnabled);
        btnNext.setAlpha(isNextEnabled ? 1.0f : 0.5f);  // Full opacity if enabled, 50% opacity if disabled

        // Update the page counter TextView to show the current page
        pageCounter.setText("" + (currentPage + 1));  // Display the page number (starting from 1)

        // Set up the "Next" button click listener
        btnNext.setOnClickListener(v -> {
            currentPage++;
            loadPage(currentPage);
            historyAdapter.notifyDataSetChanged();
            setupPaginationButtons(); // Update button states
        });

        // Set up the "Previous" button click listener
        btnPrevious.setOnClickListener(v -> {
            currentPage--;
            loadPage(currentPage);
            historyAdapter.notifyDataSetChanged();
            setupPaginationButtons(); // Update button states
        });

        // Add OnClickListener to the pageCounter TextView
        pageCounter.setOnClickListener(v -> {
            // Calculate the total number of pages
            int totalPages = (int) Math.ceil((double) historyList.size() / ITEMS_PER_PAGE);
            String[] pageNumbers = new String[totalPages];
            for (int i = 0; i < totalPages; i++) {
                pageNumbers[i] = "Page " + (i + 1);  // Create an array of page names (e.g., "Page 1", "Page 2", ...)
            }

            // Show an AlertDialog with the list of pages
            new AlertDialog.Builder(requireContext())
                    .setTitle("Go to Page")
                    .setItems(pageNumbers, (dialog, which) -> {
                        // Redirect to the selected page
                        currentPage = which; // 'which' corresponds to the selected index
                        loadPage(currentPage);
                        historyAdapter.notifyDataSetChanged();
                        setupPaginationButtons(); // Update button states
                    })
                    .show();
        });
    }


    private void clearHistory(String userId) {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference userHistoryRef = firebaseDatabase.getReference("users").child(userId).child("history");
        DatabaseReference recycleBinRef = firebaseDatabase.getReference("historyrecyclebin").child(userId).child("history");

        // Retrieve the user's history
        userHistoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get the current time to set as start date
                    long currentTime = System.currentTimeMillis();

                    // Iterate over each history item
                    for (DataSnapshot historySnapshot : snapshot.getChildren()) {
                        String historyId = historySnapshot.getKey();
                        Object historyItem = historySnapshot.getValue(); // Get the original history item data

                        // Calculate the expiry time (current time + 10 seconds)
                        long expiryTime = currentTime + 10000;  // 10 seconds = 10000 milliseconds

                        // Save the history item with the timestamp directly in the recycle bin
                        Map<String, Object> historyWithTimestamp = new HashMap<>();
                        historyWithTimestamp.put("startDate", currentTime);  // Set start date (current time)
                        historyWithTimestamp.put("expiryDate", expiryTime);  // Set expiry date (current time + 10 secs)
                        historyWithTimestamp.putAll((Map<? extends String, ?>) historyItem);  // Add original history item data

                        // Save the history item with the timestamp to the recycle bin
                        recycleBinRef.child(historyId).setValue(historyWithTimestamp)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("conTwo", "History item moved to recycle bin with timestamp");

                                    // Delete the original history node
                                    userHistoryRef.removeValue()
                                            .addOnSuccessListener(aVoid2 -> Log.d("conTwo", "Original history deleted"))
                                            .addOnFailureListener(e -> Log.e("conTwo", "Failed to delete original history", e));
                                })
                                .addOnFailureListener(e -> Log.e("conTwo", "Failed to move history item to recycle bin", e));
                    }

                    Toast.makeText(requireContext(), "Item(s) moved to recycle bin", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d("conTwo", "No history found for user");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("conTwo", "Failed to retrieve user history", error.toException());
            }
        });
    }


    // Monitor the recycle bin for expired items and remove them when expired
    private void monitorExpiredItems(String userId) {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference recycleBinRef = firebaseDatabase.getReference("historyrecyclebin").child(userId).child("history");

        recycleBinRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Retrieve the expiry date and check if it has passed
                Long expiryDate = (Long) snapshot.child("expiryDate").getValue();
                if (expiryDate != null && expiryDate <= System.currentTimeMillis()) {
                    // Retrieve the image URL before deleting the item
                    String imageUrl = snapshot.child("imageUrl").getValue(String.class);

                    // Remove the history item from the recycle bin
                    recycleBinRef.child(snapshot.getKey()).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Firebase", "History item expired and removed");

                                // If imageUrl exists, delete the corresponding image from Firebase Storage
                                if (imageUrl != null) {
                                    FirebaseStorage storage = FirebaseStorage.getInstance();
                                    StorageReference imageRef = storage.getReferenceFromUrl(imageUrl);
                                    imageRef.delete()
                                            .addOnSuccessListener(aVoid2 -> Log.d("Firebase", "Image deleted successfully"))
                                            .addOnFailureListener(e -> Log.e("Firebase", "Failed to delete image", e));
                                }
                            })
                            .addOnFailureListener(e -> Log.e("Firebase", "Failed to remove expired history item", e));
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Handle changes if needed
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // Handle removal if needed
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Handle movement if needed
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Listener cancelled", error.toException());
            }
        });
    }





    private void recoverHistory(String userId) {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference recycleBinRef = firebaseDatabase.getReference("historyrecyclebin").child(userId).child("history");
        DatabaseReference userHistoryRef = firebaseDatabase.getReference("users").child(userId).child("history");

        // Retrieve the history from the recycle bin to get the item count
        recycleBinRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    long itemCount = snapshot.getChildrenCount(); // Count the number of items in the recycle bin

                    // Create and customize the AlertDialog
                    AlertDialog dialog = new AlertDialog.Builder(requireContext())
                            .setTitle("Recover History")
                            .setMessage("Are you sure you want to recover all " + itemCount + " item(s) from your recycle bin?")
                            .setPositiveButton("Yes", (dialogInterface, which) -> {
                                refreshFragment();
                                // Retrieve existing history from the user's history node (if any)
                                userHistoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                        if (userSnapshot.exists()) {
                                            // Merge existing data with the recovered data from the recycle bin
                                            for (DataSnapshot historyItem : snapshot.getChildren()) {
                                                Map<String, Object> historyData = (Map<String, Object>) historyItem.getValue();
                                                if (historyData != null) {
                                                    // Set "dateTime" and "expiryDate" fields to null or empty
                                                    historyData.put("starDate", null);
                                                    historyData.put("expiryDate", null);

                                                    // Add each history item to the user's history node
                                                    userHistoryRef.child(historyItem.getKey()).setValue(historyData)
                                                            .addOnSuccessListener(aVoid -> Log.d("conTwo", "Item merged successfully"))
                                                            .addOnFailureListener(e -> Log.e("conTwo", "Failed to merge item", e));
                                                }
                                            }
                                            Toast.makeText(requireContext(), "Item(s) recovered successfully!", Toast.LENGTH_SHORT).show();
                                        } else {
                                            // Copy the entire data from the recycle bin
                                            Map<String, Object> updatedData = new HashMap<>();
                                            for (DataSnapshot historyItem : snapshot.getChildren()) {
                                                Map<String, Object> historyData = (Map<String, Object>) historyItem.getValue();
                                                if (historyData != null) {
                                                    // Set "dateTime" and "expiryDate" fields to null or empty
                                                    historyData.put("startDate", null);
                                                    historyData.put("expiryDate", null);
                                                    updatedData.put(historyItem.getKey(), historyData);
                                                }
                                            }
                                            userHistoryRef.setValue(updatedData)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(requireContext(), "Item(s) recovered successfully!", Toast.LENGTH_SHORT).show();
                                                        Log.d("conTwo", "History recovered successfully");
                                                    })
                                                    .addOnFailureListener(e -> Log.e("conTwo", "Failed to recover history", e));
                                        }

                                        // After merging or setting, clear the recycle bin
                                        recycleBinRef.removeValue()
                                                .addOnSuccessListener(aVoid2 -> Log.d("conTwo", "Recycle bin cleared"))
                                                .addOnFailureListener(e -> Log.e("conTwo", "Failed to clear recycle bin", e));
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("conTwo", "Failed to retrieve user history", error.toException());
                                    }
                                });
                            })
                            .setNegativeButton("Pick manually", (dialogInterface, which) -> {
                                clearHistoryButton.setEnabled(false);
                                clearHistoryButton.setAlpha(0.5f);
                                hisindicator.setText("Recycle bin");
                                // Fetch the history data from the recycle bin
                                recycleBinRef.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        historyList.clear();
                                        originalHistoryList.clear(); // Ensure originalHistoryList is cleared before using it

                                        // Populate originalHistoryList with all the data from Firebase
                                        for (DataSnapshot historySnapshot : dataSnapshot.getChildren()) {
                                            ClassificationHistory history = historySnapshot.getValue(ClassificationHistory.class);
                                            if (history != null) {
                                                historyList.add(history);
                                                originalHistoryList.add(history);
                                            }
                                        }

                                        // Sort the history list by timestamp in descending order
                                        // Sort the history list by timestamp in descending order, handle null timestamps
                                        Collections.sort(historyList, (o1, o2) -> {
                                            if (o1.getTimestamp() == null && o2.getTimestamp() == null) {
                                                return 0; // Both timestamps are null, so they're considered equal
                                            } else if (o1.getTimestamp() == null) {
                                                return 1; // Null timestamps are considered "less" than non-null ones
                                            } else if (o2.getTimestamp() == null) {
                                                return -1; // Null timestamps are considered "less" than non-null ones
                                            }
                                            // Compare non-null timestamps
                                            return o2.getTimestamp().compareTo(o1.getTimestamp());
                                        });


                                        // Load the data for the current page
                                        loadPage(currentPage);

                                        // Set up the adapter to display the paginated data
                                        if (getActivity() != null) {
                                            historyAdapter = new HistoryAdapter((Context) getActivity(), (ArrayList<ClassificationHistory>) paginatedHistoryList, userId);
                                            listView.setAdapter(historyAdapter);
                                        }

                                        // Set up pagination buttons
                                        setupPaginationButtons();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        Toast.makeText(getActivity(), "Failed to load history: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .setNeutralButton("History", (dialogInterface, which) -> {
                                // Refresh the fragment when "History" is selected
                                refreshFragment();
                            })
                            .create();

                    // Show the dialog and then customize button colors
                    dialog.setOnShowListener(dialogInterface -> {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.WHITE); // For "Back" button
                    });

                    dialog.show();
                } else {
                    Log.d("conTwo", "No history found in the recycle bin for user");
                    Toast.makeText(requireContext(), "No history found to recover!", Toast.LENGTH_SHORT).show();
                    refreshFragment();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("conTwo", "Failed to retrieve recycle bin history", error.toException());
            }
        });
    }












    private void setupAvailableYears() {
        // Clear the previous list and generate new available years from the original list
        Set<String> availableYearsSet = new HashSet<>();

        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());

        for (ClassificationHistory history : originalHistoryList) {
            try {
                String timestampString = history.getTimestamp();
                Date parsedDate = inputDateFormat.parse(timestampString);
                if (parsedDate != null) {
                    String year = yearFormat.format(parsedDate);
                    availableYearsSet.add(year);
                }
            } catch (Exception e) {
                Log.e("setupAvailableYears", "Error parsing timestamp: " + history.getTimestamp(), e);
            }
        }
        allAvailableDates.clear();
        allAvailableDates.addAll(availableYearsSet); // Add unique years
    }

    private void displayAvailableDates(final Button allButton) {
        // Setup the available years before displaying them in the dialog
        setupAvailableYears();

        // Add "All" option at the beginning and sort years
        List<String> sortedYears = new ArrayList<>(allAvailableDates);
        sortedYears.add(0, "All");

        // Show the available years in a dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setTitle("Available Years");

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, sortedYears);
        dialogBuilder.setAdapter(yearAdapter, (dialog, which) -> {
            String selectedYear = sortedYears.get(which);

            if ("All".equals(selectedYear)) {
                refreshFragment();
                allButton.setText("All");
            } else {
                // Display months for the selected year
                displayAvailableMonths(allButton, selectedYear);
            }
        });

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    private void displayAvailableMonths(final Button allButton, final String selectedYear) {
        // Setup months for the selected year
        Set<String> availableMonthsSet = new HashSet<>();
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy MMM", Locale.getDefault()); // Format year and month

        for (ClassificationHistory history : originalHistoryList) {
            try {
                String timestampString = history.getTimestamp();
                Date parsedDate = inputDateFormat.parse(timestampString);
                if (parsedDate != null && selectedYear.equals(new SimpleDateFormat("yyyy", Locale.getDefault()).format(parsedDate))) {
                    String monthYear = monthFormat.format(parsedDate); // Format the combined year and month
                    availableMonthsSet.add(monthYear);
                }
            } catch (Exception e) {
                Log.e("displayAvailableMonths", "Error parsing timestamp: " + history.getTimestamp(), e);
            }
        }

        // Show available months in a dialog
        List<String> sortedMonths = new ArrayList<>(availableMonthsSet);

        // Sort the months by month number (January - 1, December - 12)
        Collections.sort(sortedMonths, (month1, month2) -> {
            try {
                SimpleDateFormat monthParser = new SimpleDateFormat("yyyy MMM", Locale.getDefault());
                Date date1 = monthParser.parse(month1);
                Date date2 = monthParser.parse(month2);
                return date1.compareTo(date2); // Compare dates to ensure correct order
            } catch (Exception e) {
                Log.e("displayAvailableMonths", "Error sorting months", e);
                return 0; // In case of error, don't alter order
            }
        });

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setTitle("Available Months");

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, sortedMonths);
        dialogBuilder.setAdapter(monthAdapter, (dialog, which) -> {
            String selectedMonthYear = sortedMonths.get(which);

            // Display dates for the selected year and month
            String[] yearMonth = selectedMonthYear.split(" "); // Split year and month
            String selectedMonth = yearMonth[1];
            displayAvailableDatesForMonthAndYear(allButton, selectedYear, selectedMonth);
        });

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }




    private void displayAvailableDatesForMonthAndYear(final Button allButton, final String selectedYear, final String selectedMonth) {
        // Setup available dates for the selected year and month
        Set<String> availableDatesSet = new HashSet<>();
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MMM dd", Locale.getDefault()); // Format year, month, day

        for (ClassificationHistory history : originalHistoryList) {
            try {
                String timestampString = history.getTimestamp();
                Date parsedDate = inputDateFormat.parse(timestampString);
                if (parsedDate != null && selectedYear.equals(new SimpleDateFormat("yyyy", Locale.getDefault()).format(parsedDate)) &&
                        selectedMonth.equals(new SimpleDateFormat("MMM", Locale.getDefault()).format(parsedDate))) {

                    String formattedDate = dateFormat.format(parsedDate); // Format date as "yyyy MMM dd"
                    availableDatesSet.add(formattedDate);
                }
            } catch (Exception e) {
                Log.e("displayAvailableDatesForMonthAndYear", "Error parsing timestamp: " + history.getTimestamp(), e);
            }
        }

        // Sort available dates
        List<String> sortedDates = new ArrayList<>(availableDatesSet);
        Collections.sort(sortedDates);

        // Show available dates in a dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setTitle("Available Dates");

        ArrayAdapter<String> dateAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, sortedDates);
        dialogBuilder.setAdapter(dateAdapter, (dialog, which) -> {
            String selectedDate = sortedDates.get(which);

            // Handle the "All" button
            if ("All".equals(selectedDate)) {
                refreshFragment();
                allButton.setText("All");
            } else {
                // Split selected date into year, month, and day
                String[] dateParts = selectedDate.split(" ");
                String selectedYearFromDate = dateParts[0]; // "yyyy"
                String selectedMonthFromDate = dateParts[1]; // "MMM"
                String selectedDayFromDate = dateParts[2]; // "dd"

                // Adjust filter logic to compare date correctly
                filterHistoryListByDate(selectedYearFromDate, selectedMonthFromDate, selectedDayFromDate);
                allButton.setText(selectedDate); // Display selected date on the button
            }
        });

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }



    private void filterHistoryListByDate(String selectedYear, String selectedMonth, String selectedDate) {
        // Filter the historyList by the selected year, month, and day
        ArrayList<ClassificationHistory> filteredList = new ArrayList<>();
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MMM dd", Locale.getDefault());  // Adjusted format for exact matching

        // Create a combined selected date string
        String selectedFormattedDate = selectedYear + " " + selectedMonth + " " + selectedDate;

        for (ClassificationHistory history : originalHistoryList) {
            try {
                String timestampString = history.getTimestamp();
                Date parsedDate = inputDateFormat.parse(timestampString);
                if (parsedDate != null) {
                    // Format the timestamp to compare with the selected date
                    String formattedDate = dateFormat.format(parsedDate);

                    // Now check for exact match
                    if (formattedDate.equals(selectedFormattedDate)) {
                        filteredList.add(history);
                    }
                }
            } catch (Exception e) {
                Log.e("filterHistoryListByDate", "Error parsing timestamp: " + e.getMessage(), e);
            }
        }

        // Update the history list with the filtered results
        historyList.clear();
        historyList.addAll(filteredList);

        // Check if the historyAdapter is properly initialized
        if (historyAdapter != null) {
            historyAdapter.notifyDataSetChanged(); // Refresh the ListView with the updated data
        } else {
            Log.e("filterHistoryListByDate", "HistoryAdapter is null, unable to update ListView");
        }

        // Force a ListView refresh
        if (listView != null) {
            listView.invalidate(); // Explicitly refresh the ListView view
        }
        // Reset to the first page when the list is filtered
        currentPage = 0;

        // Reload the current page to ensure the pagination reflects the filtered data
        loadPage(currentPage);
        historyAdapter.notifyDataSetChanged();
        setupPaginationButtons(); // Ensure the pagination buttons are correctly updated
    }



    private void refreshFragment() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new conTwo()); // Replace with your fragment
        transaction.addToBackStack(null);
        transaction.commit();
    }



}
