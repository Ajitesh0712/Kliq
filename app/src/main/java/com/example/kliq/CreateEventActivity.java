package com.example.kliq;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.UUID;

public class CreateEventActivity extends AppCompatActivity {

    private EditText etEventName;
    private Button btnCreateEvent;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        etEventName = findViewById(R.id.etEventName);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnCreateEvent.setOnClickListener(v -> createEvent());
    }

    private void createEvent() {
        String eventName = etEventName.getText().toString().trim();

        if (eventName.isEmpty()) {
            etEventName.setError("Please enter an event name");
            etEventName.requestFocus();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this,
                    "Please log in first",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setLoading(true);

        // Firestore generates a unique document ID.
        String eventId = db.collection("events").document().getId();

        // Random 8-character code. A collision is unlikely, but
        // uniqueness is not guaranteed by this generation alone.
        String joinCode = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        WriteBatch batch = db.batch();

        // Event document.
        batch.set(
                db.collection("events").document(eventId),
                new java.util.HashMap<String, Object>() {{
                    put("name", eventName);
                    put("createdBy", user.getUid());
                    put("joinCode", joinCode);
                    put("createdAt", FieldValue.serverTimestamp());
                }}
        );

        // Register the creator as an admin.
        batch.set(
                db.collection("events")
                        .document(eventId)
                        .collection("members")
                        .document(user.getUid()),
                new java.util.HashMap<String, Object>() {{
                    put("role", "admin");
                    put("joinedAt", FieldValue.serverTimestamp());
                }}
        );

        batch.commit()
                .addOnSuccessListener(unused -> {

                    setLoading(false);

                    Intent intent = new Intent(
                            CreateEventActivity.this,
                            EventDetailsActivity.class
                    );

                    intent.putExtra("eventId", eventId);

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);

                    Toast.makeText(
                            CreateEventActivity.this,
                            "Could not create event: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        btnCreateEvent.setEnabled(!loading);
        etEventName.setEnabled(!loading);
    }
}