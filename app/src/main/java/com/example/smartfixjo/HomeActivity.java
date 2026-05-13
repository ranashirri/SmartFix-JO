package com.example.smartfixjo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// The Firebase & Vertex AI Imports
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.vertexai.FirebaseVertexAI; // <- This was missing earlier!
import com.google.firebase.vertexai.GenerativeModel;
import com.google.firebase.vertexai.java.GenerativeModelFutures;
import com.google.firebase.vertexai.type.Content;
import com.google.firebase.vertexai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private EditText etProblemDescription;
    private Button btnEmergency;

    private GenerativeModelFutures generativeModel;
    private Executor executor;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvLocation = findViewById(R.id.tvLocation);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 1. Prepare the Permission Popup
        ActivityResultLauncher<String[]> locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    if (fineLocationGranted != null && fineLocationGranted) {
                        fetchRealLocation();
                    } else {
                        Toast.makeText(this, "Location permission is required!", Toast.LENGTH_SHORT).show();
                    }
                });

        // 2. Make the button trigger the popup
        findViewById(R.id.btnDetectLocation).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fetchRealLocation(); // Already have permission!
            } else {
                // Ask the user for permission
                locationPermissionRequest.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });

        // Wake up Firebase
        FirebaseApp.initializeApp(this);

        // Link the screen to the code
        etProblemDescription = findViewById(R.id.etProblemDescription);

        findViewById(R.id.cardPlumbing).setOnClickListener(v -> routeDirectlyToBooking("Plumbing"));
        findViewById(R.id.cardElectrical).setOnClickListener(v -> routeDirectlyToBooking("Electrical"));
        findViewById(R.id.cardAC).setOnClickListener(v -> routeDirectlyToBooking("AC Repair"));
        findViewById(R.id.cardCarpentry).setOnClickListener(v -> routeDirectlyToBooking("Carpentry"));
        findViewById(R.id.cardAppliances).setOnClickListener(v -> routeDirectlyToBooking("Appliances"));
        findViewById(R.id.cardPestControl).setOnClickListener(v -> routeDirectlyToBooking("Pest Control"));

        // Initialize the Gemini AI
        GenerativeModel gm = FirebaseVertexAI.getInstance().generativeModel("gemini-2.5-flash");
        generativeModel = GenerativeModelFutures.from(gm);
        executor = Executors.newSingleThreadExecutor();

        // Listen for the user hitting "Enter" on the keyboard
        etProblemDescription.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String userInput = etProblemDescription.getText().toString().trim();
                if (!userInput.isEmpty()) {
                    Toast.makeText(HomeActivity.this, "AI is thinking...", Toast.LENGTH_SHORT).show();
                    analyzeProblemWithAI(userInput);
                }
                return true;
            }
            return false;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_home); // Assuming your first icon is nav_home

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_explore) { // Assuming your middle icon is nav_explore
                startActivity(new Intent(HomeActivity.this, ExploreActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_home) {
                return true; // Already here
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

    }

    // The Triage Engine: Sending text to Gemini
    private void analyzeProblemWithAI(String userText) {
        String prompt = "You are a maintenance dispatcher in Amman, Jordan. " +
                "Analyze the following text from a user describing a broken item: '" + userText + "'\n\n" +
                "You must reply ONLY with a JSON object containing three keys:\n" +
                "1. 'category': Must be exactly one of [Plumbing, Electrical, AC Repair, Carpentry, Appliances, Pest Control].\n" +
                "2. 'urgency': Must be 'Low', 'Medium', 'High', or 'Emergency'.\n" +
                "3. 'short_title': A 3-word summary of the issue.\n\n" +
                "Understand Jordanian slang (e.g. 'كيزر' is water heater, 'مكيف' is AC). DO NOT add formatting blocks like ```json.";

        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = generativeModel.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String aiDiagnosis = result.getText();
                Log.d("SMARTFIX_AI", "Raw Result: " + aiDiagnosis);

                // Run UI updates on the main thread
                runOnUiThread(() -> {
                    try {
                        // 1. Clean the text (Sometimes the AI adds markdown blocks, this removes them)
                        String cleanJson = aiDiagnosis.replace("```json", "").replace("```", "").trim();

                        // 2. Parse the JSON string into actual Java objects
                        org.json.JSONObject jsonObject = new org.json.JSONObject(cleanJson);
                        String category = jsonObject.getString("category");
                        String urgency = jsonObject.getString("urgency");
                        String title = jsonObject.getString("short_title");

                        // 3. Create a beautiful Popup Alert instead of a Toast
                        new androidx.appcompat.app.AlertDialog.Builder(HomeActivity.this)
                                .setTitle("🛠️ AI Diagnosis Complete")
                                .setMessage("Issue: " + title + "\n\nCategory: " + category + "\nUrgency: " + urgency)
                                .setPositiveButton("Dispatch " + category + " Tech", (dialog, which) -> {
                                    // 1. Create the Intent (The delivery truck)
                                    android.content.Intent intent = new android.content.Intent(HomeActivity.this, BookingActivity.class);

                                    // 2. Load the truck with the AI's data
                                    intent.putExtra("EXTRA_CATEGORY", category);
                                    intent.putExtra("EXTRA_URGENCY", urgency);
                                    intent.putExtra("EXTRA_TITLE", title);

                                    // 3. Drive to the new screen!
                                    startActivity(intent);
                                })
                                .setNegativeButton("Cancel", null)
                                .setCancelable(false)
                                .show();

                    } catch (Exception e) {
                        Log.e("SMARTFIX_AI", "JSON Parsing Error", e);
                        Toast.makeText(HomeActivity.this, "AI responded, but couldn't read format.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("SMARTFIX_AI", "AI Error", t);
                runOnUiThread(() -> Toast.makeText(HomeActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }, executor);
    }
    // FAST TRACK ROUTING: Skips the AI and goes straight to dispatch
    private void routeDirectlyToBooking(String selectedCategory) {
        android.content.Intent intent = new android.content.Intent(HomeActivity.this, BookingActivity.class);

        // Load the delivery truck with default fast-track data
        intent.putExtra("EXTRA_CATEGORY", selectedCategory);
        intent.putExtra("EXTRA_URGENCY", "Standard"); // Default urgency for manual clicks
        intent.putExtra("EXTRA_TITLE", selectedCategory + " Service Request");

        // Drive to the new screen!
        startActivity(intent);
    }
    @SuppressLint("MissingPermission")
    private void fetchRealLocation() {
        tvLocation.setText("Locating satellite...");

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                try {
                    // Turn coordinates into a real street name!
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                    if (addresses != null && !addresses.isEmpty()) {
                        String realAddress = addresses.get(0).getAddressLine(0);
                        tvLocation.setText(realAddress);
                    } else {
                        tvLocation.setText("Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());
                    }
                } catch (Exception e) {
                    tvLocation.setText("Amman, Jordan (GPS Default)");
                }
            } else {
                Toast.makeText(this, "Make sure your phone's GPS is turned on!", Toast.LENGTH_LONG).show();
                tvLocation.setText("Location not found");
            }
        });
    }

}