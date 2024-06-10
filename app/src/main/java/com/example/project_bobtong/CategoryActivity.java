package com.example.project_bobtong;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

public class CategoryActivity extends AppCompatActivity {
    private Spinner categorySpinner;
    private Spinner distanceSpinner;
    private Button searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        categorySpinner = findViewById(R.id.categorySpinner);
        distanceSpinner = findViewById(R.id.distanceSpinner);
        searchButton = findViewById(R.id.searchButton);

        searchButton.setOnClickListener(v -> {
            String category = categorySpinner.getSelectedItem().toString();
            String distanceStr = distanceSpinner.getSelectedItem().toString();
            double distance = Double.parseDouble(distanceStr.replaceAll("[^\\d.]", "")); // 숫자와 소수점만 추출하여 변환

            Log.d("CategoryDistance", "Category: " + category + ", Distance: " + distance);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("category", category);
            resultIntent.putExtra("distance", distance);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        // 메인 페이지로 돌아가기 버튼 추가
        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }
}
