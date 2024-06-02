package com.example.project_bobtong;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.ViewHolder> {

    private List<Restaurant> restaurants = new ArrayList<>();

    public void setRestaurants(List<Restaurant> restaurants) {
        this.restaurants = restaurants;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_restaurant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Restaurant restaurant = restaurants.get(position);
        if (restaurant != null) {
            String title = restaurant.getTitle();
            if (title != null) {
                title = title.replaceAll("<b>", "").replaceAll("</b>", "");
                holder.textViewName.setText(title);
            } else {
                holder.textViewName.setText("Unknown Restaurant");
            }
            holder.textViewAddress.setText(restaurant.getAddress());
        }
    }

    @Override
    public int getItemCount() {
        return restaurants.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewName;
        private TextView textViewAddress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewAddress = itemView.findViewById(R.id.textViewAddress);
        }

        public void bind(Restaurant restaurant) {
            textViewName.setText(restaurant.getTitle());
            textViewAddress.setText(restaurant.getAddress());
        }
    }
}
