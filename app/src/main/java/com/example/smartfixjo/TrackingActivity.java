package com.example.smartfixjo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// The New Map Imports
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class TrackingActivity extends AppCompatActivity {

    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. MUST do this before loading the layout! Tells the map to behave normally.
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_tracking);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        // Link UI Elements
        TextView tvSheetTitle = findViewById(R.id.tvSheetTitle);
        LinearLayout techDetailsLayout = findViewById(R.id.techDetailsLayout);
        Button btnCallTech = findViewById(R.id.btnCallTech);
        Button btnCancelSearch = findViewById(R.id.btnCancelSearch);

        // 2. Setup the Map!
        map = findViewById(R.id.mapView);
        map.setMultiTouchControls(true); // Lets you pinch to zoom

        // Set the map center to Amman, Jordan (7th Circle Area)
        org.osmdroid.api.IMapController mapController = map.getController();
        mapController.setZoom(16.0);
        GeoPoint ammanLocation = new GeoPoint(31.957, 35.845); // Coordinates for Amman!
        mapController.setCenter(ammanLocation);

        // Put a red pin on the map
        Marker userMarker = new Marker(map);
        userMarker.setPosition(ammanLocation);
        userMarker.setTitle("Your Location");
        map.getOverlays().add(userMarker);


        // 3. The Match Logic (Still keeping our 4-second delay!)
        String incomingCategory = getIntent().getStringExtra("EXTRA_CATEGORY");
        final String category = (incomingCategory != null) ? incomingCategory : "Technician";

        // THE TIMELINE SIMULATOR
        Handler timelineHandler = new Handler(Looper.getMainLooper());

        // Event 1: Match Found (4 seconds)
        timelineHandler.postDelayed(() -> {
            tvSheetTitle.setText(category + " Expert Found!");
            techDetailsLayout.setVisibility(View.VISIBLE);
            btnCallTech.setVisibility(View.VISIBLE);
            btnCancelSearch.setText("CANCEL JOB");

            // 🔔 SEND NOTIFICATION 1
            sendPushNotification("Expert Found!", "Mahmoud is assigned to your " + category + " issue.", 1);
        }, 4000);

        // Event 2: Technician Arrives (10 seconds)
        timelineHandler.postDelayed(() -> {
            tvSheetTitle.setText("Technician is at your door!");

            // 🔔 SEND NOTIFICATION 2
            sendPushNotification("Arrived 📍", "Mahmoud has arrived at your location.", 2);
        }, 10000);

        // Event 3: Job Done - Ask for Rating (15 seconds)
        timelineHandler.postDelayed(() -> {
            tvSheetTitle.setText("Job Completed!");
            btnCallTech.setVisibility(View.GONE);
            btnCancelSearch.setText("RATE & PAY");
            btnCancelSearch.setTextColor(getResources().getColor(R.color.white));
            btnCancelSearch.setBackgroundTintList(getResources().getColorStateList(R.color.brand_dark_blue));

            // 🔔 SEND NOTIFICATION 3
            sendPushNotification("Job Complete ✅", "How did we do? Tap to rate and enter payment details.", 3);

            // Change the button to open a new Rating screen
            btnCancelSearch.setOnClickListener(v -> {
                Intent intent = new Intent(TrackingActivity.this, ReviewActivity.class);
                startActivity(intent);
                finish(); // Close the map screen
            });

        }, 15000);
        // Button Clicks
        btnCallTech.setOnClickListener(v -> Toast.makeText(this, "Calling Mahmoud...", Toast.LENGTH_SHORT).show());
        btnCancelSearch.setOnClickListener(v -> finish());
    }

    // Required for the map to not drain battery when app is closed
    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }

    // THE NOTIFICATION ENGINE
    private void sendPushNotification(String title, String message, int notificationId) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8.0+ requires a "Channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "SMARTFIX_UPDATES",
                    "Job Status Updates",
                    NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        // Build the actual notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "SMARTFIX_UPDATES")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Default icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[]{0, 500, 200, 500}) // Makes the phone buzz!
                .setAutoCancel(true);

        // Send it! (Only if the user gave permission)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(notificationId, builder.build());
        }
    }
}