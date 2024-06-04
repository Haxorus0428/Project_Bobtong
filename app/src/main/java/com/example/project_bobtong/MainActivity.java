package com.example.project_bobtong;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String CLIENT_ID = "REDACTED"; // 네이버 API 클라이언트 ID
    private static final String CLIENT_SECRET = "REDACTED"; // 네이버 API 클라이언트 시크릿

    private FusedLocationProviderClient mFusedLocationClient;
    private NaverMap mNaverMap;
    private FusedLocationSource mLocationSource;

    private RecyclerView mRecyclerView;
    private RestaurantAdapter mAdapter;
    private DatabaseReference mDatabase;
    private NaverApiService mNaverApiService;

    private boolean isFirstLoad = true;
    private EditText editTextQuery;
    private Button buttonSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase 초기화
        FirebaseApp.initializeApp(this);

        // 위치 권한 요청
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);

        // Toolbar 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // RecyclerView 초기화
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new RestaurantAdapter();
        mRecyclerView.setAdapter(mAdapter);

        // Firebase Database 초기화
        mDatabase = FirebaseDatabase.getInstance().getReference("restaurants");

        // 네이버 검색 API 서비스 초기화
        Gson gson = new GsonBuilder().create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(NaverApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        mNaverApiService = retrofit.create(NaverApiService.class);

        // FusedLocationProviderClient 초기화
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 지도 초기화
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        // 검색창 및 버튼 초기화
        editTextQuery = findViewById(R.id.editTextQuery);
        buttonSearch = findViewById(R.id.buttonSearch);

        // FusedLocationSource 초기화
        mLocationSource = new FusedLocationSource(this, PERMISSION_REQUEST_CODE);

        // 검색 버튼 클릭 리스너 설정
        buttonSearch.setOnClickListener(v -> {
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_category) {
            startActivity(new Intent(this, CategoryActivity.class));
            return true;
        } else if (id == R.id.action_bookmark) {
            startActivity(new Intent(this, BookmarkActivity.class));
            return true;
        } else if (id == R.id.action_mypage) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                startActivity(new Intent(this, MyPageActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        mNaverMap = naverMap;

        // 위치 소스 및 추적 모드 설정
        naverMap.setLocationSource(mLocationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        // Firebase에서 데이터를 불러와 마커 추가
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Restaurant restaurant = dataSnapshot.getValue(Restaurant.class);
                    if (restaurant != null) {
                        Log.d("RestaurantData", "Name: " + restaurant.getTitle() + " Lat: " + restaurant.getLatitude() + " Lng: " + restaurant.getLongitude());
                        // 위도와 경도로 변환
                        double latitude = restaurant.getLatitude() / 10.0;
                        double longitude = restaurant.getLongitude() / 10.0;
                        LatLng latLng = new LatLng(latitude, longitude);

                        // 마커 설정
                        Marker marker = new Marker();
                        marker.setPosition(latLng);
                        marker.setMap(mNaverMap);

                        // 마커 클릭 리스너 설정
                        marker.setOnClickListener(overlay -> {
                            showRestaurantInfo(restaurant);
                            return true;
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load restaurants", error.toException());
            }
        });
    }

    private void searchRestaurants(String query) {
        Call<SearchResponse> call = mNaverApiService.searchRestaurants(query, 10, 1, "random", CLIENT_ID, CLIENT_SECRET);
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("API Response", response.body().toString());
                    List<Restaurant> restaurants = response.body().getItems();

                    for (Restaurant restaurant : restaurants) {
                        String name = restaurant.getTitle();
                        if (name != null) {
                            name = name.replaceAll("<b>", "").replaceAll("</b>", "");
                            restaurant.setTitle(name);
                        } else {
                            restaurant.setTitle("Unknown Restaurant");
                        }

                        // TM128 좌표를 사용하여 위도와 경도로 변환하지 않고 그대로 저장
                        restaurant.setMapx(restaurant.getMapx());
                        restaurant.setMapy(restaurant.getMapy());

                        // Firebase에 데이터 저장
                        String key = mDatabase.push().getKey();
                        if (key != null) {
                            restaurant.setId(key); // ID 설정
                            mDatabase.child(key).setValue(restaurant).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("Firebase", "Restaurant saved: " + restaurant.getTitle());
                                } else {
                                    Log.e("Firebase", "Failed to save restaurant", task.getException());
                                }
                            });
                        }
                    }

                    // RecyclerView에 검색 결과 표시
                    mAdapter.setRestaurants(restaurants);
                } else {
                    Log.e("API Error", response.errorBody().toString());
                    Toast.makeText(MainActivity.this, "검색에 실패했습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                Log.e("API Error", "Error fetching data", t);
                Toast.makeText(MainActivity.this, "검색 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRestaurantInfo(Restaurant restaurant) {
        // 식당 정보 표시를 위한 다이얼로그 또는 액티비티를 구현
        Toast.makeText(this, "식당 이름: " + restaurant.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (mLocationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!mLocationSource.isActivated()) { // 권한 거부됨
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
