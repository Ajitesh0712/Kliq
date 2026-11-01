package com.example.kliq;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

public class JoinEventActivity extends AppCompatActivity {

    private EditText etJoinCode;
    private Button btnJoinEvent;
    private ProgressBar progressBar;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_event);

        etJoinCode = findViewById(R.id.etJoinCode);
        btnJoinEvent = findViewById(R.id.btnJoinEvent);
        progressBar = findViewById(R.id.progressBar);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnJoinEvent.setOnClickListener(v -> joinEvent());
    }

    private void joinEvent() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please log in first.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String code = etJoinCode.getText()
                .toString()
                .trim()
                .toUpperCase(Locale.ROOT);

        if (code.isEmpty()) {
            etJoinCode.setError("Enter an invite code");
            etJoinCode.requestFocus();
            return;
        }

        if (!code.matches("[A-F0-9]{8}")) {
            etJoinCode.setError("Enter a valid 8-character code");
            etJoinCode.requestFocus();
            return;
        }

        setLoading(true);

        DocumentReference codeRef =
                db.collection("joinCodes").document(code);

        codeRef.get().addOnSuccessListener(codeSnapshot -> {

            if (!codeSnapshot.exists()) {
                setLoading(false);

                Toast.makeText(
                        JoinEventActivity.this,
                        "Invalid invite code.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String eventId = codeSnapshot.getString("eventId");

            if (eventId == null || eventId.trim().isEmpty()) {
                setLoading(false);

                Toast.makeText(
                        JoinEventActivity.this,
                        "This invite code is not configured correctly.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            addMemberToEvent(eventId, user.getUid(), code);

        }).addOnFailureListener(e -> {

            setLoading(false);

            Log.e(
                    "JoinEventActivity",
                    "Invite code lookup failed",
                    e
            );

            Toast.makeText(
                    JoinEventActivity.this,
                    "Could not verify invite code. Check your connection and Firestore rules.",
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    private void addMemberToEvent(
            String eventId,
            String uid,
            String code
    ) {

        DocumentReference memberRef = db.collection("events")
                .document(eventId)
                .collection("members")
                .document(uid);

        // The user's My Events index document.
        DocumentReference joinedEventRef = db.collection("users")
                .document(uid)
                .collection("joinedEvents")
                .document(eventId);

        db.runTransaction(transaction -> {

            // Read both documents before making any writes.
            DocumentSnapshot memberSnapshot =
                    transaction.get(memberRef);

            DocumentSnapshot indexSnapshot =
                    transaction.get(joinedEventRef);

            // Handle users who are already members.
            if (memberSnapshot.exists()) {

                // Their membership exists, but the My Events index
                // might be missing from an earlier version of Kliq.
                if (!indexSnapshot.exists()) {

                    String existingRole =
                            memberSnapshot.getString("role");

                    if (existingRole == null) {
                        existingRole = "member";
                    }

                    Map<String, Object> indexData =
                            new HashMap<>();

                    indexData.put("eventId", eventId);
                    indexData.put("role", existingRole);
                    indexData.put(
                            "joinedAt",
                            FieldValue.serverTimestamp()
                    );

                    transaction.set(joinedEventRef, indexData);

                    return "INDEX_REPAIRED";
                }

                throw new IllegalStateException("ALREADY_MEMBER");
            }

            // 1. Create the event membership.
            Map<String, Object> memberData = new HashMap<>();

            memberData.put("role", "member");
            memberData.put("joinCode", code);
            memberData.put("joinedAt", FieldValue.serverTimestamp());

            transaction.set(memberRef, memberData);

            // 2. Add the event to My Events.
            Map<String, Object> indexData = new HashMap<>();

            indexData.put("eventId", eventId);
            indexData.put("role", "member");
            indexData.put("joinedAt", FieldValue.serverTimestamp());

            transaction.set(joinedEventRef, indexData);

            return "JOINED";

        }).addOnSuccessListener(result -> {

            setLoading(false);

            if ("INDEX_REPAIRED".equals(result)) {

                Toast.makeText(
                        JoinEventActivity.this,
                        "Your My Events list has been updated.",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                Toast.makeText(
                        JoinEventActivity.this,
                        "You joined the event!",
                        Toast.LENGTH_SHORT
                ).show();
            }

            Intent intent = new Intent(
                    JoinEventActivity.this,
                    EventDetailsActivity.class
            );

            intent.putExtra("eventId", eventId);

            startActivity(intent);
            finish();

        }).addOnFailureListener(e -> {

            setLoading(false);

            String message = e.getMessage();

            if (message != null
                    && message.contains("ALREADY_MEMBER")) {

                Toast.makeText(
                        JoinEventActivity.this,
                        "You are already a member of this event.",
                        Toast.LENGTH_LONG
                ).show();

            } else {

                Log.e(
                        "JoinEventActivity",
                        "Joining event failed",
                        e
                );

                Toast.makeText(
                        JoinEventActivity.this,
                        "Could not join event. Check Firestore rules.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void setLoading(boolean loading) {

        progressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        btnJoinEvent.setEnabled(!loading);
        etJoinCode.setEnabled(!loading);
    }
}