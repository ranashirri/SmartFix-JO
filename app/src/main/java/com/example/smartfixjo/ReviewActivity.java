package com.example.smartfixjo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ReviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        RatingBar ratingBar = findViewById(R.id.ratingBar);
        TextInputEditText etFinalPrice = findViewById(R.id.etFinalPrice);
        Button btnSubmitReview = findViewById(R.id.btnSubmitReview);

        btnSubmitReview.setOnClickListener(v -> {
            String priceEntered = etFinalPrice.getText().toString().trim();
            float stars = ratingBar.getRating();

            // 1. Safety Check: Make sure they actually typed a number
            if (priceEntered.isEmpty()) {
                etFinalPrice.setError("Please enter the amount paid");
                return;
            }

            // Lock the button so they don't double-click and pay twice!
            btnSubmitReview.setText("SAVING TO CLOUD...");
            btnSubmitReview.setEnabled(false);

            // 2. Package the data up for Firebase
            double finalAmount = Double.parseDouble(priceEntered);

            Map<String, Object> reviewData = new HashMap<>();
            reviewData.put("technician_name", "Mahmoud A."); // Hardcoded for the prototype
            reviewData.put("rating", stars);
            reviewData.put("amount_paid_jod", finalAmount);
            reviewData.put("timestamp", FieldValue.serverTimestamp());

            // 3. Send it to the Firestore Database
            FirebaseFirestore.getInstance().collection("job_reviews")
                    .add(reviewData)
                    .addOnSuccessListener(documentReference -> {
                        // Success!
                        Toast.makeText(this, "Payment Logged! Rating: " + stars + " Stars", Toast.LENGTH_LONG).show();

                        // Teleport back to Home Screen
                        Intent intent = new Intent(ReviewActivity.this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        // Uh oh, internet issue
                        Toast.makeText(this, "Error saving payment. Try again.", Toast.LENGTH_SHORT).show();
                        btnSubmitReview.setText("SUBMIT & RETURN HOME");
                        btnSubmitReview.setEnabled(true);
                    });
        });
    }
}