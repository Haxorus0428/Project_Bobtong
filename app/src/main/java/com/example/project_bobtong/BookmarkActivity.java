package com.example.project_bobtong;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

public class BookmarkActivity extends AppCompatActivity {

    private ListView listView;
    private TextView noBookmarksText;
    private List<Restaurant> bookmarkList;
    private ArrayAdapter<String> adapter;
    private DatabaseReference bookmarkRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmark);

        listView = findViewById(R.id.list_view);
        noBookmarksText = findViewById(R.id.no_bookmarks_text);
        bookmarkList = new ArrayList<>();
        List<String> bookmarkTitles = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bookmarkTitles);
        listView.setAdapter(adapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            bookmarkRef = FirebaseDatabase.getInstance().getReference("bookmarks").child(user.getUid());
            loadBookmarks(bookmarkTitles);
        } else {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Restaurant selectedRestaurant = bookmarkList.get(position);
            if (selectedRestaurant != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", selectedRestaurant.getLatitude());
                resultIntent.putExtra("longitude", selectedRestaurant.getLongitude());
                resultIntent.putExtra("title", selectedRestaurant.getTitle());
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(BookmarkActivity.this, "음식점을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // 메인 페이지로 돌아가기 버튼 추가
        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

    private void loadBookmarks(List<String> bookmarkTitles) {
        bookmarkRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookmarkList.clear();
                bookmarkTitles.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Restaurant restaurant = dataSnapshot.getValue(Restaurant.class);
                        if (restaurant != null) {
                            bookmarkList.add(restaurant);
                            bookmarkTitles.add(restaurant.getTitle());
                        }
                    }
                    adapter.notifyDataSetChanged();
                }

                if (bookmarkList.isEmpty()) {
                    noBookmarksText.setVisibility(View.VISIBLE);
                } else {
                    noBookmarksText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load bookmarks", error.toException());
                noBookmarksText.setText("북마크를 불러오는 데 실패했습니다.");
                noBookmarksText.setVisibility(View.VISIBLE);
            }
        });
    }
}
