package com.example.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class splash extends AppCompatActivity {

    private static int SPLASH_SCREEN = 5000;

    // Variables
    ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        // Initialize ImageView
        image = findViewById(R.id.gifImageView);

        // Load GIF using Glide
        if (image != null) {
            Glide.with(this)
                    .asGif()
                    .load(R.drawable.sunricesplash) // Ensure this file exists in res/drawable
                    .into(image);
        }

        // Check Internet Connection
        if (!isConnected()) {
            showNoInternetDialog();
        } else {
            // Splash Screen Delay
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(splash.this, Start.class);
                    startActivity(intent);
                    finish();
                }
            }, SPLASH_SCREEN);
        }
    }

    // Function to check internet connection
    private boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    // Function to show the "No Internet" dialog
    private void showNoInternetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Internet")
                .setMessage("You are not connected to the internet. Please check your connection.")
                .setCancelable(false)
                .setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Retry checking for internet connection
                        if (isConnected()) {
                            // Proceed to the next activity if connected
                            Intent intent = new Intent(splash.this, Start.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // Show dialog again if still no internet
                            showNoInternetDialog();
                        }
                    }
                })
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Exit the app
                        finish();
                    }
                })
                .show();
    }
}
