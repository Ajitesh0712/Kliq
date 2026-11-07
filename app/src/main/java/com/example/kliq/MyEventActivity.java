package com.example.kliq;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
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

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private void loadMyEvents() {

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        eventsContainer.removeAllViews();

        db.collection("users")
                .document(uid)
                .collection("joinedEvents")
                .get()
                .addOnSuccessListener(joinedEvents -> {

                    if (isFinishing() || isDestroyed()) return;

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
                        progressBar.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    loadEventDetails(eventIds, uid);
                })
                .addOnFailureListener(e -> {

                    progressBar.setVisibility(View.GONE);

                    Log.e("MyEventActivity",
                            "Failed to load joinedEvents index", e);

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

                        if (isFinishing() || isDestroyed()) return;

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

                        Log.e("MyEventActivity",
                                "Failed to load event: " + eventId, e);

                        completed[0]++;
                        finishLoadingIfDone(completed[0], total);
                    });
        }
    }

    private void finishLoadingIfDone(int completed, int total) {

        if (completed == total) {

            progressBar.setVisibility(View.GONE);

            if (eventsContainer.getChildCount() == 0) {
                tvEmpty.setVisibility(View.VISIBLE);
            } else {
                tvEmpty.setVisibility(View.GONE);
            }
        }
    }

    private void addEventCard(
            String eventId,
            String name,
            boolean isAdmin,
            String joinCode
    ) {

        // CARD CONTAINER

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(
                dp(22), dp(22),
                dp(22), dp(20)
        );

        GradientDrawable cardBackground = new GradientDrawable();
        cardBackground.setColor(Color.parseColor("#202020"));
        cardBackground.setCornerRadius(dp(14));

        card.setBackground(cardBackground);
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(cardParams);

        // EVENT NAME

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(20);
        tvName.setTextColor(Color.WHITE);
        tvName.setTypeface(null, Typeface.BOLD);

        // ROLE

        TextView tvRole = new TextView(this);
        tvRole.setText(
                isAdmin
                        ? "ADMIN  •  CREATED BY YOU"
                        : "MEMBER  •  JOINED EVENT"
        );

        tvRole.setTextSize(11);
        tvRole.setLetterSpacing(0.08f);
        tvRole.setTextColor(Color.parseColor("#A3A3A3"));

        LinearLayout.LayoutParams roleParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        roleParams.topMargin = dp(8);
        tvRole.setLayoutParams(roleParams);

        // INVITE CODE

        TextView tvCode = new TextView(this);

        tvCode.setText(
                joinCode.isEmpty()
                        ? "INVITE CODE  •  UNAVAILABLE"
                        : "INVITE CODE  •  " + joinCode
        );

        tvCode.setTextSize(13);
        tvCode.setTextColor(Color.parseColor("#D0D0D0"));

        LinearLayout.LayoutParams codeParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        codeParams.topMargin = dp(18);
        tvCode.setLayoutParams(codeParams);

        // OPEN EVENT

        TextView tvOpen = new TextView(this);
        tvOpen.setText("OPEN EVENT     →");
        tvOpen.setTextSize(13);
        tvOpen.setTextColor(Color.WHITE);
        tvOpen.setTypeface(null, Typeface.BOLD);
        tvOpen.setGravity(Gravity.END);

        LinearLayout.LayoutParams openParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        openParams.topMargin = dp(22);
        tvOpen.setLayoutParams(openParams);

        // ADD VIEWS

        card.addView(tvName);
        card.addView(tvRole);
        card.addView(tvCode);
        card.addView(tvOpen);

        // OPEN EVENT DETAILS

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

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh events when returning to this screen.
        if (eventsContainer != null && auth != null
                && auth.getCurrentUser() != null) {
            loadMyEvents();
        }
    }
}