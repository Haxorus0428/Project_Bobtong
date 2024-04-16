package com.example.project_bobtong;

import com.example.project_bobtong.Restaurant;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SearchResponse {

    @SerializedName("items")
    private List<Restaurant> items;

    public List<Restaurant> getItems() {
        return items;
    }
}

