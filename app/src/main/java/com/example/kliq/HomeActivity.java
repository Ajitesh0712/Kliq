package com.example.kliq;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView tvWelcome;
    private Button btnLogout;

    private SharedPreferences preferences;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvWelcome = findViewById(R.id.tvWelcome);
        btnLogout = findViewById(R.id.btnLogout);

        preferences = getSharedPreferences(
                "KliqPrefs",
                MODE_PRIVATE
        );

        loadUserName();
        Button btnCreateEvent = findViewById(R.id.btnCreateEvent);

        btnCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this,
                    CreateEventActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void loadUserName() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Check whether a user is logged in.
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        currentUid = currentUser.getUid();

        // Use a separate cache key for each user's UID.
        String cacheKey = "userName_" + currentUid;

        // Display the cached name immediately, if available.
        String cachedName = preferences.getString(cacheKey, null);

        if (cachedName != null && !cachedName.trim().isEmpty()) {
            tvWelcome.setText("Welcome, " + cachedName + "!");
        } else {
            tvWelcome.setText("Welcome to Kliq!");
        }

        // Fetch the latest profile from Firestore.
        db.collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener(document -> {

                    // Ignore the result if the account changed.
                    FirebaseUser latestUser = mAuth.getCurrentUser();

                    if (latestUser == null ||
                            !currentUid.equals(latestUser.getUid())) {
                        return;
                    }

                    if (document.exists()) {

                        String name = document.getString("name");

                        if (name != null && !name.trim().isEmpty()) {

                            // Update the welcome message.
                            tvWelcome.setText("Welcome, " + name + "!");

                            // Save the name for the next launch.
                            preferences.edit()
                                    .putString(cacheKey, name)
                                    .apply();

                        } else {
                            tvWelcome.setText("Welcome to Kliq!");
                        }

                    } else {
                        tvWelcome.setText("Welcome to Kliq!");
                    }
                })
                .addOnFailureListener(e -> {

                    // Keep the cached name if the network request fails.
                    FirebaseUser latestUser = mAuth.getCurrentUser();

                    if (latestUser == null ||
                            !currentUid.equals(latestUser.getUid())) {
                        return;
                    }

                    String latestCachedName =
                            preferences.getString(cacheKey, null);

                    if (latestCachedName == null ||
                            latestCachedName.trim().isEmpty()) {
                        tvWelcome.setText("Welcome to Kliq!");
                    }

                    Toast.makeText(
                            HomeActivity.this,
                            "Couldn't refresh your profile.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void logoutUser() {

        // Sign out the current user.
        mAuth.signOut();

        // Clear the activity stack and return to LoginActivity.
        redirectToLogin();
    }

    private void redirectToLogin() {

        Intent intent = new Intent(
                HomeActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}