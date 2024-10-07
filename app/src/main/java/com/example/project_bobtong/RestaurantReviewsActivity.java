package com.example.project_bobtong;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RestaurantReviewsActivity extends AppCompatActivity {

    private ListView listView;
    private TextView noReviewsText;
    private Button buttonWriteReview;
    private List<Review> reviewList; // List<Review>로 변경
    private ReviewAdapter adapter;
    private DatabaseReference reviewsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_reviews);

        listView = findViewById(R.id.listView);
        noReviewsText = findViewById(R.id.no_reviews_text);
        buttonWriteReview = findViewById(R.id.buttonSubmitReview); // 리뷰 작성 버튼
        reviewList = new ArrayList<>(); // List<Review>로 변경
        adapter = new ReviewAdapter(this, reviewList);
        listView.setAdapter(adapter);

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        String restaurantId = getIntent().getStringExtra("restaurantId");
        if (restaurantId != null) {
            reviewsRef = FirebaseDatabase.getInstance().getReference("restaurant_reviews").child(restaurantId);
            loadReviews();

            buttonWriteReview.setOnClickListener(v -> {
                Intent intent = new Intent(RestaurantReviewsActivity.this, WriteReviewActivity.class);
                intent.putExtra("restaurantId", restaurantId);
                startActivity(intent);
            });
        } else {
            noReviewsText.setText("음식점 정보가 없습니다.");
            noReviewsText.setVisibility(View.VISIBLE);
        }
    }

    private void loadReviews() {
        reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Review review = dataSnapshot.getValue(Review.class);
                        if (review != null) {
                            // 리뷰 작성 시간을 포맷팅
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            String timestamp = sdf.format(new Date(review.getTimestamp()));
                            review.setTimestamp(review.getTimestamp()); // Timestamp 업데이트
                            reviewList.add(review); // Review 객체를 추가
                        }
                    }
                    adapter.notifyDataSetChanged();
                }

                noReviewsText.setVisibility(reviewList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noReviewsText.setText("리뷰 불러오기를 실패했습니다.");
                noReviewsText.setVisibility(View.VISIBLE);
            }
        });
    }
}