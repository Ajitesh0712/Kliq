
package com.example.kliq;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoViewerActivity extends AppCompatActivity {

    private static final String DELETE_FUNCTION_URL =
            "https://eujrmgtijfntomumwhjy.supabase.co/functions/v1/kliq-photo-delete";

    private ViewPager2 photoViewPager;
    private TextView tvPhotoPosition;
    private Button btnDeletePhoto;

    private ArrayList<String> photoUrls;
    private ArrayList<String> photoIds;
    private ArrayList<String> photoUploaderIds;

    private String eventId;
    private PhotoViewerAdapter adapter;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private boolean isAdmin = false;
    private boolean isDeleting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);

        photoViewPager = findViewById(R.id.photoViewPager);
        tvPhotoPosition = findViewById(R.id.tvPhotoPosition);
        btnDeletePhoto = findViewById(R.id.btnDeletePhoto);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        photoUrls = getIntent().getStringArrayListExtra("photoUrls");
        photoIds = getIntent().getStringArrayListExtra("photoIds");
        photoUploaderIds =
                getIntent().getStringArrayListExtra("photoUploaderIds");

        eventId = getIntent().getStringExtra("eventId");

        if (photoUrls == null ||
                photoIds == null ||
                photoUploaderIds == null ||
                photoUrls.isEmpty() ||
                photoUrls.size() != photoIds.size() ||
                photoUrls.size() != photoUploaderIds.size() ||
                eventId == null ||
                eventId.trim().isEmpty() ||
                currentUser == null) {

            Toast.makeText(
                    this,
                    "Could not open photos",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        adapter = new PhotoViewerAdapter(this, photoUrls);
        photoViewPager.setAdapter(adapter);

        int startPosition = getIntent().getIntExtra("startPosition", 0);

        startPosition = Math.max(
                0,
                Math.min(startPosition, photoUrls.size() - 1)
        );

        photoViewPager.setCurrentItem(startPosition, false);
        updatePhotoPosition(startPosition);

        photoViewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        updatePhotoPosition(position);
                        updateDeleteButtonVisibility();
                    }
                }
        );

        btnDeletePhoto.setVisibility(Button.GONE);
        btnDeletePhoto.setOnClickListener(v -> confirmDeletePhoto());

        checkAdminPermission();
    }

    // Check the current user's role in this event.

    private void checkAdminPermission() {

        String uid = currentUser.getUid();

        Log.d("KliqDeleteDebug", "========== ADMIN CHECK ==========");
        Log.d("KliqDeleteDebug", "Current UID: " + uid);
        Log.d("KliqDeleteDebug", "Event ID: " + eventId);

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("members")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    String role = document.getString("role");
                    isAdmin = "admin".equals(role);

                    Log.d("KliqDeleteDebug",
                            "Admin permission check: role = " + role);

                    updateDeleteButtonVisibility();
                })
                .addOnFailureListener(e -> {
                    isAdmin = false;

                    Log.e("KliqDeleteDebug",
                            "Admin permission failed", e);

                    updateDeleteButtonVisibility();
                });
    }


    // Show delete button to admins or the uploader of the current photo.

    private void updateDeleteButtonVisibility() {

        if (isFinishing() || isDestroyed() || isDeleting) {
            return;
        }

        int position = photoViewPager.getCurrentItem();

        String uploaderId = photoUploaderIds.get(position);

        boolean isOwner = currentUser != null
                && currentUser.getUid().equals(uploaderId);

        boolean canDelete = isAdmin || isOwner;

        Log.d("KliqDeleteDebug",
                "Button visibility check:");

        Log.d("KliqDeleteDebug", "Is Admin: " + isAdmin);
        Log.d("KliqDeleteDebug", "Is Owner: " + isOwner);
        Log.d("KliqDeleteDebug", "Can Delete: " + canDelete);

        btnDeletePhoto.setVisibility(
                canDelete ? View.VISIBLE : View.GONE
        );

        btnDeletePhoto.bringToFront();

        Log.d("KliqDeleteDebug",
                "Actual button visibility: "
                        + btnDeletePhoto.getVisibility());

        Log.d("KliqDeleteDebug",
                "Button height: "
                        + btnDeletePhoto.getHeight());
    }


    private boolean canDeleteCurrentPhoto() {

        if (currentUser == null) {
            return false;
        }

        int position = photoViewPager.getCurrentItem();

        if (position < 0 || position >= photoUploaderIds.size()) {
            return false;
        }

        String uploaderId = photoUploaderIds.get(position);

        boolean isOwner = currentUser.getUid().equals(uploaderId);

        return isAdmin || isOwner;
    }

    private void confirmDeletePhoto() {

        if (!canDeleteCurrentPhoto() || isDeleting) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete photo?")
                .setMessage(
                        "This photo will be permanently removed from this event."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteCurrentPhoto();
                })
                .show();
    }

    private void deleteCurrentPhoto() {

        if (!canDeleteCurrentPhoto() || isDeleting || currentUser == null) {
            return;
        }

        int position = photoViewPager.getCurrentItem();

        if (position < 0 || position >= photoIds.size()) {
            return;
        }

        String photoId = photoIds.get(position);

        isDeleting = true;
        btnDeletePhoto.setEnabled(false);
        btnDeletePhoto.setText("DELETING...");

        currentUser.getIdToken(false)
                .addOnSuccessListener(tokenResult -> {

                    String firebaseToken = tokenResult.getToken();

                    if (firebaseToken == null) {
                        resetDeleteButton();

                        Toast.makeText(
                                this,
                                "Could not authenticate user",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    executor.execute(() ->
                            sendDeleteRequest(firebaseToken, photoId)
                    );
                })
                .addOnFailureListener(error -> {

                    resetDeleteButton();

                    Toast.makeText(
                            this,
                            "Authentication failed: " + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Send the delete request to the trusted Supabase backend.
    private void sendDeleteRequest(String firebaseToken, String photoId) {

        HttpURLConnection connection = null;

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("eventId", eventId);
            requestBody.put("photoId", photoId);

            URL url = new URL(DELETE_FUNCTION_URL);

            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
            );
            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " + firebaseToken
            );

            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(
                        requestBody.toString()
                                .getBytes(StandardCharsets.UTF_8)
                );
            }

            int responseCode = connection.getResponseCode();

            InputStream responseStream =
                    responseCode >= 200 && responseCode < 300
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            String responseText = "";

            if (responseStream != null) {
                try (ByteArrayOutputStream buffer =
                             new ByteArrayOutputStream()) {

                    byte[] bytes = new byte[4096];
                    int count;

                    while ((count = responseStream.read(bytes)) != -1) {
                        buffer.write(bytes, 0, count);
                    }

                    responseText = buffer.toString("UTF-8");
                }
            }

            JSONObject responseJson = new JSONObject(responseText);
            boolean success = responseJson.optBoolean("success", false);

            if (responseCode >= 200 && responseCode < 300 && success) {

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    // Find by photo ID so swiping during deletion
                    // cannot cause the wrong photo to be removed.
                    int deletedPosition = photoIds.indexOf(photoId);

                    if (deletedPosition != -1) {
                        removePhotoFromViewer(deletedPosition);
                    } else {
                        resetDeleteButton();
                    }

                    Toast.makeText(
                            this,
                            "Photo deleted successfully",
                            Toast.LENGTH_SHORT
                    ).show();
                });

            } else {

                String errorMessage = responseJson.optString(
                        "error",
                        "Could not delete photo"
                );

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    resetDeleteButton();

                    Toast.makeText(
                            this,
                            errorMessage,
                            Toast.LENGTH_LONG
                    ).show();
                });
            }

        } catch (Exception error) {

            Log.e("KliqPhotoViewer", "Delete request failed", error);

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                resetDeleteButton();

                Toast.makeText(
                        this,
                        "Deletion failed: " + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            });

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // Remove the successfully deleted photo from the current viewer.
    private void removePhotoFromViewer(int position) {

        if (position < 0 || position >= photoUrls.size()) {
            resetDeleteButton();
            return;
        }

        photoUrls.remove(position);
        photoIds.remove(position);
        photoUploaderIds.remove(position);

        if (photoUrls.isEmpty()) {
            finish();
            return;
        }

        adapter.notifyDataSetChanged();

        int nextPosition = Math.min(
                position,
                photoUrls.size() - 1
        );

        photoViewPager.setCurrentItem(nextPosition, false);
        updatePhotoPosition(nextPosition);

        resetDeleteButton();
    }

    private void resetDeleteButton() {

        isDeleting = false;

        if (!isFinishing() && !isDestroyed()) {
            btnDeletePhoto.setEnabled(true);
            btnDeletePhoto.setText("DELETE PHOTO");
            updateDeleteButtonVisibility();
        }
    }

    private void updatePhotoPosition(int position) {

        tvPhotoPosition.setText(
                (position + 1) + " / " + photoUrls.size()
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
