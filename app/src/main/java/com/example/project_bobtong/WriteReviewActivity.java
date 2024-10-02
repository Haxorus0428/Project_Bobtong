package com.example.project_bobtong;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class WriteReviewActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText editTextReview;
    private Button buttonSubmitReview, buttonChooseImage;
    private ImageView imageViewPreview;
    private Uri imageUri;

    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        // Firebase 초기화
        mDatabase = FirebaseDatabase.getInstance().getReference("restaurant_reviews");
        mStorageRef = FirebaseStorage.getInstance().getReference("review_images");

        // UI 요소 연결
        editTextReview = findViewById(R.id.editTextReview);
        buttonSubmitReview = findViewById(R.id.buttonSubmitReview);
        buttonChooseImage = findViewById(R.id.buttonChooseImage);
        imageViewPreview = findViewById(R.id.imageViewPreview);

        String restaurantId = getIntent().getStringExtra("restaurantId");

        // 이미지 선택 버튼 클릭 리스너
        buttonChooseImage.setOnClickListener(v -> openFileChooser());

        // 리뷰 제출 버튼 클릭 리스너
        buttonSubmitReview.setOnClickListener(v -> {
            String reviewText = editTextReview.getText().toString().trim();
            if (!reviewText.isEmpty()) {
                if (imageUri != null) {
                    uploadImageAndSubmitReview(restaurantId, reviewText);
                } else {
                    submitReview(restaurantId, reviewText, null); // 이미지 없이 리뷰 제출
                }
            } else {
                Toast.makeText(WriteReviewActivity.this, "리뷰를 입력해주세요", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 파일 선택
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // 이미지 선택 결과 처리
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imageViewPreview.setImageURI(imageUri); // 이미지 미리보기
        }
    }

    // 이미지 업로드 및 리뷰 제출
    private void uploadImageAndSubmitReview(String restaurantId, String reviewText) {
        String fileName = UUID.randomUUID().toString(); // 파일 이름 UUID로 생성
        StorageReference fileRef = mStorageRef.child(fileName);

        // 이미지 업로드
        fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                submitReview(restaurantId, reviewText, imageUrl); // 이미지 URL과 함께 리뷰 제출
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(WriteReviewActivity.this, "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
        });
    }

    // 리뷰 제출
    private void submitReview(String restaurantId, String reviewText, @Nullable String imageUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            String userName = user.getDisplayName();
            long currentTime = System.currentTimeMillis();

            // 리뷰 객체 생성
            Review review = new Review(userId, restaurantId, reviewText, userName, imageUrl, currentTime);
            String key = mDatabase.child(restaurantId).push().getKey(); // Firebase에 저장할 키 생성
            if (key != null) {
                mDatabase.child(restaurantId).child(key).setValue(review).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(WriteReviewActivity.this, "리뷰가 제출되었습니다.", Toast.LENGTH_SHORT).show();
                        finish(); // 작성 완료 후 액티비티 종료
                    } else {
                        Toast.makeText(WriteReviewActivity.this, "리뷰 제출에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
