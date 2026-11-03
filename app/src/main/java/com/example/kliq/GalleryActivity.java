
package com.example.kliq;
import android.content.Intent;
import android.util.Base64;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import org.json.JSONException;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class GalleryActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String SUPABASE_FUNCTION_URL =
            "https://eujrmgtijfntomumwhjy.supabase.co/functions/v1/kliq-photo-upload";
    private static final String SUPABASE_VIEW_URL =
            "https://eujrmgtijfntomumwhjy.supabase.co/functions/v1/kliq-photo-view";
    private RecyclerView recyclerPhotos;
    private PhotoAdapter photoAdapter;
    private List<Photo> photoList;
    private Button btnUpload;
    private TextView tvPhotoCount;
    private ActivityResultLauncher<String> photoPicker;
    private Uri pendingPhotoUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        photoPicker = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        pendingPhotoUri = uris.get(0);
                        getFirebaseIdToken();

                        int count = 0;

                        for (Uri uri : uris) {

                            Photo photo = new Photo(
                                    "local_" + System.currentTimeMillis() + "_" + count,
                                    null,
                                    null,
                                    null,
                                    uri.toString(),
                                    null
                            );

                            photoList.add(photo);
                            loadPhotoImage(photo);
                            count++;
                        }

                        photoAdapter.notifyDataSetChanged();

                        Toast.makeText(
                                this,
                                count + " photos selected",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_gallery);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                });

        recyclerPhotos = findViewById(R.id.recyclerPhotos);
        btnUpload = findViewById(R.id.btnUpload);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);

        btnUpload.setOnClickListener(v -> {
            photoPicker.launch("image/*");
        });
        String eventId = getIntent().getStringExtra("eventId");

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this,
                    "Event ID is missing",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        photoList = new ArrayList<>();

        // Display photos in three columns
        recyclerPhotos.setLayoutManager(
                new GridLayoutManager(this, 3));

        photoAdapter = new PhotoAdapter(this, photoList, position -> {
            ArrayList<String> urls = new ArrayList<>();
            int selectedIndex = -1;

            for (int i = 0; i < photoList.size(); i++) {
                String url = photoList.get(i).getImageUrl();

                if (url != null && !url.trim().isEmpty()) {
                    if (i == position) {
                        selectedIndex = urls.size();
                    }
                    urls.add(url);
                }
            }

            if (selectedIndex == -1) {
                Toast.makeText(this, "Photo is still loading. Please try again.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(GalleryActivity.this, PhotoViewerActivity.class);
            intent.putStringArrayListExtra("photoUrls", urls);
            intent.putExtra("startPosition", selectedIndex);
            startActivity(intent);
        });

        recyclerPhotos.setAdapter(photoAdapter);
        loadPhotos(eventId);

    }
    private void getFirebaseIdToken() {

        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUser.getIdToken(false)
                .addOnSuccessListener(result -> {

                    String firebaseToken = result.getToken();
                    if (firebaseToken != null) {
                        uploadPhoto(pendingPhotoUri, firebaseToken);
                    }
                    else {
                        Toast.makeText(
                                this,
                                "Could not get Firebase token",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Authentication error: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }
    private void uploadPhoto(Uri imageUri, String firebaseToken) {
        executor.execute(() -> {
            try {
                String contentType = getContentResolver().getType(imageUri);

                if (contentType == null ||
                        !(contentType.equals("image/jpeg") ||
                                contentType.equals("image/png") ||
                                contentType.equals("image/webp"))) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Unsupported image format", Toast.LENGTH_LONG).show());
                    return;
                }

                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    throw new Exception("Could not read selected photo");
                }

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                inputStream.close();

                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                String imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("eventId", getIntent().getStringExtra("eventId"));
                jsonBody.put("contentType", contentType);
                jsonBody.put("imageBase64", imageBase64);
                URL url = new URL(SUPABASE_FUNCTION_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + firebaseToken);
                connection.setDoOutput(true);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
                }
                int responseCode = connection.getResponseCode();

                InputStream responseStream = responseCode >= 200 && responseCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
                byte[] responseBytes = new byte[4096];
                int responseLength;

                while ((responseLength = responseStream.read(responseBytes)) != -1) {
                    responseBuffer.write(responseBytes, 0, responseLength);
                }

                responseStream.close();

                String responseText = responseBuffer.toString("UTF-8");
                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject responseJson = new JSONObject(responseText);

                    boolean success = responseJson.optBoolean("success", false);

                    if (!success) {
                        throw new Exception("Supabase upload was not successful");
                    }

                    String photoId = responseJson.getString("photoId");
                    String storagePath = responseJson.getString("storagePath");
                    Map<String, Object> photoData = new HashMap<>();
                    photoData.put("uploadedBy", currentUser.getUid());
                    photoData.put("storagePath", storagePath);
                    photoData.put("uploadedAt", FieldValue.serverTimestamp());

                    String eventId = getIntent().getStringExtra("eventId");

                    FirebaseFirestore.getInstance()
                            .collection("events")
                            .document(eventId)
                            .collection("photos")
                            .document(photoId)
                            .set(photoData)
                            .addOnSuccessListener(unused ->
                                    runOnUiThread(() ->
                                            Toast.makeText(this, "Photo uploaded successfully!",
                                                    Toast.LENGTH_SHORT).show()))
                            .addOnFailureListener(error ->
                                    runOnUiThread(() ->
                                            Toast.makeText(this, "Photo uploaded, but metadata saving failed: "
                                                    + error.getMessage(), Toast.LENGTH_LONG).show()));
                } else {
                    throw new Exception("Upload failed: " + responseText);
                }
                connection.disconnect();
                if (imageBytes.length > 10 * 1024 * 1024) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Photo must be smaller than 10 MB", Toast.LENGTH_LONG).show());
                    return;
                }
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Could not read photo: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        });

    }
    private void loadPhotos(String eventId) {

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("photos")
                .orderBy("uploadedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Log.e(
                                "KliqGallery",
                                "Could not load photo metadata",
                                error
                        );

                        Toast.makeText(
                                this,
                                "Could not load photos: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    if (snapshots == null || isFinishing() || isDestroyed()) {
                        return;
                    }

                    photoList.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot document
                            : snapshots.getDocuments()) {

                        String storagePath = document.getString("storagePath");

                        Photo photo = new Photo(
                                document.getId(),
                                document.getString("uploadedBy"),
                                null,
                                storagePath,
                                null,
                                document.getTimestamp("uploadedAt")
                        );

                        photoList.add(photo);

                        // IMPORTANT: Request the actual image URL from Supabase.
                        loadPhotoImage(photo);

                        Log.d(
                                "KliqGallery",
                                "Photo found: id=" + document.getId()
                                        + ", storagePath=" + storagePath
                        );
                    }

                    photoAdapter.notifyDataSetChanged();
                    int count = photoList.size();

                    tvPhotoCount.setText(
                            count + (count == 1 ? " photo" : " photos")
                    );

                    Log.d(
                            "KliqGallery",
                            "Photos loaded from Firestore: "
                                    + photoList.size()
                    );
                });
    }
    private void loadPhotoImage(Photo photo) {
        if (currentUser == null) {
            Log.e("KliqGallery", "No authenticated user");
            return;
        }

        currentUser.getIdToken(false)
                .addOnSuccessListener(tokenResult -> {
                    String firebaseToken = tokenResult.getToken();

                    if (firebaseToken == null) {
                        Log.e("KliqGallery", "Firebase token is null");
                        return;
                    }

                    executor.execute(() -> {
                        HttpURLConnection connection = null;

                        try {
                            JSONObject requestBody = new JSONObject();
                            requestBody.put(
                                    "eventId",
                                    getIntent().getStringExtra("eventId")
                            );
                            requestBody.put("photoId", photo.getPhotoId());

                            URL url = new URL(SUPABASE_VIEW_URL);
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

                            try (OutputStream outputStream =
                                         connection.getOutputStream()) {
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

                            Log.d(
                                    "KliqGallery",
                                    "View response code: " + responseCode
                            );
                            Log.d(
                                    "KliqGallery",
                                    "View response body: " + responseText
                            );

                            if (responseCode >= 200 && responseCode < 300) {
                                JSONObject responseJson =
                                        new JSONObject(responseText);

                                String imageUrl =
                                        responseJson.optString("imageUrl", "");

                                if (imageUrl.trim().isEmpty()
                                        || imageUrl.equals("null")) {
                                    Log.e(
                                            "KliqGallery",
                                            "Response did not contain a valid imageUrl"
                                    );
                                    return;
                                }

                                Log.d(
                                        "KliqGallery",
                                        "Image URL received: " + imageUrl
                                );

                                photo.setImageUrl(imageUrl);

                                runOnUiThread(() -> {
                                    if (!isFinishing() && !isDestroyed()) {
                                        photoAdapter.notifyDataSetChanged();
                                    }
                                });

                            } else {
                                Log.e(
                                        "KliqGallery",
                                        "Photo view request failed: " + responseText
                                );
                            }

                        } catch (Exception e) {
                            Log.e(
                                    "KliqGallery",
                                    "Could not prepare photo request",
                                    e
                            );
                        } finally {
                            if (connection != null) {
                                connection.disconnect();
                            }
                        }
                    });
                })
                .addOnFailureListener(error ->
                        Log.e(
                                "KliqGallery",
                                "Could not get Firebase token",
                                error
                        ));
    }
}
