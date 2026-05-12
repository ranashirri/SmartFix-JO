package com.example.smartfixjo;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// The Database Imports
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Wake up the Database connection
        db = FirebaseFirestore.getInstance();

        TextView tvConfirmation = new TextView(this);
        tvConfirmation.setTextSize(20f);
        tvConfirmation.setPadding(50, 50, 50, 50);

        android.content.Intent truck = getIntent();
        if (truck != null) {
            String category = truck.getStringExtra("EXTRA_CATEGORY");
            String urgency = truck.getStringExtra("EXTRA_URGENCY");
            String title = truck.getStringExtra("EXTRA_TITLE");

            tvConfirmation.setText("✅ Dispatch Confirmed!\n\n" +
                    "Category: " + category + "\n" +
                    "Urgency: " + urgency + "\n" +
                    "Issue: " + title + "\n\n" +
                    "Uploading to cloud database...");

            // 2. Package the data into a neat box (HashMap)
            Map<String, Object> ticket = new HashMap<>();
            ticket.put("category", category);
            ticket.put("urgency", urgency);
            ticket.put("title", title);
            ticket.put("status", "Pending Technician Assignment");
            ticket.put("timestamp", FieldValue.serverTimestamp()); // Logs the exact time!

            // 3. Send the box to the "maintenance_tickets" folder in the cloud
            db.collection("maintenance_tickets")
                    .add(ticket)
                    .addOnSuccessListener(documentReference -> {
                        // Success! Update the screen.
                        tvConfirmation.append("\n\n✅ Saved to Cloud Database!\nTicket ID: " + documentReference.getId());
                        Toast.makeText(this, "Ticket Saved Securely!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // Uh oh, something went wrong
                        Toast.makeText(this, "Database Error", Toast.LENGTH_SHORT).show();
                        Log.e("FIRESTORE", "Error saving ticket", e);
                    });
        }

        setContentView(tvConfirmation);
    }
}