package com.example.project_bobtong;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.CircleOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String SEARCH_CLIENT_ID = "REDACTED"; // 네이버 검색 API 클라이언트 ID
    private static final String SEARCH_CLIENT_SECRET = "REDACTED"; // 네이버 검색 API 클라이언트 시크릿

    private FusedLocationProviderClient mFusedLocationClient;
    private NaverMap mNaverMap;
    private FusedLocationSource mLocationSource;
    private Location mCurrentLocation;

    private RecyclerView mRecyclerView;
    private RestaurantAdapter mAdapter;
    private DatabaseReference mDatabase;
    private NaverApiService mNaverApiService;

    private boolean isFirstLoad = true;
    private EditText editTextQuery;
    private Button buttonSearch;

    private SlidingUpPanelLayout slidingLayout;
    private List<Marker> searchMarkerList = new ArrayList<>(); // 검색된 마커 리스트
    private List<Marker> bookmarkMarkerList = new ArrayList<>(); // 북마크된 마커 리스트
    private PathOverlay pathOverlay;
    private CircleOverlay circleOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase 초기화
        FirebaseApp.initializeApp(this);

        // 위치 권한 요청
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);

        // RecyclerView 초기화
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new RestaurantAdapter();
        mRecyclerView.setAdapter(mAdapter);

        // Firebase Database 초기화
        mDatabase = FirebaseDatabase.getInstance().getReference("restaurants");

        // 네이버 검색 API 서비스 초기화
        Gson gson = new GsonBuilder().create();
        Retrofit retrofitSearch = new Retrofit.Builder()
                .baseUrl("https://openapi.naver.com/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        mNaverApiService = retrofitSearch.create(NaverApiService.class);

        // FusedLocationProviderClient 초기화
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 지도 초기화
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.mapContainer);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        // 검색창 및 버튼 초기화
        editTextQuery = findViewById(R.id.editTextQuery);
        buttonSearch = findViewById(R.id.buttonSearch);

        // FusedLocationSource 초기화
        mLocationSource = new FusedLocationSource(this, PERMISSION_REQUEST_CODE);

        // SlidingUpPanelLayout 초기화
        slidingLayout = findViewById(R.id.sliding_layout);

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

        // BottomNavigationView 초기화 및 클릭 리스너 설정
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_category) {
                showCategoryDialog();
                return true;
            } else if (itemId == R.id.action_bookmark) {
                startActivity(new Intent(this, BookmarkActivity.class));
                return true;
            } else if (itemId == R.id.action_mypage) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    startActivity(new Intent(this, MyPageActivity.class));
                } else {
                    startActivity(new Intent(this, LoginActivity.class));
                }
                return true;
            }
            return false;
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
            showCategoryDialog();
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

        // 지도 클릭 리스너 설정
        naverMap.setOnMapClickListener((point, coord) -> {
            clearSearchMarkers(); // 검색된 마커 제거
            if (pathOverlay != null) {
                pathOverlay.setMap(null); // 경로 오버레이 제거
            }
        });

        // 현재 위치 표시
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            mCurrentLocation = location;
                            showCurrentLocationCircle();
                        }
                    });
        }

        // Firebase에서 데이터를 불러와 북마크된 마커 추가
        addBookmarkMarkers();
    }

    private void clearSearchMarkers() {
        for (Marker marker : searchMarkerList) {
            marker.setMap(null);
        }
        searchMarkerList.clear();
    }

    private void searchRestaurants(String query) {
        Call<SearchResponse> call = mNaverApiService.searchRestaurants(query, 10, 1, "random", SEARCH_CLIENT_ID, SEARCH_CLIENT_SECRET);
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

                        // 지도에 마커 추가
                        addMarkerForRestaurant(restaurant, false); // 검색된 마커는 북마크 아님
                    }

                    // RecyclerView에 검색 결과 표시
                    mAdapter.setRestaurants(restaurants);
                    slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
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

    private void showCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("카테고리 선택");
        builder.setItems(new String[]{"한식", "중식", "일식"}, (dialog, which) -> {
            String category = which == 0 ? "한식" : which == 1 ? "중식" : "일식";
            filterRestaurantsByCategory(category);
        });
        builder.show();
    }

    private void filterRestaurantsByCategory(String category) {
        // Firebase에서 해당 카테고리에 속하는 음식점을 불러와 지도에 마커로 표시
        mDatabase.orderByChild("category").equalTo(category).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                clearSearchMarkers(); // 기존 마커 제거
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Restaurant restaurant = dataSnapshot.getValue(Restaurant.class);
                    if (restaurant != null) {
                        addMarkerForRestaurant(restaurant, false); // 검색된 마커는 북마크 아님
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load restaurants", error.toException());
            }
        });
    }

    private void addMarkerForRestaurant(Restaurant restaurant, boolean isBookmark) {
        double latitude = restaurant.getLatitude() / 10.0;
        double longitude = restaurant.getLongitude() / 10.0;
        LatLng latLng = new LatLng(latitude, longitude);

        Marker marker = new Marker();
        marker.setPosition(latLng);
        marker.setMap(mNaverMap);

        if (isBookmark) {
            bookmarkMarkerList.add(marker);
        } else {
            searchMarkerList.add(marker);
        }

        marker.setOnClickListener(overlay -> {
            showRestaurantInfo(restaurant);
            moveCameraToMarker(marker);
            showDistanceAndPathToMarker(marker);
            return true;
        });
    }

    private void addBookmarkMarkers() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference bookmarkRef = FirebaseDatabase.getInstance().getReference("bookmarks").child(user.getUid());
        bookmarkRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Restaurant restaurant = dataSnapshot.getValue(Restaurant.class);
                    if (restaurant != null) {
                        addMarkerForRestaurant(restaurant, true); // 북마크된 마커 추가
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load bookmarks", error.toException());
            }
        });
    }

    private void showRestaurantInfo(Restaurant restaurant) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.restaurant_info, null);
        builder.setView(dialogView);

        TextView foodTitle = dialogView.findViewById(R.id.getFoodTitle);
        TextView foodAddress = dialogView.findViewById(R.id.getFoodAddress);
        TextView foodCategory = dialogView.findViewById(R.id.getFoodCategory);
        Button buttonBookmark = dialogView.findViewById(R.id.buttonBookmark);
        Button buttonViewReviews = dialogView.findViewById(R.id.buttonViewReviews);

        foodTitle.setText(restaurant.getTitle());
        foodAddress.setText(restaurant.getAddress());
        foodCategory.setText(restaurant.getCategory());

        AlertDialog dialog = builder.create();

        buttonBookmark.setOnClickListener(v -> {
            // 북마크 추가/삭제 기능 구현
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                DatabaseReference bookmarkRef = FirebaseDatabase.getInstance().getReference("bookmarks").child(user.getUid()).child(restaurant.getId());
                bookmarkRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            bookmarkRef.removeValue();
                            Toast.makeText(MainActivity.this, "북마크가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            removeBookmarkMarker(restaurant.getId()); // 북마크 삭제 시 마커 제거
                        } else {
                            bookmarkRef.setValue(restaurant);
                            Toast.makeText(MainActivity.this, "북마크가 추가되었습니다.", Toast.LENGTH_SHORT).show();
                            addMarkerForRestaurant(restaurant, true); // 북마크 추가 시 마커 추가
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Failed to update bookmark", error.toException());
                    }
                });
            } else {
                Toast.makeText(MainActivity.this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonViewReviews.setOnClickListener(v -> {
            // 리뷰 보기 기능 구현
            Intent intent = new Intent(MainActivity.this, RestaurantReviewsActivity.class);
            intent.putExtra("restaurantId", restaurant.getId());
            startActivity(intent);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void removeBookmarkMarker(String restaurantId) {
        for (Marker marker : bookmarkMarkerList) {
            if (marker.getTag() != null && marker.getTag().equals(restaurantId)) {
                marker.setMap(null);
                bookmarkMarkerList.remove(marker);
                break;
            }
        }
    }

    private void moveCameraToMarker(Marker marker) {
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(marker.getPosition());
        mNaverMap.moveCamera(cameraUpdate);
    }

    private void showDistanceAndPathToMarker(Marker marker) {
        if (mCurrentLocation == null) return;

        LatLng markerPosition = marker.getPosition();
        String origin = mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude();
        String destination = markerPosition.latitude + "," + markerPosition.longitude;

        Log.d("API Request", "origin: " + origin + ", destination: " + destination);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GoogleMapsApiService service = retrofit.create(GoogleMapsApiService.class);
        Call<DirectionsResponse> call = service.getWalkingDirections(origin, destination, "transit", "REDACTED");

        call.enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DirectionsResponse directionsResponse = response.body();
                    Log.d("API Response", new Gson().toJson(directionsResponse));

                    if (directionsResponse.routes != null && !directionsResponse.routes.isEmpty()) {
                        List<LatLng> path = PolyUtil.decode(directionsResponse.routes.get(0).overview_polyline.points);

                        // 기존 오버레이 제거
                        if (pathOverlay != null) {
                            pathOverlay.setMap(null);
                        }

                        // 거리 및 예상 시간 계산
                        double totalDistance = directionsResponse.routes.get(0).legs.get(0).distance.value / 1000.0; // km로 변환
                        double totalDuration = directionsResponse.routes.get(0).legs.get(0).duration.value / 3600.0; // 시간으로 변환

                        if (totalDuration > 1) {
                            Toast.makeText(MainActivity.this, "너무 멀어요!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String distanceText = totalDistance < 1 ? String.format("%dm", (int) (totalDistance * 1000))
                                : String.format("%.0fkm", totalDistance);

                        Toast.makeText(MainActivity.this, "거리: " + distanceText, Toast.LENGTH_SHORT).show();

                        // 경로 표시
                        pathOverlay = new PathOverlay();
                        pathOverlay.setCoords(path);
                        pathOverlay.setWidth(12); // 흰색 선 두께 조절
                        pathOverlay.setColor(Color.WHITE); // 흰색 바탕
                        pathOverlay.setMap(mNaverMap);

                    } else {
                        Log.e("API Error", "No routes found in response: " + new Gson().toJson(directionsResponse));
                        Toast.makeText(MainActivity.this, "경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("API Error", "Response Code: " + response.code() + ", Message: " + response.message());
                    Toast.makeText(MainActivity.this, "경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Log.e("API Failure", t.getMessage(), t);
                Toast.makeText(MainActivity.this, "경로 찾기 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCurrentLocationCircle() {
        if (circleOverlay != null) {
            circleOverlay.setMap(null);
        }

        LatLng currentLatLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

        circleOverlay = new CircleOverlay();
        circleOverlay.setCenter(currentLatLng);
        circleOverlay.setRadius(1000);
        circleOverlay.setColor(Color.parseColor("#220000FF")); // 반투명한 파란색
        circleOverlay.setMap(mNaverMap);
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
