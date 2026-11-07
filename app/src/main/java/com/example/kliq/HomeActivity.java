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

        Button btnCreateEvent = findViewById(R.id.btnCreateEvent);
        Button btnJoinEvent = findViewById(R.id.btnJoinEvent);
        Button btnMyEvents = findViewById(R.id.btnMyEvents);

        loadUserName();

        // MY EVENTS
        btnMyEvents.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    MyEventActivity.class
            );
            startActivity(intent);
        });

        // CREATE EVENT
        btnCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    CreateEventActivity.class
            );
            startActivity(intent);
        });

        // JOIN EVENT
        btnJoinEvent.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    JoinEventActivity.class
            );
            startActivity(intent);
        });

        // LOGOUT
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    // LOAD USER NAME

    private void loadUserName() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        currentUid = currentUser.getUid();

        String cacheKey = "userName_" + currentUid;

        // Display cached name immediately.

        String cachedName = preferences.getString(cacheKey, null);

        if (cachedName != null && !cachedName.trim().isEmpty()) {
            tvWelcome.setText("Welcome, " + cachedName + "!");
        } else {
            tvWelcome.setText("Welcome to Kliq!");
        }

        // Fetch latest profile from Firestore.

        db.collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener(document -> {

                    FirebaseUser latestUser = mAuth.getCurrentUser();

                    if (latestUser == null ||
                            !currentUid.equals(latestUser.getUid())) {
                        return;
                    }

                    if (document.exists()) {

                        String name = document.getString("name");

                        if (name != null && !name.trim().isEmpty()) {

                            tvWelcome.setText("Welcome, " + name + "!");

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

    // LOGOUT

    private void logoutUser() {

        mAuth.signOut();

        redirectToLogin();
    }

    // REDIRECT TO LOGIN

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
