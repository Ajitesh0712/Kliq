package com.example.kliq;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;

public class PhotoViewerActivity extends AppCompatActivity {

    private ViewPager2 photoViewPager;
    private TextView tvPhotoPosition;
    private ArrayList<String> photoUrls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);

        photoViewPager = findViewById(R.id.photoViewPager);
        tvPhotoPosition = findViewById(R.id.tvPhotoPosition);

        photoUrls = getIntent().getStringArrayListExtra("photoUrls");

        if (photoUrls == null || photoUrls.isEmpty()) {
            finish();
            return;
        }

        PhotoViewerAdapter adapter =
                new PhotoViewerAdapter(this, photoUrls);

        photoViewPager.setAdapter(adapter);

        int startPosition = getIntent().getIntExtra("startPosition", 0);
        startPosition = Math.max(0, Math.min(startPosition, photoUrls.size() - 1));

        photoViewPager.setCurrentItem(startPosition, false);
        updatePhotoPosition(startPosition);

        photoViewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        updatePhotoPosition(position);
                    }
                }
        );
    }

    private void updatePhotoPosition(int position) {
        tvPhotoPosition.setText(
                (position + 1) + " / " + photoUrls.size()
        );
    }
}