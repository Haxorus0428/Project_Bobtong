package com.example.project_bobtong;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RestaurantReviewsActivity extends AppCompatActivity {

    private ListView listView;
    private TextView noReviewsText;
    private EditText editTextReview;
    private Button buttonSubmitReview;
    private List<String> reviewList;
    private ArrayAdapter<String> adapter;
    private DatabaseReference reviewsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_reviews);

        listView = findViewById(R.id.listView);
        noReviewsText = findViewById(R.id.no_reviews_text);
        editTextReview = findViewById(R.id.editTextReview);
        buttonSubmitReview = findViewById(R.id.buttonSubmitReview);
        reviewList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reviewList);
        listView.setAdapter(adapter);

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        String restaurantId = getIntent().getStringExtra("restaurantId");
        if (restaurantId != null) {
            reviewsRef = FirebaseDatabase.getInstance().getReference("restaurant_reviews").child(restaurantId);
            loadReviews();

            buttonSubmitReview.setOnClickListener(v -> submitReview(restaurantId));
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
                            reviewList.add(review.getReviewText());
                        }
                    }
                    adapter.notifyDataSetChanged();
                }

                if (reviewList.isEmpty()) {
                    noReviewsText.setVisibility(View.VISIBLE);
                } else {
                    noReviewsText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noReviewsText.setText("리뷰 불러오기를 실패했습니다.");
                noReviewsText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void submitReview(String restaurantId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            String reviewText = editTextReview.getText().toString().trim();
            if (!reviewText.isEmpty()) {
                DatabaseReference newReviewRef = reviewsRef.push();
                Review review = new Review(userId, restaurantId, reviewText);
                newReviewRef.setValue(review);
                editTextReview.setText("");

                // 사용자 리뷰도 저장
                DatabaseReference userReviewRef = FirebaseDatabase.getInstance().getReference("user_reviews").child(userId).child(newReviewRef.getKey());
                userReviewRef.setValue(reviewText);
            } else {
                Toast.makeText(this, "리뷰를 입력해주세요", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
