package com.example.project_bobtong;

// NaverApiService.java

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface NaverApiService {

    @GET("search/local.json")
    Call<SearchResponse> searchRestaurants(
            @Query("query") String query,
            @Header("X-Naver-Client-Id") String clientId,
            @Header("X-Naver-Client-Secret") String clientSecret
    );
}

