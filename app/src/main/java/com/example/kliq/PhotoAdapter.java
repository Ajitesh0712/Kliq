package com.example.kliq;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private static final String TAG = "KliqGallery";

    private final Context context;
    private final List<Photo> photoList;

    public PhotoAdapter(Context context, List<Photo> photoList) {
        this.context = context;
        this.photoList = photoList;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_photo, parent, false);

        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull PhotoViewHolder holder,
            int position
    ) {
        Photo photo = photoList.get(position);
        String imageUrl = photo.getImageUrl();

        Log.d(TAG, "Binding photo at position " + position);
        Log.d(TAG, "Image URL: " + imageUrl);

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            Log.e(TAG, "Image URL is null or empty at position " + position);

            Glide.with(context).clear(holder.imagePhoto);
            holder.imagePhoto.setImageResource(
                    android.R.drawable.ic_menu_report_image
            );
            return;
        }

        Glide.with(context)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .listener(new RequestListener<android.graphics.drawable.Drawable>() {

                    @Override
                    public boolean onLoadFailed(
                            GlideException e,
                            Object model,
                            Target<android.graphics.drawable.Drawable> target,
                            boolean isFirstResource
                    ) {
                        Log.e(TAG, "Glide failed to load: " + model, e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(
                            android.graphics.drawable.Drawable resource,
                            Object model,
                            Target<android.graphics.drawable.Drawable> target,
                            DataSource dataSource,
                            boolean isFirstResource
                    ) {
                        Log.d(TAG, "Image loaded successfully: " + model);
                        return false;
                    }
                })
                .into(holder.imagePhoto);
    }

    @Override
    public int getItemCount() {
        return photoList.size();
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {

        ImageView imagePhoto;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePhoto = itemView.findViewById(R.id.imagePhoto);
        }
    }
}