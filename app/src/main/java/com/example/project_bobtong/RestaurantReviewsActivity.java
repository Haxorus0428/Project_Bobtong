package com.example.project_bobtong;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
    private List<String> reviewList;
    private ArrayAdapter<String> adapter;
    private DatabaseReference reviewsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_reviews);

        listView = findViewById(R.id.listView);
        noReviewsText = findViewById(R.id.no_reviews_text);
        reviewList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reviewList);
        listView.setAdapter(adapter);

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        String restaurantId = getIntent().getStringExtra("restaurantId");
        if (restaurantId != null) {
            reviewsRef = FirebaseDatabase.getInstance().getReference("restaurant_reviews").child(restaurantId);
            loadReviews();
        } else {
            noReviewsText.setText("식당 정보가 없습니다.");
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
                        String review = dataSnapshot.getValue(String.class);
                        if (review != null) {
                            reviewList.add(review);
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
                noReviewsText.setText("리뷰를 불러오는 데 실패했습니다.");
                noReviewsText.setVisibility(View.VISIBLE);
            }
        });
    }
}
