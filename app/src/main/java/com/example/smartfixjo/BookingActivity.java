package com.example.smartfixjo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {

    private String category;
    private double basePrice = 5.0; // 5 JD base callout fee
    private double currentTotal = 5.0;

    private TextView tvCategoryTitle, tvQuestion1, tvQuestion2, tvQuestion3, tvTotalPrice;
    private Spinner spinner1, spinner2, spinner3;
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking); // Links to your new beautiful XML layout!

        // Link the UI elements
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        tvQuestion1 = findViewById(R.id.tvQuestion1);
        tvQuestion2 = findViewById(R.id.tvQuestion2);
        tvQuestion3 = findViewById(R.id.tvQuestion3);
        spinner1 = findViewById(R.id.spinner1);
        spinner2 = findViewById(R.id.spinner2);
        spinner3 = findViewById(R.id.spinner3);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        btnConfirm = findViewById(R.id.btnConfirmBooking);

        // Get the category from the delivery truck (Intent)
        category = getIntent().getStringExtra("EXTRA_CATEGORY");
        if (category == null) category = "General Maintenance";
        tvCategoryTitle.setText(category + " Service");

        // Load the right questions and prices
        setupDynamicQuestions();

        // The Magic Calculator: Listen for whenever the user changes a dropdown answer
        AdapterView.OnItemSelectedListener priceCalculator = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calculateDynamicPrice();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinner1.setOnItemSelectedListener(priceCalculator);
        spinner2.setOnItemSelectedListener(priceCalculator);
        spinner3.setOnItemSelectedListener(priceCalculator);

        // Save to Database Button
        btnConfirm.setOnClickListener(v -> saveTicketToCloud());
    }

    private void setupDynamicQuestions() {
        if (category.equals("Plumbing")) {
            tvQuestion1.setText("1. Where is the issue located?");
            setSpinnerOptions(spinner1, new String[]{"Bathroom (+0 JD)", "Kitchen (+0 JD)", "Main Water Line (+10 JD)", "Outdoor (+7 JD)"});
            tvQuestion2.setText("2. What is the exact problem?");
            setSpinnerOptions(spinner2, new String[]{"Pipe Leaking (+12 JD)", "Drain Clogged (+7 JD)", "No Water Pressure (+25 JD)", "Broken Fixture (+5 JD)"});
            tvQuestion3.setText("3. Is there active flooding right now?");
            setSpinnerOptions(spinner3, new String[]{"No, standard repair (+0 JD)", "Yes, EMERGENCY! (+30 JD)"});

        } else if (category.equals("Electrical")) {
            tvQuestion1.setText("1. What is the scope of the outage?");
            setSpinnerOptions(spinner1, new String[]{"Single Outlet/Light (+5 JD)", "Whole Room (+15 JD)", "Entire House (+35 JD)"});
            tvQuestion2.setText("2. Are there any dangerous signs?");
            setSpinnerOptions(spinner2, new String[]{"No visible danger (+0 JD)", "Sparks visible (+20 JD)", "Burning smell (+25 JD)"});
            tvQuestion3.setText("3. Have you checked the main circuit breaker?");
            setSpinnerOptions(spinner3, new String[]{"Yes, it tripped (+0 JD)", "Yes, looks normal (+10 JD)", "No, I don't know how (+5 JD)"});

        } else if (category.equals("AC Repair")) {
            tvQuestion1.setText("1. What type of AC unit is it?");
            setSpinnerOptions(spinner1, new String[]{"Split Unit/Inverter (+0 JD)", "Central AC (+20 JD)", "Window Unit (-5 JD)"});
            tvQuestion2.setText("2. What is the primary issue?");
            setSpinnerOptions(spinner2, new String[]{"Not cooling at all (+15 JD)", "Dripping water inside (+10 JD)", "Making loud noises (+5 JD)", "Won't turn on (+20 JD)"});
            tvQuestion3.setText("3. When was it last serviced?");
            setSpinnerOptions(spinner3, new String[]{"Within the last 6 months (+0 JD)", "Over a year ago (+10 JD)", "Never (+15 JD)"});

        } else if (category.equals("Carpentry")) {
            tvQuestion1.setText("1. What needs to be fixed/installed?");
            setSpinnerOptions(spinner1, new String[]{"Doors & Locks (+5 JD)", "Kitchen Cabinets (+15 JD)", "Furniture Assembly (+10 JD)", "Windows (+10 JD)"});
            tvQuestion2.setText("2. What is the condition?");
            setSpinnerOptions(spinner2, new String[]{"Wood is broken/cracked (+15 JD)", "Swollen from water (+20 JD)", "Hinge/Lock jammed (+5 JD)", "Brand new/Assembly (+0 JD)"});
            tvQuestion3.setText("3. Do you have replacement parts?");
            setSpinnerOptions(spinner3, new String[]{"Yes, I have them (+0 JD)", "No, technician must supply (+25 JD)"});

        } else if (category.equals("Appliances")) {
            tvQuestion1.setText("1. Which appliance is broken?");
            setSpinnerOptions(spinner1, new String[]{"Washing Machine (+10 JD)", "Refrigerator (+15 JD)", "Oven & Stove (+10 JD)", "Water Heater/Geyser (+5 JD)"});
            tvQuestion2.setText("2. What is the brand?");
            setSpinnerOptions(spinner2, new String[]{"Samsung (+5 JD)", "LG (+5 JD)", "Bosch (+10 JD)", "Beko/Other (+0 JD)"});
            tvQuestion3.setText("3. What is it doing wrong?");
            setSpinnerOptions(spinner3, new String[]{"Won't power on (+15 JD)", "Making strange noises (+5 JD)", "Leaking water (+10 JD)", "Not completing cycle (+10 JD)"});

        } else if (category.equals("Pest Control")) {
            tvQuestion1.setText("1. What type of pests are you seeing?");
            setSpinnerOptions(spinner1, new String[]{"Cockroaches (+10 JD)", "Ants (+5 JD)", "Mice & Rodents (+20 JD)", "Bedbugs (+35 JD)"});
            tvQuestion2.setText("2. Where is the infestation located?");
            setSpinnerOptions(spinner2, new String[]{"Kitchen (+5 JD)", "Bedroom (+10 JD)", "Garden (+15 JD)", "The whole house (+40 JD)"});
            tvQuestion3.setText("3. Are there children/pets in the house?");
            setSpinnerOptions(spinner3, new String[]{"No (+0 JD)", "Yes, need organic/safe chemicals (+15 JD)"});

        } else {
            // General default fallback
            tvQuestion1.setText("Describe the severity:");
            setSpinnerOptions(spinner1, new String[]{"Minor (+0 JD)", "Major (+20 JD)"});
            tvQuestion2.setVisibility(View.GONE);
            spinner2.setVisibility(View.GONE);
            tvQuestion3.setVisibility(View.GONE);
            spinner3.setVisibility(View.GONE);
        }
    }

    // THE ENGINE: Reads the text in the dropdowns and extracts the price logic
    private void calculateDynamicPrice() {
        currentTotal = basePrice; // Always start at 10 JD base fee

        String ans1 = spinner1.getSelectedItem() != null ? spinner1.getSelectedItem().toString() : "";
        String ans2 = spinner2.getSelectedItem() != null ? spinner2.getSelectedItem().toString() : "";
        String ans3 = spinner3.getSelectedItem() != null ? spinner3.getSelectedItem().toString() : "";

        if (category.equals("Plumbing")) {
            if (ans1.contains("Main Water")) currentTotal += 20;
            if (ans1.contains("Outdoor")) currentTotal += 10;
            if (ans2.contains("Pipe Leaking")) currentTotal += 15;
            if (ans2.contains("Drain Clogged")) currentTotal += 10;
            if (ans2.contains("No Water Pressure")) currentTotal += 25;
            if (ans2.contains("Broken Fixture")) currentTotal += 5;
            if (ans3.contains("EMERGENCY")) currentTotal += 30;

        } else if (category.equals("Electrical")) {
            if (ans1.contains("Single Outlet")) currentTotal += 5;
            if (ans1.contains("Whole Room")) currentTotal += 15;
            if (ans1.contains("Entire House")) currentTotal += 35;
            if (ans2.contains("Sparks visible")) currentTotal += 20;
            if (ans2.contains("Burning smell")) currentTotal += 25;
            if (ans3.contains("looks normal")) currentTotal += 10;
            if (ans3.contains("don't know how")) currentTotal += 5;

        } else if (category.equals("AC Repair")) {
            if (ans1.contains("Central AC")) currentTotal += 20;
            if (ans1.contains("Window Unit")) currentTotal -= 5;
            if (ans2.contains("Not cooling")) currentTotal += 15;
            if (ans2.contains("Dripping water")) currentTotal += 10;
            if (ans2.contains("loud noises")) currentTotal += 5;
            if (ans2.contains("Won't turn on")) currentTotal += 20;
            if (ans3.contains("Over a year")) currentTotal += 10;
            if (ans3.contains("Never")) currentTotal += 15;

        } else if (category.equals("Carpentry")) {
            if (ans1.contains("Doors & Locks")) currentTotal += 5;
            if (ans1.contains("Cabinets")) currentTotal += 15;
            if (ans1.contains("Furniture")) currentTotal += 10;
            if (ans1.contains("Windows")) currentTotal += 10;
            if (ans2.contains("broken/cracked")) currentTotal += 15;
            if (ans2.contains("Swollen")) currentTotal += 20;
            if (ans2.contains("jammed")) currentTotal += 5;
            if (ans3.contains("technician must supply")) currentTotal += 25;

        } else if (category.equals("Appliances")) {
            if (ans1.contains("Washing Machine")) currentTotal += 10;
            if (ans1.contains("Refrigerator")) currentTotal += 15;
            if (ans1.contains("Oven & Stove")) currentTotal += 10;
            if (ans1.contains("Water Heater")) currentTotal += 5;
            if (ans2.contains("Samsung") || ans2.contains("LG")) currentTotal += 5;
            if (ans2.contains("Bosch")) currentTotal += 10;
            if (ans3.contains("Won't power on")) currentTotal += 15;
            if (ans3.contains("strange noises")) currentTotal += 5;
            if (ans3.contains("Leaking water")) currentTotal += 10;
            if (ans3.contains("Not completing")) currentTotal += 10;

        } else if (category.equals("Pest Control")) {
            if (ans1.contains("Cockroaches")) currentTotal += 10;
            if (ans1.contains("Ants")) currentTotal += 5;
            if (ans1.contains("Mice")) currentTotal += 20;
            if (ans1.contains("Bedbugs")) currentTotal += 35;
            if (ans2.contains("Kitchen")) currentTotal += 5;
            if (ans2.contains("Bedroom")) currentTotal += 10;
            if (ans2.contains("Garden")) currentTotal += 15;
            if (ans2.contains("whole house")) currentTotal += 40;
            if (ans3.contains("organic/safe")) currentTotal += 15;

        } else {
            if (ans1.contains("Major")) currentTotal += 20;
        }

        // Ensure price never drops below the base callout fee (just in case of negatives!)
        if (currentTotal < basePrice) currentTotal = basePrice;

        tvTotalPrice.setText(String.format("%.2f JOD", currentTotal));
    }

    // Helper method to easily fill dropdowns
    private void setSpinnerOptions(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
        spinner.setAdapter(adapter);
    }



    // Same save function as before, but now we save the answers and the exact price!
    private void saveTicketToCloud() {
        btnConfirm.setText("Uploading...");
        btnConfirm.setEnabled(false);

        Map<String, Object> ticket = new HashMap<>();
        ticket.put("category", category);
        ticket.put("q1_answer", spinner1.getSelectedItem() != null ? spinner1.getSelectedItem().toString() : "");
        ticket.put("q2_answer", spinner2.getSelectedItem() != null ? spinner2.getSelectedItem().toString() : "");
        ticket.put("q3_answer", spinner3.getSelectedItem() != null ? spinner3.getSelectedItem().toString() : "");
        ticket.put("total_estimated_price", currentTotal);
        ticket.put("status", "Pending Technician Assignment");
        ticket.put("timestamp", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("maintenance_tickets")
                .add(ticket)
                .addOnSuccessListener(docRef -> {
                    // Start the Uber Tracking Screen!
                    android.content.Intent intent = new android.content.Intent(BookingActivity.this, TrackingActivity.class);
                    intent.putExtra("EXTRA_CATEGORY", category);
                    startActivity(intent);
                    finish(); // Close the booking screen
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving to cloud.", Toast.LENGTH_SHORT).show();
                    btnConfirm.setText("CONFIRM & DISPATCH");
                    btnConfirm.setEnabled(true);
                });
    }
}