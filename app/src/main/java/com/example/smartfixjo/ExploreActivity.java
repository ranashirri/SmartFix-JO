package com.example.smartfixjo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class ExploreActivity extends AppCompatActivity {

    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Required for OSMDroid
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_explore);

        // 1. Setup the Map
        map = findViewById(R.id.exploreMapView);
        map.setMultiTouchControls(true);

        // Center on Amman (Seventh Circle area)
        org.osmdroid.api.IMapController mapController = map.getController();
        mapController.setZoom(15.0);
        GeoPoint myLocation = new GeoPoint(31.957, 35.845);
        mapController.setCenter(myLocation);

        // Add User's Location Pin
        Marker userMarker = new Marker(map);
        userMarker.setPosition(myLocation);
        userMarker.setTitle("You are here");
        map.getOverlays().add(userMarker);

        // 2. Add Nearby Technicians!
        addTechnicianPin(31.959, 35.842, "Ahmed (Plumber)", "⭐ 4.8 - Available Now");
        addTechnicianPin(31.954, 35.848, "Sami (Electrician)", "⭐ 4.9 - 5 mins away");
        addTechnicianPin(31.958, 35.850, "Omar (AC Repair)", "⭐ 4.7 - Available Now");
        addTechnicianPin(31.960, 35.846, "Khalid (Pest Control)", "⭐ 5.0 - 10 mins away");

        // 3. Setup Bottom Navigation Bar
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_explore); // Assuming your middle icon ID is nav_explore

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) { // Change ID to match your home icon's ID in bottom_nav_menu.xml
                startActivity(new Intent(ExploreActivity.this, HomeActivity.class));
                overridePendingTransition(0, 0); // Removes the annoying sliding animation
                finish();
                return true;
            } else if (itemId == R.id.nav_explore) {
                return true; // We are already here!
            }
            return false;
        });
    }

    // Helper Method to drop pins quickly
    private void addTechnicianPin(double lat, double lng, String name, String details) {
        Marker techMarker = new Marker(map);
        techMarker.setPosition(new GeoPoint(lat, lng));
        techMarker.setTitle(name);
        techMarker.setSnippet(details);

        // Make the pin look like a wrench/person instead of a default marker
        techMarker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_myplaces));

        // When the user taps the info bubble above the pin
        techMarker.setOnMarkerClickListener((marker, mapView) -> {
            marker.showInfoWindow(); // Show the name and details
            Toast.makeText(ExploreActivity.this, "Selected: " + marker.getTitle(), Toast.LENGTH_SHORT).show();
            // TODO: Later, we can make this open the BookingActivity directly!
            return true;
        });

        map.getOverlays().add(techMarker);
    }

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
}