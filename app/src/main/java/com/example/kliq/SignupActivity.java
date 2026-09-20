package com.example.kliq;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SignupActivity extends AppCompatActivity {

    EditText name, email, password, confirmPassword;
    Button signupButton;

    FirebaseAuth mAuth;

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

            if (!passwordText.equals(confirmPasswordText)) {
                confirmPassword.setError("Passwords do not match");
                return;
            }

            mAuth.createUserWithEmailAndPassword(
                    emailText,
                    passwordText
            ).addOnCompleteListener(this, task -> {

                if (task.isSuccessful()) {

                    Toast.makeText(
                            SignupActivity.this,
                            "Account created!",
                            Toast.LENGTH_SHORT
                    ).show();

                } else {

                    Toast.makeText(
                            SignupActivity.this,
                            "Signup failed: "
                                    + task.getException().getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        });
    }
}