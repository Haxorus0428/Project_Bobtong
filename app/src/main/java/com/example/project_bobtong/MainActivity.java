package com.example.project_bobtong;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;

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

    private RecyclerView mRecyclerView;
    private RestaurantAdapter mAdapter;
    private DatabaseReference mDatabase;
    private NaverApiService mNaverApiService;

    private boolean isFirstLoad = true;

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

        // 사용자의 현재 위치 가져오기
        if (isFirstLoad) {
            isFirstLoad = false;
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                // 위치 정보 가져오기 성공
                                Location location = task.getResult();
                                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                CameraUpdate cameraUpdate = CameraUpdate.scrollTo(currentLatLng);
                                mNaverMap.moveCamera(cameraUpdate);

                                // 현재 위치를 Firebase에 저장
                                String key = mDatabase.push().getKey();
                                if (key != null) {
                                    Restaurant restaurant = new Restaurant();
                                    restaurant.setTitle("Current Location");
                                    restaurant.setLatitude(location.getLatitude());
                                    restaurant.setLongitude(location.getLongitude());
                                    mDatabase.child(key).setValue(restaurant);
                                }
                            } else {
                                // 위치 정보 가져오기 실패
                                Toast.makeText(MainActivity.this, "현재 위치를 가져올 수 없습니다", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
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
                        // API 응답 데이터 구조 확인
                        Log.d("Restaurant Data", restaurant.toString());

                        // <b> 태그 제거 및 null 체크
                        String name = restaurant.getTitle();
                        if (name != null) {
                            Log.d("Restaurant Name", name);  // 이름을 로그에 출력
                            name = name.replaceAll("<b>", "").replaceAll("</b>", "");
                            restaurant.setTitle(name);
                        } else {
                            Log.d("Restaurant Name", "Unknown Restaurant");  // 이름을 로그에 출력
                            restaurant.setTitle("Unknown Restaurant");
                        }

                        // 위도와 경도를 TM128에서 WGS84로 변환
                        double latitude = convertTm128ToWgs84Latitude(restaurant.getLatitude());
                        double longitude = convertTm128ToWgs84Longitude(restaurant.getLongitude());
                        restaurant.setLatitude(latitude);
                        restaurant.setLongitude(longitude);

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

                        // 지도에 마커 추가
                        Marker marker = new Marker();
                        marker.setPosition(new LatLng(latitude, longitude));
                        marker.setMap(mNaverMap);

                        // 마커 클릭 리스너 추가
                        marker.setOnClickListener(overlay -> {
                            showRestaurantInfo(restaurant);
                            return true;
                        });
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
                Log.e("API Failure", t.getMessage(), t);
                Toast.makeText(MainActivity.this, "검색에 실패했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double convertTm128ToWgs84Latitude(double tm128Latitude) {
        double a = 6378137.0;
        double b = 6356752.314245;
        double e = Math.sqrt(1 - (b * b) / (a * a));
        double x = tm128Latitude - 200000.0;
        double y = tm128Latitude - 500000.0;
        double lambda0 = Math.toRadians(127.0);
        double m = x / 6367449.145771;
        double mu = m + (1.0 - 0.25 * e * e - 3.0 / 64.0 * e * e * e * e) * Math.sin(2.0 * m)
                + (3.0 / 8.0 * e * e + 3.0 / 32.0 * e * e * e * e) * Math.sin(4.0 * m)
                + (15.0 / 256.0 * e * e * e * e) * Math.sin(6.0 * m);

        double e1 = (1.0 - Math.sqrt(1.0 - e * e)) / (1.0 + Math.sqrt(1.0 - e * e));
        double phi1 = mu + (3.0 / 2.0 * e1 - 27.0 / 32.0 * e1 * e1 * e1) * Math.sin(2.0 * mu)
                + (21.0 / 16.0 * e1 * e1 - 55.0 / 32.0 * e1 * e1 * e1) * Math.sin(4.0 * mu)
                + (151.0 / 96.0 * e1 * e1 * e1) * Math.sin(6.0 * mu);

        double N1 = a / Math.sqrt(1.0 - e * e * Math.sin(phi1) * Math.sin(phi1));
        double R1 = a * (1.0 - e * e) / Math.pow(1.0 - e * e * Math.sin(phi1) * Math.sin(phi1), 1.5);
        double D = y / (N1 * Math.cos(phi1));

        double lat = phi1 - (N1 * Math.tan(phi1) / R1) * (D * D / 2.0 - (5.0 + 3.0 * Math.tan(phi1) * Math.tan(phi1)
                + 10.0 * e1 - 4.0 * e1 * e1 - 9.0 * e1 * Math.tan(phi1) * Math.tan(phi1)) * D * D * D * D / 24.0
                + (61.0 + 90.0 * Math.tan(phi1) * Math.tan(phi1) + 298.0 * e1 + 45.0 * Math.tan(phi1) * Math.tan(phi1)
                - 252.0 * e1 - 3.0 * e1 * e1) * D * D * D * D * D * D / 720.0);

        return Math.toDegrees(lat);
    }

    private double convertTm128ToWgs84Longitude(double tm128Longitude) {
        double a = 6378137.0;
        double b = 6356752.314245;
        double e = Math.sqrt(1 - (b * b) / (a * a));
        double x = tm128Longitude - 200000.0;
        double y = tm128Longitude - 500000.0;
        double lambda0 = Math.toRadians(127.0);
        double m = x / 6367449.145771;
        double mu = m + (1.0 - 0.25 * e * e - 3.0 / 64.0 * e * e * e * e) * Math.sin(2.0 * m)
                + (3.0 / 8.0 * e * e + 3.0 / 32.0 * e * e * e * e) * Math.sin(4.0 * m)
                + (15.0 / 256.0 * e * e * e * e) * Math.sin(6.0 * m);

        double e1 = (1.0 - Math.sqrt(1.0 - e * e)) / (1.0 + Math.sqrt(1.0 - e * e));
        double phi1 = mu + (3.0 / 2.0 * e1 - 27.0 / 32.0 * e1 * e1 * e1) * Math.sin(2.0 * mu)
                + (21.0 / 16.0 * e1 * e1 - 55.0 / 32.0 * e1 * e1 * e1) * Math.sin(4.0 * mu)
                + (151.0 / 96.0 * e1 * e1 * e1) * Math.sin(6.0 * mu);

        double N1 = a / Math.sqrt(1.0 - e * e * Math.sin(phi1) * Math.sin(phi1));
        double R1 = a * (1.0 - e * e) / Math.pow(1.0 - e * e * Math.sin(phi1) * Math.sin(phi1), 1.5);
        double D = y / (N1 * Math.cos(phi1));

        double lng = lambda0 + (D - (1.0 + 2.0 * Math.tan(phi1) * Math.tan(phi1) + e1) * D * D * D / 6.0
                + (5.0 - 2.0 * e1 + 28.0 * Math.tan(phi1) * Math.tan(phi1) - 3.0 * e1 * e1 + 8.0 * e1 * e1 * e1
                + 24.0 * Math.tan(phi1) * Math.tan(phi1) * Math.tan(phi1)) * D * D * D * D * D / 120.0) / Math.cos(phi1);

        return Math.toDegrees(lng);
    }

    private void showRestaurantInfo(Restaurant restaurant) {
        // 식당 정보 표시 및 북마크, 리뷰 기능 추가
        RestaurantInfoDialog dialog = new RestaurantInfoDialog(this, restaurant);
        dialog.show();
    }
}
