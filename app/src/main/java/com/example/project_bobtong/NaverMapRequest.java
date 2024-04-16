package com.example.project_bobtong;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NaverMapRequest {

    public static String BASE_URL = "https://openapi.naver.com/v1/";

    private static Retrofit retrofit;
    public static Retrofit getClient() {

        if(retrofit == null){
            retrofit = new Retrofit.Builder() // retrofit 객체 생성
                    .baseUrl(BASE_URL) // BASE_URL로 통신
                    .addConverterFactory(GsonConverterFactory.create()) // gson-converter로 데이터 parsing
                    .build();
        }
        return retrofit;
    }
}