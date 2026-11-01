
package com.example.kliq;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    private RecyclerView recyclerPhotos;
    private PhotoAdapter photoAdapter;
    private List<Photo> photoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        photoList = new ArrayList<>();
        photoList.add(new Photo("1", null, null, null,
                "file:///android_res/drawable/photo1.jpg", null));

        photoList.add(new Photo("2", null, null, null,
                "file:///android_res/drawable/photo2.webp", null));

        photoList.add(new Photo("3", null, null, null,
                "file:///android_res/drawable/photo3.jpg", null));

        photoList.add(new Photo("4", null, null, null,
                "file:///android_res/drawable/photo4.jpg", null));

        photoList.add(new Photo("5", null, null, null,
                "file:///android_res/drawable/photo5.jpg", null));

        photoList.add(new Photo("6", null, null, null,
                "file:///android_res/drawable/photo6.jpg", null));
        // Display photos in three columns
        recyclerPhotos.setLayoutManager(
                new GridLayoutManager(this, 3));

        photoAdapter = new PhotoAdapter(this, photoList);

        recyclerPhotos.setAdapter(photoAdapter);
    }
}
