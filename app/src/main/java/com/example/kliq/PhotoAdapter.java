package com.example.kliq;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    private Context context;
    private List<Photo> photoList;
    public PhotoAdapter(Context context, List<Photo> photoList) {
        this.context = context;
        this.photoList = photoList;
    }
    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_photo, parent, false);

        return new PhotoViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {

        Photo photo = photoList.get(position);

        Glide.with(context)
                .load(photo.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
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
