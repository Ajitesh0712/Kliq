
package com.example.kliq;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.WriteBatch;

public class EventDetailsActivity extends AppCompatActivity {

    private TextView tvEventName, tvJoinCode, tvMemberCount;

    private Button btnCopyCode, btnOpenGallery, btnLeaveEvent;

    private ImageView ivJoinQRCode;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

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
        btnLeaveEvent = findViewById(R.id.btnLeaveEvent);

        ivJoinQRCode = findViewById(R.id.ivJoinQRCode);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

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

        btnLeaveEvent.setOnClickListener(v -> showLeaveEventDialog());
    }

    // LOAD EVENT DETAILS AND GENERATE QR CODE

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

                    boolean validJoinCode =
                            joinCode != null && !joinCode.trim().isEmpty();

                    btnCopyCode.setEnabled(validJoinCode);

                    if (validJoinCode) {

                        Bitmap qrBitmap = QRCodeHelper.generateQRCode(
                                joinCode.trim(),
                                600
                        );

                        if (qrBitmap != null) {
                            ivJoinQRCode.setImageBitmap(qrBitmap);
                        } else {
                            Toast.makeText(
                                    EventDetailsActivity.this,
                                    "Could not generate QR code",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                    } else {
                        ivJoinQRCode.setImageDrawable(null);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load event: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    // LISTEN FOR MEMBER COUNT

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

    // COPY INVITATION CODE

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

        ClipData clip = ClipData.newPlainText(
                "Kliq Invite Code",
                joinCode
        );

        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Invite code copied!",
                Toast.LENGTH_SHORT).show();
    }

    // SHOW LEAVE EVENT CONFIRMATION

    private void showLeaveEventDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Leave Event")
                .setMessage(
                        "Are you sure you want to leave this event? " +
                                "You will no longer have access to its gallery."
                )
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton("Leave", (dialog, which) -> {
                    leaveEvent();
                })
                .show();
    }

    // LEAVE EVENT

    private void leaveEvent() {

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid event",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnLeaveEvent.setEnabled(false);

        DocumentReference memberRef = db.collection("events")
                .document(eventId)
                .collection("members")
                .document(uid);

        DocumentReference joinedEventRef = db.collection("users")
                .document(uid)
                .collection("joinedEvents")
                .document(eventId);

        // Check membership and role before leaving.

        memberRef.get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (!documentSnapshot.exists()) {

                        btnLeaveEvent.setEnabled(true);

                        Toast.makeText(this,
                                "You are not a member of this event",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String role = documentSnapshot.getString("role");

                    // Admin is not allowed to leave.

                    if ("admin".equalsIgnoreCase(role)) {

                        btnLeaveEvent.setEnabled(true);

                        Toast.makeText(this,
                                "Event admin cannot leave. Transfer ownership first.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (!"member".equalsIgnoreCase(role)) {

                        btnLeaveEvent.setEnabled(true);

                        Toast.makeText(this,
                                "Unable to leave this event",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Delete both documents atomically.

                    WriteBatch batch = db.batch();

                    batch.delete(memberRef);
                    batch.delete(joinedEventRef);

                    batch.commit()
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(this,
                                        "You left the event successfully",
                                        Toast.LENGTH_SHORT).show();

                                // Return to previous screen.
                                finish();
                            })
                            .addOnFailureListener(e -> {

                                if (isFinishing() || isDestroyed()) return;

                                btnLeaveEvent.setEnabled(true);

                                Toast.makeText(this,
                                        "Could not leave event: " +
                                                e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {

                    if (isFinishing() || isDestroyed()) return;

                    btnLeaveEvent.setEnabled(true);

                    Toast.makeText(this,
                            "Could not verify membership: " +
                                    e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // REMOVE FIRESTORE LISTENER

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (memberListener != null) {
            memberListener.remove();
        }
    }
}
