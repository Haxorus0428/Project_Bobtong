package com.example.project_bobtong;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MyPageActivity extends AppCompatActivity {

    private Button buttonLogout, buttonMainPage, buttonMyReviews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage);

        buttonLogout = findViewById(R.id.buttonLogout);
        buttonMainPage = findViewById(R.id.buttonMainPage);
        buttonMyReviews = findViewById(R.id.buttonMyReviews);

        buttonLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MyPageActivity.this, LoginActivity.class));
            finish();
        });

        buttonMainPage.setOnClickListener(v -> {
            startActivity(new Intent(MyPageActivity.this, MainActivity.class));
        });

        buttonMyReviews.setOnClickListener(v -> {
            startActivity(new Intent(MyPageActivity.this, MyReviewsActivity.class));
        });
    }
}
