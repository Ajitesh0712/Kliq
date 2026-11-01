package com.example.kliq;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class EventDetailsActivity extends AppCompatActivity {

    private TextView tvEventName, tvJoinCode, tvMemberCount;
    private Button btnCopyCode, btnOpenGallery;

    private FirebaseFirestore db;
    private String eventId;
    private String joinCode;

    private ListenerRegistration memberListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        tvEventName = findViewById(R.id.tvEventName);
        tvJoinCode = findViewById(R.id.tvJoinCode);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        btnCopyCode = findViewById(R.id.btnCopyCode);
        btnOpenGallery = findViewById(R.id.btnOpenGallery);

        db = FirebaseFirestore.getInstance();

        eventId = getIntent().getStringExtra("eventId");

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Event ID is missing",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadEventDetails();
        listenForMemberCount();

        btnCopyCode.setOnClickListener(v -> copyJoinCode());

        btnOpenGallery.setOnClickListener(v -> {

            Intent intent = new Intent(
                    EventDetailsActivity.this,
                    GalleryActivity.class
            );

            intent.putExtra("eventId", eventId);

            startActivity(intent);
        });
    }

    private void loadEventDetails() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Toast.makeText(this, "Event not found",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    String name = document.getString("name");
                    joinCode = document.getString("joinCode");

                    tvEventName.setText(
                            name != null ? name : "Unnamed Event"
                    );

                    tvJoinCode.setText(
                            joinCode != null ? joinCode : "Unavailable"
                    );

                    btnCopyCode.setEnabled(
                            joinCode != null && !joinCode.isEmpty()
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load event: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void listenForMemberCount() {
        memberListener = db.collection("events")
                .document(eventId)
                .collection("members")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        tvMemberCount.setText("Members: unavailable");
                        return;
                    }

                    if (snapshot != null) {
                        tvMemberCount.setText(
                                "Members: " + snapshot.size()
                        );
                    }
                });
    }

    private void copyJoinCode() {
        if (joinCode == null || joinCode.isEmpty()) {
            Toast.makeText(this, "Invite code isn't loaded yet",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(
                        Context.CLIPBOARD_SERVICE
                );

        ClipData clip = ClipData.newPlainText("Kliq Invite Code", joinCode);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Invite code copied!",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (memberListener != null) {
            memberListener.remove();
        }
    }
}