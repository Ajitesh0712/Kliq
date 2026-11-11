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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CreateEventActivity extends AppCompatActivity {

    private EditText etEventName;
    private Button btnCreateEvent;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final int MAX_ATTEMPTS = 5;

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
            Toast.makeText(
                    this,
                    "Please log in first",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        setLoading(true);

        String eventId = db.collection("events").document().getId();

        createEventWithUniqueCode(
                eventId,
                eventName,
                user.getUid(),
                1
        );
    }

    private void createEventWithUniqueCode(
            String eventId,
            String eventName,
            String uid,
            int attempt
    ) {

        if (attempt > MAX_ATTEMPTS) {
            setLoading(false);

            Toast.makeText(
                    this,
                    "Could not generate a unique code. Please try again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String joinCode = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase(Locale.ROOT);

        // Event document.
        DocumentReference eventRef =
                db.collection("events").document(eventId);

        // Creator's membership document.
        DocumentReference memberRef =
                eventRef.collection("members").document(uid);

        // Invite-code mapping document.
        DocumentReference codeRef =
                db.collection("joinCodes").document(joinCode);

        // My Events index document.
        DocumentReference joinedEventRef =
                db.collection("users")
                        .document(uid)
                        .collection("joinedEvents")
                        .document(eventId);

        db.runTransaction(transaction -> {

            // Check whether the invite code already exists.
            DocumentSnapshot codeSnapshot = transaction.get(codeRef);

            if (codeSnapshot.exists()) {
                throw new IllegalStateException("JOIN_CODE_EXISTS");
            }

            // 1. Create the event.
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("name", eventName);
            eventData.put("createdBy", uid);
            eventData.put("joinCode", joinCode);
            eventData.put("createdAt", FieldValue.serverTimestamp());

            transaction.set(eventRef, eventData);

            // 2. Register the creator as an admin.
            Map<String, Object> memberData = new HashMap<>();
            memberData.put("role", "admin");
            memberData.put("joinedAt", FieldValue.serverTimestamp());

            transaction.set(memberRef, memberData);

            // 3. Map the invite code to the event.
            Map<String, Object> codeData = new HashMap<>();
            codeData.put("eventId", eventId);

            transaction.set(codeRef, codeData);

            // 4. Add the event to the creator's My Events index.
            Map<String, Object> joinedEventData = new HashMap<>();
            joinedEventData.put("eventId", eventId);
            joinedEventData.put("role", "admin");
            joinedEventData.put("joinedAt", FieldValue.serverTimestamp());

            transaction.set(joinedEventRef, joinedEventData);

            return null;

        }).addOnSuccessListener(unused -> {

            setLoading(false);

            Toast.makeText(
                    CreateEventActivity.this,
                    "Event created successfully!",
                    Toast.LENGTH_SHORT
            ).show();

            Intent intent = new Intent(
                    CreateEventActivity.this,
                    EventDetailsActivity.class
            );

            intent.putExtra("eventId", eventId);

            startActivity(intent);
            finish();

        }).addOnFailureListener(e -> {

            String message = e.getMessage();

            // Retry if the generated invite code already exists.
            if (message != null
                    && message.contains("JOIN_CODE_EXISTS")
                    && attempt < MAX_ATTEMPTS) {

                createEventWithUniqueCode(
                        eventId,
                        eventName,
                        uid,
                        attempt + 1
                );

            } else {

                setLoading(false);

                Toast.makeText(
                        CreateEventActivity.this,
                        "Could not create event. Check your connection and Firestore rules.",
                        Toast.LENGTH_LONG
                ).show();

                android.util.Log.e(
                        "CreateEventActivity",
                        "Event creation failed",
                        e
                );
            }
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