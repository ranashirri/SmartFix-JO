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
        TextView tvTechName = findViewById(R.id.tvTechName); // You'll need to add this ID to your XML!
        TextView tvTechDetails = findViewById(R.id.tvTechDetails);

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

// STEP 4: GENERATE THE RANDOM WORKER HERE!
// (We mark them 'final' so the timers below are allowed to use them)
        final String[] assignedTech = getRandomTechnician(category);
        final String techName = assignedTech[0];
        final String techRating = "⭐ " + assignedTech[1] + " • " + assignedTech[2];

// THE TIMELINE SIMULATOR
        Handler timelineHandler = new Handler(Looper.getMainLooper());

// Event 1: Match Found (4 seconds)
        timelineHandler.postDelayed(() -> {
            tvSheetTitle.setText(category + " Expert Found!");

            // UPDATE THE UI WITH THE RANDOM WORKER'S INFO
            tvTechName.setText(techName);
            tvTechDetails.setText(techRating);

            techDetailsLayout.setVisibility(View.VISIBLE);
            btnCallTech.setVisibility(View.VISIBLE);
            btnCancelSearch.setText("CANCEL JOB");

            // 🔔 SEND NOTIFICATION 1 (Notice how we replaced "Mahmoud" with techName!)
            sendPushNotification("Expert Found!", techName + " is assigned to your " + category + " issue.", 1);
        }, 4000);

        // Event 2: Technician Arrives (10 seconds)
        timelineHandler.postDelayed(() -> {
            tvSheetTitle.setText("Technician is at your door!");

            // 🔔 SEND NOTIFICATION 2
            sendPushNotification("Arrived 📍", techName + " has arrived at your location.", 2);
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
                // Catch the data from Booking and pass it to Review
                double estimatedPrice = getIntent().getDoubleExtra("EXTRA_ESTIMATE", 10.0);

                Intent intent = new Intent(TrackingActivity.this, ReviewActivity.class);
                intent.putExtra("EXTRA_CATEGORY", category);
                intent.putExtra("EXTRA_ESTIMATE", estimatedPrice);
                startActivity(intent);
                finish();
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
    private String[] getRandomTechnician(String category) {
        String[][] workers;

        // Load the right list based on the category
        if (category.equals("Plumbing")) {
            workers = new String[][]{{"Tariq Haddad", "4.9 (142 jobs)", "Toyota Hilux"}, {"Faisal Qasem", "4.6 (85 jobs)", "Ford Transit"}, {"Zaid Al-Masri", "4.8 (210 jobs)", "Mitsubishi L200"}};
        } else if (category.equals("Electrical")) {
            workers = new String[][]{{"Rami Naser", "4.7 (93 jobs)", "Nissan Sunny"}, {"Youssef Ali", "5.0 (34 jobs)", "Kia Rio"}, {"Bilal Yasin", "4.8 (176 jobs)", "Hyundai Elantra"}};
        } else if (category.equals("AC Repair")) {
            workers = new String[][]{{"Kareem Salem", "4.9 (300 jobs)", "Isuzu D-Max"}, {"Samer Issa", "4.5 (42 jobs)", "Peugeot Partner"}};
        } else if (category.equals("Carpentry")) {
            workers = new String[][]{{"Hassan Dawoud", "4.8 (155 jobs)", "Nissan Navara"}, {"Omar Jaber", "4.9 (204 jobs)", "Ford Ranger"}};
        } else if (category.equals("Appliances")) {
            workers = new String[][]{{"Ahmad Zaqzouq", "4.6 (67 jobs)", "Chevrolet Spark"}, {"Tamer Suleiman", "4.9 (188 jobs)", "Kia Cerato"}};
        } else if (category.equals("Pest Control")) {
            workers = new String[][]{{"Fadi Khoury", "4.9 (215 jobs)", "VW Caddy"}, {"Khaled Mansour", "4.7 (90 jobs)", "Renault Kangoo"}};
        } else {
            // Default fallback
            workers = new String[][]{{"Mahmoud A.", "4.9 (120 jobs)", "Honda Civic"}};
        }

        // Pick a completely random worker from the chosen list!
        int randomIndex = new java.util.Random().nextInt(workers.length);
        return workers[randomIndex];
    }
}