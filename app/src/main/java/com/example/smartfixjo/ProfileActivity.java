package com.example.smartfixjo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Mock Logout Button
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v ->
                Toast.makeText(ProfileActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        );

        // Setup Bottom Navigation Bar
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_profile); // Highlight the Profile icon

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(ProfileActivity.this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_explore) {
                startActivity(new Intent(ProfileActivity.this, ExploreActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                return true; // We are already here
            }
            return false;
        });
    }
}