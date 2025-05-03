package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.example.myapplication.ml.ModelUnquant;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private boolean doubleBackToExitPressedOnce = false;

    TextView result, demoTxt, classified, clickHere;
    ImageView imageView, temporaryImageView;
    Button picture, upload;
    int imageSize = 224; // Default image size
    DatabaseReference databaseReference; // Firebase Database reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        Context appContext = getApplicationContext();
        ConnectivityManager connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                Log.d("NetworkStatus", "Connected to network");
            } else {
                Log.d("NetworkStatus", "No network connection");
            }
        }


        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isUserLoggedIn", false); // Default is false
        String userName = sharedPreferences.getString("userName", ""); // Retrieve the user's name
        String userId = sharedPreferences.getString("userId", ""); // Retrieve the user ID

        if (!isLoggedIn) {
            // If not logged in, navigate to LoginActivity
            Intent intent = new Intent(MainActivity.this, Login.class);
            startActivity(intent);
            finish(); // Close current activity to avoid going back to MainActivity
            return;
        }

        // Initialize UI elements
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Hi " + userName +"!"); // Use the retrieved user name
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);

        // Set the hamburger icon color to black
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.black));

        toggle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new scan()).commit();
            navigationView.setCheckedItem(R.id.nav_scan);
        }

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("history");

        result = findViewById(R.id.result);
        temporaryImageView = findViewById(R.id.temporaryImageView);
        imageView = findViewById(R.id.imageView);
        picture = findViewById(R.id.button);
        upload = findViewById(R.id.buttonUpload); // Initialize the upload button

        demoTxt = findViewById(R.id.demoText);
        clickHere = findViewById(R.id.click_here);
        classified = findViewById(R.id.classified);


        demoTxt.setVisibility(View.VISIBLE);
        clickHere.setVisibility(View.GONE);
        classified.setVisibility(View.GONE);
        result.setVisibility(View.GONE);

        picture.setOnClickListener(v -> {
            // Launch camera via permission
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(Intent.createChooser(cameraIntent, "Select Camera App"), 1);
                } else {
                    Toast.makeText(MainActivity.this, "No camera apps available", Toast.LENGTH_SHORT).show();
                }
            } else {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
            }
        });

        upload.setOnClickListener(v -> {
            // Launch file picker
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 2);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (data == null) {
                Toast.makeText(this, "No data received", Toast.LENGTH_SHORT).show();
                return;
            }

            if (requestCode == 1) { // Capture photo
                Bitmap image = (Bitmap) data.getExtras().get("data");
                if (image != null) {
                    processImage(image);
                } else {
                    Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == 2) { // Upload photo
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        Bitmap image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                        processImage(image);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void processImage(Bitmap image) {
        // Dimension for the thumbnail
        int dimension = Math.min(image.getWidth(), image.getHeight());
        image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);

        // Initially hide everything (imageView, result, temporary image/GIF)
        temporaryImageView.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        result.setVisibility(View.GONE);
        classified.setVisibility(View.GONE);
        demoTxt.setVisibility(View.GONE);
        clickHere.setVisibility(View.GONE);

        // Show everything after a 2-second delay (including the temporary GIF/image and the result text)
        Bitmap finalImage = image;
        new Handler().postDelayed(() -> {
            Glide.with(this)
                    .load(R.drawable.scanloading)  // Your GIF resource
                    .into(temporaryImageView);  // Set it to the ImageView
            // Show the temporary image or GIF
            temporaryImageView.setVisibility(View.VISIBLE);

            // Show the result texts
            demoTxt.setTextSize(15);
            demoTxt.setTextColor(Color.parseColor("#CE7711")); // Using hex color code
            demoTxt.setAlpha(1f);
            demoTxt.setText("Loading...");
            demoTxt.setVisibility(View.VISIBLE);

            clickHere.setVisibility(View.VISIBLE);
            clickHere.setText("Loading...\nLoading...\nLoading...\nLoading...\nLoading...\nLoading...");
            classified.setVisibility(View.VISIBLE);
            result.setText("Loading...");
            result.setVisibility(View.VISIBLE);
            picture.setAlpha(0.5f);
            upload.setAlpha(0.5f);
            picture.setEnabled(false);
            upload.setEnabled(false);

            // Delay the final image update and classification
            new Handler().postDelayed(() -> {
                // Hide the temporary image/GIF
                temporaryImageView.setVisibility(View.GONE);

                // Show the main imageView and set the final image
                demoTxt.setTextSize(15);
                demoTxt.setTextColor(Color.parseColor("#CE7711")); // Using hex color code
                demoTxt.setAlpha(1f);
                demoTxt.setText("Tap the disease^^^");
                demoTxt.setVisibility(View.VISIBLE);

                picture.setAlpha(1f);
                upload.setAlpha(1f);
                picture.setEnabled(true);
                upload.setEnabled(true);

                imageView.setVisibility(View.VISIBLE);
                imageView.setImageBitmap(finalImage);  // Set the final image

                // Call the classification function after the image is displayed
                classifyImage(finalImage);

            }, 5000);  // Wait for 5 seconds before calling the classification function

        }, 1);  // Immediate delay to start the process
    }




    private void classifyImage(Bitmap image) {
        try {
            ModelUnquant model = ModelUnquant.newInstance(getApplicationContext());

            // Ensure the image is of the correct size
            Bitmap scaledImage = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);

            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, imageSize, imageSize, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            scaledImage.getPixels(intValues, 0, scaledImage.getWidth(), 0, 0, scaledImage.getWidth(), scaledImage.getHeight());

            int pixel = 0;
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValues[pixel++];
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            ModelUnquant.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidence = outputFeature0.getFloatArray();

            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidence.length; i++) {
                if (confidence[i] > maxConfidence) {
                    maxConfidence = confidence[i];
                    maxPos = i;
                }
            }

            Log.d("Classification", "MaxPos: " + maxPos + ", MaxConfidence: " + (maxConfidence * 100) + "%");

            String[] classes = {"Brown Spot", "Leaf Blast", "Neck Blast", "Stem Borer", "Tungro", "Healthy"};

            // Check if the max confidence is below 90%
            String resultText = (maxConfidence < 0.90f) ? "Disease Unidentified" : classes[maxPos];

            StringBuilder resultString = new StringBuilder();
            for (int i = 0; i < classes.length; i++) {
                String confidencePercentage = String.format("%.2f%%", confidence[i] * 100);
                resultString.append(classes[i]).append(": ").append(confidencePercentage).append("\n");
            }

            result.setText(resultText); // Show result as "Disease Unidentified" or the class name
            clickHere.setText(resultString.toString().trim());

            final int finalMaxPos = maxPos;
            final float finalMaxConfidence = maxConfidence; // Capture the max confidence
            String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            // Store classification result in Firebase
            storeClassificationResult(classes[finalMaxPos], finalMaxConfidence, currentDateTime, scaledImage);

            result.setOnClickListener(v -> {
                if ("Disease Unidentified".equals(resultText)) {
                    Toast.makeText(this, "Try to capture or upload an image again", Toast.LENGTH_SHORT).show();
                } else {
                    dislib fragment = new dislib();

                    // Pass data
                    Bundle bundle = new Bundle();
                    bundle.putString("diseaseName", classes[finalMaxPos]);
                    fragment.setArguments(bundle);

                    // Navigate to the fragment
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            });

            model.close();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Model loading failed", Toast.LENGTH_SHORT).show();
        }
    }




    private void storeClassificationResult(String result, float confidence, String timestamp, Bitmap image) {
        // Upload the image to Firebase Storage
        String imageName = "historyimages/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(imageName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();

        imageRef.putBytes(imageData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Get the image URL
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();

                            // Store the classification result along with the image URL in Firebase Database
                            String id = databaseReference.push().getKey();
                            ClassificationHistory history = new ClassificationHistory(id, result, confidence, timestamp, imageUrl);
                            databaseReference.child(id).setValue(history).addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    Toast.makeText(MainActivity.this, "Data Recorded", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Failed to store result", Toast.LENGTH_SHORT).show();
                                }
                            });
                        });
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_scan) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new scan()).commit();
        } else if (itemId == R.id.nav_conOne) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new conOne()).commit();
        } else if (itemId == R.id.nav_conTwo) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new conTwo()).commit();
        } else if (itemId == R.id.nav_dislib) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new dislib()).commit();
        } else if (itemId == R.id.nav_about) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new about()).commit();
        } else if (itemId == R.id.nav_conThree) {
            // Handle logout action with confirmation dialog
            showLogoutConfirmationDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showLogoutConfirmationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialogInterface, which) -> {
                    SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear();
                    editor.apply();

                    Intent intent = new Intent(MainActivity.this, Login.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .create(); // Create the dialog first

        dialog.show(); // Show the dialog

        // Retrieve the buttons and set their text colors
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.white));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.white));
    }


    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
        }
    }
}
