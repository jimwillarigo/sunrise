package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class tungro extends AppCompatActivity {

    private EditText disease, description, symptoms, cause, prevent, chemtreat, orgTreat;
    private Button saveButton;
    private TextView diseaseTextView, descriptionTextView, symptomsTextView, causeTextView, preventTextView, chemtreatTextview, orgTreatTextview;
    private String diseaseLibraryID = "-ODPVA8KdVzxdXzEMAue"; // Directly set your specific ID
    private boolean isUserLoggedIn = false; // Set this according to your app's login status logic

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tungro); // Change to your activity layout

        // Initialize views
        disease = findViewById(R.id.disease);
        description = findViewById(R.id.description);
        symptoms = findViewById(R.id.symptoms);
        cause = findViewById(R.id.cause);
        prevent = findViewById(R.id.prevent);
        chemtreat = findViewById(R.id.chemtreat);
        orgTreat = findViewById(R.id.orgTreat);
        saveButton = findViewById(R.id.buttonSave);

        // Initialize TextViews for displaying data when user is logged in
        diseaseTextView = findViewById(R.id.diseaseTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        symptomsTextView = findViewById(R.id.symptomsTextView);
        causeTextView = findViewById(R.id.causeTextView);
        preventTextView = findViewById(R.id.prevenTextView);
        chemtreatTextview = findViewById(R.id.chemtreatTextview);
        orgTreatTextview = findViewById(R.id.orgTreatTextview);

        // Fetch existing data for the specific ID
        retrieveDiseaseData();

        // Check if the user is logged in and adjust UI accordingly
        checkUserLoginStatus();

        // Save data on button click
        saveButton.setOnClickListener(v -> saveData());
    }

    private void retrieveDiseaseData() {
        if (diseaseLibraryID.isEmpty()) {
            Toast.makeText(this, "Disease Library ID is empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the specific diseaseLibraryID directly in the reference
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("diseaselibrary").child(diseaseLibraryID);
        Log.d("DatabaseRef", "Retrieving data for diseaseLibraryID: " + diseaseLibraryID);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Retrieve data from the database under that specific ID
                    String dis = snapshot.child("disease").getValue(String.class);
                    String des = snapshot.child("description").getValue(String.class);
                    String sym = snapshot.child("symptoms").getValue(String.class);
                    String cau = snapshot.child("cause").getValue(String.class);
                    String pre = snapshot.child("prevent").getValue(String.class);
                    String che = snapshot.child("chemtreat").getValue(String.class);
                    String org = snapshot.child("orgTreat").getValue(String.class);


                    // Set the values to the EditText fields (or TextViews when logged in)
                    disease.setText(dis);
                    description.setText(des);
                    symptoms.setText(sym);
                    cause.setText(cau);
                    prevent.setText(pre);
                    chemtreat.setText(che);
                    orgTreat.setText(org);

                    // Set the TextViews if the user is logged in
                    if (isUserLoggedIn) {
                        diseaseTextView.setText(dis);
                        descriptionTextView.setText(des);
                        symptomsTextView.setText(sym);
                        causeTextView.setText(cau);
                        preventTextView.setText(pre);
                        chemtreatTextview.setText(che);
                        orgTreatTextview.setText(org);
                    }
                } else {
                    Toast.makeText(tungro.this, "No data found for the disease library.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(tungro.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserLoginStatus() {
        // Retrieve login status from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        isUserLoggedIn = sharedPreferences.getBoolean("isUserLoggedIn", false);

        if (isUserLoggedIn) {
            // Show TextViews and hide EditTexts
            disease.setVisibility(View.GONE);
            description.setVisibility(View.GONE);
            symptoms.setVisibility(View.GONE);
            cause.setVisibility(View.GONE);
            prevent.setVisibility(View.GONE);
            chemtreat.setVisibility(View.GONE);
            orgTreat.setVisibility(View.GONE);

            diseaseTextView.setVisibility(View.VISIBLE);
            descriptionTextView.setVisibility(View.VISIBLE);
            symptomsTextView.setVisibility(View.VISIBLE);
            causeTextView.setVisibility(View.VISIBLE);
            preventTextView.setVisibility(View.VISIBLE);
            chemtreatTextview.setVisibility(View.VISIBLE);
            orgTreatTextview.setVisibility(View.VISIBLE);

            saveButton.setVisibility(View.GONE); // Hide Save Button
        } else {
            // Show EditTexts and Save Button
            disease.setVisibility(View.VISIBLE);
            description.setVisibility(View.VISIBLE);
            symptoms.setVisibility(View.VISIBLE);
            cause.setVisibility(View.VISIBLE);
            prevent.setVisibility(View.VISIBLE);
            chemtreat.setVisibility(View.VISIBLE);
            orgTreat.setVisibility(View.VISIBLE);

            diseaseTextView.setVisibility(View.GONE);
            descriptionTextView.setVisibility(View.GONE);
            symptomsTextView.setVisibility(View.GONE);
            causeTextView.setVisibility(View.GONE);
            preventTextView.setVisibility(View.GONE);
            chemtreatTextview.setVisibility(View.GONE);
            orgTreatTextview.setVisibility(View.GONE);

            saveButton.setVisibility(View.VISIBLE);
        }
    }

    private void saveData() {
        String dis = disease.getText().toString().trim();
        String des = description.getText().toString().trim();
        String sym = symptoms.getText().toString().trim();
        String cau = cause.getText().toString().trim();
        String pre = prevent.getText().toString().trim();
        String che = chemtreat.getText().toString().trim();
        String org = orgTreat.getText().toString().trim();


        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("diseaselibrary").child(diseaseLibraryID);
        databaseReference.child("disease").setValue(dis);
        databaseReference.child("description").setValue(des);
        databaseReference.child("symptoms").setValue(sym);
        databaseReference.child("cause").setValue(cau);
        databaseReference.child("prevent").setValue(pre);
        databaseReference.child("chemtreat").setValue(che);
        databaseReference.child("orgTreat").setValue(org)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(tungro.this, "Data updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(tungro.this, "Failed to update data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
