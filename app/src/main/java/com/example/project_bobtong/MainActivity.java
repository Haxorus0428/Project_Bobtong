package com.example.project_bobtong;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.CameraUpdateFactory;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private FusedLocationSource mLocationSource;
    private NaverMap mNaverMap;
    private RecyclerView mRecyclerView;
    private RestaurantAdapter mAdapter;
    private List<Marker> mMarkerList = new ArrayList<>();
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 위치 권한 요청
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);

        // 지도 초기화
        mLocationSource = new FusedLocationSource(this, PERMISSION_REQUEST_CODE);
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);

        // RecyclerView 초기화
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new RestaurantAdapter();
        mRecyclerView.setAdapter(mAdapter);

        // Firebase 초기화
        mDatabase = FirebaseDatabase.getInstance().getReference("restaurants");

        // 검색창 초기화
        EditText editTextQuery = findViewById(R.id.editTextQuery);
        findViewById(R.id.buttonSearch).setOnClickListener(v -> {
            String query = editTextQuery.getText().toString().trim();
            if (!query.isEmpty()) {
                // 검색 실행
                searchRestaurants(query);
            } else {
                Toast.makeText(MainActivity.this, "검색어를 입력하세요", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mLocationSource.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        mNaverMap = naverMap;
        mNaverMap.setLocationSource(mLocationSource);

        // 사용자의 현재 위치로 지도 이동
        LatLng currentLocation = new LatLng(/* 사용자의 위도 */, /* 사용자의 경도 */);
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(currentLocation).animate(ScrollToAnimation.Easing);
        mNaverMap.moveCamera(cameraUpdate);
    }

    private void searchRestaurants(String query) {
        // Firebase에서 음식점 검색
        Query firebaseQuery = mDatabase.orderByChild("name").equalTo(query);
        firebaseQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Restaurant> restaurants = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Restaurant restaurant = snapshot.getValue(Restaurant.class);
                    restaurants.add(restaurant);
                }

                // 검색 결과를 지도에 표시
                for (Restaurant restaurant : restaurants) {
                    LatLng location = new LatLng(restaurant.getLatitude(), restaurant.getLongitude());
                    Marker marker = new Marker();
                    marker.setPosition(location);
                    marker.setMap(mNaverMap);
                    mMarkerList.add(marker);
                }

                // RecyclerView에 검색 결과 표시
                mAdapter.setRestaurants(restaurants);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "검색에 실패했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
