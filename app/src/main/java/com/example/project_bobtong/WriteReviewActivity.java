package com.example.project_bobtong;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class WriteReviewActivity extends AppCompatActivity {
    private EditText editTextReview;
    private Button buttonSubmitReview;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        mDatabase = FirebaseDatabase.getInstance().getReference("restaurant_reviews");

        editTextReview = findViewById(R.id.editTextReview);
        buttonSubmitReview = findViewById(R.id.buttonSubmitReview);

        String restaurantId = getIntent().getStringExtra("restaurantId");

        buttonSubmitReview.setOnClickListener(v -> {
            String review = editTextReview.getText().toString().trim();
            if (!review.isEmpty()) {
                submitReview(restaurantId, review);
            } else {
                Toast.makeText(WriteReviewActivity.this, "리뷰를 입력하세요", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitReview(String restaurantId, String review) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String key = mDatabase.child(restaurantId).push().getKey();
        if (key != null) {
            Review newReview = new Review(userId, restaurantId, review);
            mDatabase.child(restaurantId).child(key).setValue(newReview).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(WriteReviewActivity.this, "리뷰가 제출되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(WriteReviewActivity.this, "리뷰 제출에 실패했습니다", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
