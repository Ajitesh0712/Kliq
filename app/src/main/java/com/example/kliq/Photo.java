
package com.example.kliq;

import com.google.firebase.Timestamp;

public class Photo {

    private String photoId;
    private String uploadedBy;
    private String uploaderName;
    private String storagePath;
    private String imageUrl;
    private Timestamp uploadedAt;
    public Photo() {
    }

    public Photo(String photoId, String uploadedBy,
                 String uploaderName, String storagePath,
                 String imageUrl, Timestamp uploadedAt) {

        this.photoId = photoId;
        this.uploadedBy = uploadedBy;
        this.uploaderName = uploaderName;
        this.storagePath = storagePath;
        this.imageUrl = imageUrl;
        this.uploadedAt = uploadedAt;
    }

    public String getPhotoId() {
        return photoId;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setUploadedAt(Timestamp uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
