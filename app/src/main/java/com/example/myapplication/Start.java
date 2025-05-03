package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.carousel.CarouselSnapHelper;

import java.util.Arrays;
import java.util.List;

public class Start extends AppCompatActivity {

    private RecyclerView carouselRecyclerView;
    private CarouselAdapter adapter;
    private List<Integer> images;
    private int currentPosition = 0;
    private LinearLayout dotLayout;
    private Button loginButton;
    private Button nextButton;  // Move nextButton here
    private Button backButton;  // Move backButton here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // Initialize buttons and check login status
        loginButton = findViewById(R.id.buttonstart);
        nextButton = findViewById(R.id.nextButton);
        backButton = findViewById(R.id.backButton);

        // Check user and admin sessions
        SharedPreferences userPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        boolean isUserLoggedIn = userPrefs.getBoolean("isUserLoggedIn", false);

        SharedPreferences adminPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        boolean isAdminLoggedIn = adminPrefs.getBoolean("isLoggedIn", false);

        // Redirect based on login status
        if (isAdminLoggedIn) {
            navigateToActivity(admindashboard.class);
        } else if (isUserLoggedIn) {
            navigateToActivity(MainActivity.class);
        } else {
            setupCarouselRecyclerView();
            setupNavigationButtons(); // Ensure navigation buttons are set up
            setupLoginButton(); // Call this method to set up the login button
        }
    }


    private void setupLoginButton() {
        // Set onClickListener for login button
        loginButton.setOnClickListener(v -> {
            navigateToActivity(Login.class); // Replace with your target activity
        });
    }

    private void setupCarouselRecyclerView() {
        carouselRecyclerView = findViewById(R.id.carouselRecyclerView);
        dotLayout = findViewById(R.id.dotLayout);

        // Set LayoutManager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        carouselRecyclerView.setLayoutManager(layoutManager);

        // Load images
        images = getImages();
        adapter = new CarouselAdapter(images);
        carouselRecyclerView.setAdapter(adapter);

        // Disable swipe
        carouselRecyclerView.setOnTouchListener((v, event) -> true);

        // Add dots to the layout
        addDots(images.size());

        // Initially update the dots
        updateDots(currentPosition);

        // Hide login button initially
        loginButton.setVisibility(View.GONE);
    }

    private void setupNavigationButtons() {
        // Set onClickListener for next button
        nextButton.setOnClickListener(v -> {
            if (currentPosition < images.size() - 1) {
                currentPosition++;
                carouselRecyclerView.smoothScrollToPosition(currentPosition);
                updateDots(currentPosition);
                showLoginButtonIfLastPage();
            }
        });

        // Set onClickListener for back button
        backButton.setOnClickListener(v -> {
            if (currentPosition > 0) {
                currentPosition--;
                carouselRecyclerView.smoothScrollToPosition(currentPosition);
                updateDots(currentPosition);
                showLoginButtonIfLastPage();
            }
        });
    }

    private List<Integer> getImages() {
        return Arrays.asList(
                R.drawable.welcome,
                R.drawable.step1,
                R.drawable.step2,
                R.drawable.step3,
                R.drawable.result,
                R.drawable.end
        );
    }

    private void showLoginButtonIfLastPage() {
        Log.d("DEBUG", "Current position: " + currentPosition); // Log the current position for debugging

        if (currentPosition == images.size() - 1) {
            // Last page: Show login button, hide next and back buttons
            loginButton.setVisibility(View.VISIBLE);
            nextButton.setVisibility(View.GONE);
            backButton.setVisibility(View.GONE);
            Log.d("DEBUG", "On the last page");
        } else if (currentPosition == 0) {
            // First page: Hide login button, make back button invisible, show next button
            loginButton.setVisibility(View.GONE);
            nextButton.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.INVISIBLE); // This makes the back button invisible but still reserves its space
            Log.d("DEBUG", "On the first page");
        } else {
            // Middle pages: Hide login button, show both navigation buttons
            loginButton.setVisibility(View.GONE);
            nextButton.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.VISIBLE);
            Log.d("DEBUG", "On a middle page");
        }
    }



    private void navigateToActivity(Class<?> targetActivity) {
        Intent intent = new Intent(Start.this, targetActivity);
        startActivity(intent);
        finish();
    }

    private void addDots(int count) {
        dotLayout.removeAllViews();

        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(this);
            dot.setImageResource(R.drawable.dot_inactive);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(5, 0, 5, 0);
            dot.setLayoutParams(params);
            dotLayout.addView(dot);
        }
    }

    private void updateDots(int position) {
        for (int i = 0; i < dotLayout.getChildCount(); i++) {
            ImageView dot = (ImageView) dotLayout.getChildAt(i);
            if (i == position) {
                dot.setImageResource(R.drawable.dot_active);
            } else {
                dot.setImageResource(R.drawable.dot_inactive);
            }
        }
    }
}




