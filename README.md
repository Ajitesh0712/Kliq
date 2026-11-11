# KLIQ
### One Event. Everyone's Memories. One Shared Gallery.

KLIQ is an Android application designed to simplify photo sharing at events. Users can create or join event rooms using a unique invite code or QR code, upload photos, and access a centralized gallery where all event participants can view and download shared memories.

KLIQ eliminates the need to collect photos individually from participants by bringing all event memories together in one shared space.

---

## Overview

KLIQ provides an event-based photo-sharing platform with secure authentication, event management, and centralized photo storage.

The application supports two roles: Admin and Member. Event administrators can manage shared photos, while members can upload, view, and manage their own contributions.

## Features

### Authentication
- User registration and login using Firebase Authentication.
- Secure user authentication.
- Persistent login sessions.

### Event Management
- Create private event rooms.
- Generate unique invite codes.
- Join events using invite codes.
- QR-code-based event joining.
- View and manage joined events.
- Admin and member role management.

### Shared Photo Gallery
- Upload photos from the device gallery.
- Access a centralized event gallery.
- View photos uploaded by other participants.
- Full-screen photo viewer.
- Swipe between photos.
- Download shared photos.

### Access Control and Security
- Event-based membership permissions.
- Only authenticated event members can access event photo metadata.
- Admins can delete photos uploaded by any event member.
- Members can delete their own uploaded photos.
- Private image storage using Supabase Storage.
- Backend-controlled photo upload, viewing, and deletion.

---

## Technology Stack

| Technology | Purpose |
|---|---|
| Java | Android application development |
| XML | User interface development |
| Android Studio | Development environment |
| Firebase Authentication | User authentication |
| Cloud Firestore | Database and event management |
| Supabase Storage | Private photo storage |
| Supabase Edge Functions | Backend operations |
| ZXing | QR code generation and scanning |
| Glide | Image loading and caching |
| ViewPager2 | Swipeable photo viewer |

---

## System Architecture

KLIQ uses Firebase for authentication and event metadata, while Supabase handles private image storage and backend operations.

```text
                  KLIQ ANDROID APP
                         |
              +----------+----------+
              |                     |
       Firebase Services       Supabase Backend
              |                     |
        +-----+------+        +-----+------+
        |            |        |            |
   Firebase      Cloud     Edge         Supabase
   Auth          Firestore Functions    Storage
        |            |        |            |
   Authentication Events   Backend      Private
   and Sessions   Metadata Operations   Photo Files
```

### Photo Upload Workflow

1. The user selects a photo.
2. The application authenticates the user.
3. The backend verifies event membership.
4. The image is uploaded to private Supabase Storage.
5. Photo metadata is stored in Cloud Firestore.

### Photo Viewing Workflow

1. The application retrieves photo metadata from Firestore.
2. The backend verifies the Firebase authentication token and event membership.
3. A temporary signed URL is generated.
4. The image is displayed in the application.

### Photo Deletion Workflow

1. The user requests photo deletion.
2. The backend verifies the user's identity and event membership.
3. Administrators can delete any event photo, while regular members can delete only their own photos.
4. The image is removed from Supabase Storage.
5. Its metadata is deleted from Firestore.

---

## Project Structure

```text
Kliq/
|
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/kliq/
│   │   │   │   ├── SplashActivity.java
│   │   │   │   ├── LoginActivity.java
│   │   │   │   ├── SignupActivity.java
│   │   │   │   ├── HomeActivity.java
│   │   │   │   ├── CreateEventActivity.java
│   │   │   │   ├── JoinEventActivity.java
│   │   │   │   ├── MyEventActivity.java
│   │   │   │   ├── EventDetailsActivity.java
│   │   │   │   ├── GalleryActivity.java
│   │   │   │   ├── PhotoViewerActivity.java
│   │   │   │   ├── Photo.java
│   │   │   │   └── Adapters/
│   │   │   │
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   ├── test/
│   │   └── androidTest/
│   │
│   └── build.gradle.kts
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## Getting Started

### Prerequisites

- Android Studio
- JDK 17 or a compatible Gradle JDK
- Android SDK
- Firebase account
- Supabase account

### 1. Clone the Repository

```bash
git clone https://github.com/Ajitesh0712/Kliq.git
cd Kliq
```

### 2. Open the Project

Open the cloned repository in Android Studio and allow Gradle to synchronize the project dependencies.

### 3. Configure Firebase

1. Create a Firebase project.
2. Register an Android application with the package name `com.example.kliq`.
3. Enable Firebase Authentication.
4. Configure Cloud Firestore.
5. Download the `google-services.json` configuration file.
6. Place the file inside the `app/` directory.

### 4. Configure Supabase

1. Create a Supabase project.
2. Create a private Storage bucket named `kliq-photos`.
3. Deploy the following Edge Functions:
   - `kliq-photo-upload`
   - `kliq-photo-view`
   - `kliq-photo-delete`
4. Configure the required backend environment secrets:

   - `FIREBASE_PROJECT_ID`
   - `FIREBASE_WEB_API_KEY`
   - `FIREBASE_SERVICE_ACCOUNT_JSON`
   - `SUPA_SECRET_KEY`

5. Configure the Supabase project URL.

Store all backend credentials securely using Supabase environment secrets. Never commit service-account credentials or private API keys to the repository.

### 5. Run the Application

1. Connect an Android device or start an emulator.
2. Synchronize the Gradle project.
3. Build and run the application using Android Studio.

---

## Security

KLIQ implements multiple layers of access control:

- Firebase Authentication for user identity.
- Firestore Security Rules for event and membership permissions.
- Private Supabase Storage for image files.
- Supabase Edge Functions for authenticated backend operations.
- Temporary signed URLs for photo access.
- Server-side authorization for photo deletion.

---

## Future Enhancements

- Face detection and recognition for personalized photo albums.
- Automatic photo organization by participants.
- Push notifications for new event uploads.
- Improved photo filtering and sorting.
- iOS application support.
- Enhanced event discovery and management.

---

Contributors

Ajitesh Rajput
Ananya Soni

GitHub: [@Ajitesh0712](https://github.com/Ajitesh0712)
GitHub: [@Ananya-1009](https://github.com/Ananya-1009)

---

License

This project is currently intended for educational and portfolio purposes. A formal open-source license can be added separately.

---

KLIQ — One Event. Everyone's Memories. One Shared Gallery.
