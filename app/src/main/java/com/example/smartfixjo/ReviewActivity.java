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

    private String category;
    private double estimatedPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        // 1. Catch the data from the previous screens
        category = getIntent().getStringExtra("EXTRA_CATEGORY");
        estimatedPrice = getIntent().getDoubleExtra("EXTRA_ESTIMATE", 0.0);

        if (category == null) category = "Unknown Service";

        RatingBar ratingBar = findViewById(R.id.ratingBar);
        TextInputEditText etFinalPrice = findViewById(R.id.etFinalPrice);
        Button btnSubmitReview = findViewById(R.id.btnSubmitReview);

        btnSubmitReview.setOnClickListener(v -> {
            String priceEntered = etFinalPrice.getText().toString().trim();
            float stars = ratingBar.getRating();

            if (priceEntered.isEmpty()) {
                etFinalPrice.setError("Please enter the amount paid");
                return;
            }

            btnSubmitReview.setText("SAVING AI DATA...");
            btnSubmitReview.setEnabled(false);

            double actualPrice = Double.parseDouble(priceEntered);

            // 2. Calculate the variance for the AI!
            double priceDifference = actualPrice - estimatedPrice;

            // 3. Package the ultimate ML Training Payload
            Map<String, Object> aiTrainingData = new HashMap<>();
            aiTrainingData.put("category", category);
            aiTrainingData.put("technician_name", "Mahmoud A.");
            aiTrainingData.put("rating", stars);

            // The Cost Analysis
            aiTrainingData.put("estimated_cost", estimatedPrice);
            aiTrainingData.put("actual_cost", actualPrice);
            aiTrainingData.put("variance_jod", priceDifference); // Positive means app underpriced, Negative means app overpriced

            // Timestamp
            aiTrainingData.put("timestamp", FieldValue.serverTimestamp());

            // 4. Send to a new dedicated AI feedback collection
            FirebaseFirestore.getInstance().collection("ai_cost_analysis")
                    .add(aiTrainingData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Data secured for AI training!", Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(ReviewActivity.this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving data. Try again.", Toast.LENGTH_SHORT).show();
                        btnSubmitReview.setText("SUBMIT & RETURN HOME");
                        btnSubmitReview.setEnabled(true);
                    });
        });
    }
}