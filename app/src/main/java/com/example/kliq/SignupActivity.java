package com.example.kliq;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    EditText name, email, password, confirmPassword;
    Button signupButton;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        signupButton = findViewById(R.id.signupButton);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        signupButton.setOnClickListener(v -> {

            String nameText = name.getText().toString().trim();
            String emailText = email.getText().toString().trim();
            String passwordText = password.getText().toString().trim();
            String confirmPasswordText =
                    confirmPassword.getText().toString().trim();

            if (nameText.isEmpty()) {
                name.setError("Enter your name");
                return;
            }

            if (emailText.isEmpty()) {
                email.setError("Enter email");
                return;
            }

            if (passwordText.isEmpty()) {
                password.setError("Enter password");
                return;
            }

            if (passwordText.length() < 6) {
                password.setError("Password must be at least 6 characters");
                return;
            }

            if (!passwordText.equals(confirmPasswordText)) {
                confirmPassword.setError("Passwords do not match");
                return;
            }

            signupButton.setEnabled(false);

            mAuth.createUserWithEmailAndPassword(
                    emailText,
                    passwordText
            ).addOnCompleteListener(this, task -> {

                if (task.isSuccessful()) {

                    String uid = mAuth.getCurrentUser().getUid();

                    Map<String, Object> user = new HashMap<>();
                    user.put("name", nameText);
                    user.put("email", emailText);
                    user.put("createdAt", FieldValue.serverTimestamp());

                    db.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(
                                        SignupActivity.this,
                                        "Account and profile created!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                Intent intent = new Intent(
                                        SignupActivity.this,
                                        HomeActivity.class
                                );

                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {

                                Toast.makeText(
                                        SignupActivity.this,
                                        "Account created, but profile saving failed: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();

                                signupButton.setEnabled(true);
                            });

                } else {

                    Toast.makeText(
                            SignupActivity.this,
                            "Signup failed: "
                                    + task.getException().getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                    signupButton.setEnabled(true);
                }
            });
        });
    }
}
