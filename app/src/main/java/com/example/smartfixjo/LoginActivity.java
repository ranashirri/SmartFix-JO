package com.example.smartfixjo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvSignUp = findViewById(R.id.tvSignUp);

        // When they click Sign In, take them to the Home Screen!
        btnLogin.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Welcome back, Omar!", Toast.LENGTH_SHORT).show();

            // Go to HomeActivity
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);

            // "finish()" destroys the login screen so they can't hit the back button to return to it
            finish();
        });

        // Fake Sign Up click
        tvSignUp.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Opening Sign Up Page...", Toast.LENGTH_SHORT).show();
        });
    }
}