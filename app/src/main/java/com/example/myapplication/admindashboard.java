package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class admindashboard extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private boolean doubleBackToExitPressedOnce = false;

    private RecyclerView recyclerView;
    private RecycleViewAdapter userAdapter;
    private List<User> userList;
    private List<User> originalUserList;  // Store the original list
    private List<User> pendingUserList;  // Store the pending users list

    private Button toggleUserButton;
    private boolean showingPendingUsers = false; // Track which list is shown

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admindashboard);

        toggleUserButton = findViewById(R.id.toggleUserButton);  // Button to toggle between users and pending users
        Button addUserButton = findViewById(R.id.adduser);

        addUserButton.setOnClickListener(v -> {
            // Start the addUser activity
            Intent intent = new Intent(admindashboard.this, addUser.class);
            startActivity(intent);
        });

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        String userName = sharedPreferences.getString("userName", "");
        boolean isAdmin = sharedPreferences.getBoolean("isAdmin", false);

        if (!isLoggedIn) {
            Intent intent = new Intent(admindashboard.this, Login.class);
            startActivity(intent);
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Welcome " + userName + "!");
        toolbar.setTitleTextColor(Color.BLACK);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.open_nav,
                R.string.close_nav
        );
        toggle.getDrawerArrowDrawable().setColor(Color.BLACK);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (isAdmin) {
            navigationView.getMenu().findItem(R.id.nav_scan).setTitle("Admin Dashboard");
            navigationView.getMenu().findItem(R.id.nav_conTwo).setVisible(true);
        } else {
            navigationView.getMenu().findItem(R.id.nav_conTwo).setVisible(false);
        }

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        originalUserList = new ArrayList<>();  // Initialize the original list
        pendingUserList = new ArrayList<>();  // Initialize the pending users list
        userAdapter = new RecycleViewAdapter(this, userList, showingPendingUsers);
        recyclerView.setAdapter(userAdapter);

        fetchUsers();
        fetchPendingUsers();

        // Set up the SearchView to listen to text changes
        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return false;
            }
        });

        TextView title = findViewById(R.id.title);  // Reference the TextView with the id 'title'

        toggleUserButton.setOnClickListener(v -> {
            showingPendingUsers = !showingPendingUsers;

            // Clear the current list and add the appropriate list based on the toggle state
            if (showingPendingUsers) {
                userList.clear();
                userList.addAll(pendingUserList);
                toggleUserButton.setText("Show Users");  // Change button text to "Show Users"
                title.setText("Pending Users");  // Change TextView text to "Pending Users"
                addUserButton.setVisibility(View.GONE);
            } else {
                userList.clear();
                userList.addAll(originalUserList);
                toggleUserButton.setText("Show Pending Users");  // Change button text to "Show Pending Users"
                title.setText("Users");  // Change TextView text to "Users"
                addUserButton.setVisibility(View.VISIBLE);
            }

            // Update the adapter's showingPendingUsers flag to adjust button visibility
            userAdapter.setShowingPendingUsers(showingPendingUsers);

            // Notify the adapter that the data has changed
            userAdapter.filterList(userList);  // Ensure the filtered list is updated in the adapter
            userAdapter.notifyDataSetChanged();  // Refresh the RecyclerView
        });


        if (savedInstanceState == null) {
            loadFragment(new scan());
            navigationView.setCheckedItem(R.id.nav_scan);
        }
    }

    private void fetchUsers() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                originalUserList.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String id = userSnapshot.getKey();
                    String name = userSnapshot.child("name").getValue(String.class);

                    if (id != null && name != null) {
                        originalUserList.add(new User(id, name));
                    }
                }
                if (!showingPendingUsers) {
                    userList.clear();
                    userList.addAll(originalUserList);
                    toggleUserButton.setText("Show Pending Users");  // Reset button text
                }
                userAdapter.notifyDataSetChanged();  // Update the RecyclerView
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Failed to fetch users: " + error.getMessage());
            }
        });
    }

    private void fetchPendingUsers() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("pendingusers");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingUserList.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String id = userSnapshot.getKey();
                    String name = userSnapshot.child("username").getValue(String.class);

                    if (id != null && name != null) {
                        pendingUserList.add(new User(id, name));
                    }
                }
                if (showingPendingUsers) {
                    userList.clear();
                    userList.addAll(pendingUserList);
                    toggleUserButton.setText("Show Users");  // Reset button text
                }
                userAdapter.notifyDataSetChanged();  // Update the RecyclerView
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Failed to fetch pending users: " + error.getMessage());
            }
        });
    }

    private void filterList(String query) {
        List<User> filteredList = new ArrayList<>();
        for (User user : (showingPendingUsers ? pendingUserList : originalUserList)) {
            if (user.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(user);
            }
        }
        userAdapter.filterList(filteredList);  // Pass the filtered list to the adapter
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
        } else if (itemId == R.id.nav_conThree) {
            showLogoutConfirmationDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(androidx.fragment.app.Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
                    sharedPreferences.edit().clear().apply();
                    Intent intent = new Intent(admindashboard.this, Login.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null);

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Retrieve the buttons and set their text colors
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.white));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.white));
    }

}

