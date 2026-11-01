package com.example.kliq;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyEventActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private LinearLayout eventsContainer;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_event);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        eventsContainer = findViewById(R.id.eventsContainer);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        loadMyEvents();
    }

    private void loadMyEvents() {
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        progressBar.setVisibility(android.view.View.VISIBLE);
        tvEmpty.setVisibility(android.view.View.GONE);
        eventsContainer.removeAllViews();

        db.collection("users")
                .document(uid)
                .collection("joinedEvents")
                .get()
                .addOnSuccessListener(joinedEvents -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    List<String> eventIds = new ArrayList<>();

                    for (DocumentSnapshot document : joinedEvents) {
                        String eventId = document.getString("eventId");

                        if (eventId == null || eventId.trim().isEmpty()) {
                            eventId = document.getId();
                        }

                        if (!eventIds.contains(eventId)) {
                            eventIds.add(eventId);
                        }
                    }

                    if (eventIds.isEmpty()) {
                        progressBar.setVisibility(android.view.View.GONE);
                        tvEmpty.setVisibility(android.view.View.VISIBLE);
                        return;
                    }

                    loadEventDetails(eventIds, uid);
                })
                .addOnFailureListener(e -> {

                    progressBar.setVisibility(android.view.View.GONE);

                    android.util.Log.e(
                            "MyEventActivity",
                            "Failed to load joinedEvents index",
                            e
                    );

                    Toast.makeText(
                            MyEventActivity.this,
                            "Couldn't load events: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void loadEventDetails(List<String> eventIds, String uid) {
        final int[] completed = {0};
        final int total = eventIds.size();

        for (String eventId : eventIds) {
            db.collection("events")
                    .document(eventId)
                    .get()
                    .addOnSuccessListener(event -> {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (event.exists()) {
                            String name = event.getString("name");
                            String createdBy = event.getString("createdBy");
                            String joinCode = event.getString("joinCode");

                            addEventCard(
                                    eventId,
                                    name != null ? name : "Untitled Event",
                                    uid.equals(createdBy),
                                    joinCode != null ? joinCode : ""
                            );
                        }

                        completed[0]++;
                        finishLoadingIfDone(completed[0], total);
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e(
                                "MyEventActivity",
                                "Failed to load event: " + eventId,
                                e
                        );

                        completed[0]++;
                        finishLoadingIfDone(completed[0], total);
                    });
        }
    }

    private void finishLoadingIfDone(int completed, int total) {
        if (completed == total) {
            progressBar.setVisibility(android.view.View.GONE);

            if (eventsContainer.getChildCount() == 0) {
                tvEmpty.setVisibility(android.view.View.VISIBLE);
            } else {
                tvEmpty.setVisibility(android.view.View.GONE);
            }
        }
    }

    private void addEventCard(
            String eventId,
            String name,
            boolean isAdmin,
            String joinCode
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(32, 28, 32, 28);
        card.setBackgroundColor(0xFFF2EDFF);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 24);
        card.setLayoutParams(cardParams);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(20);
        tvName.setTextColor(0xFF24202E);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvRole = new TextView(this);
        tvRole.setText(isAdmin ? "Admin • Created by you" : "Member • Joined event");
        tvRole.setTextSize(14);
        tvRole.setTextColor(0xFF6D4AFF);

        TextView tvCode = new TextView(this);
        tvCode.setText("Invite code: " + joinCode);
        tvCode.setTextSize(14);

        TextView tvOpen = new TextView(this);
        tvOpen.setText("Open Event →");
        tvOpen.setTextSize(15);
        tvOpen.setTextColor(0xFF6D4AFF);
        tvOpen.setTypeface(null, android.graphics.Typeface.BOLD);
        tvOpen.setGravity(Gravity.END);
        tvOpen.setPadding(0, 24, 0, 0);

        card.addView(tvName);
        card.addView(tvRole);
        card.addView(tvCode);
        card.addView(tvOpen);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MyEventActivity.this,
                    EventDetailsActivity.class
            );
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        eventsContainer.addView(card);
    }
}