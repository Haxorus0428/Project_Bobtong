// Restaurant.java

package com.example.project_bobtong;

import com.google.gson.annotations.SerializedName;

public class Restaurant {

    @SerializedName("title")
    private String name;

    public String getName() {
        return name;
    }
}
