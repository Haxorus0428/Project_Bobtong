package com.example.project_bobtong;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.List;

public class ReviewAdapter extends ArrayAdapter<Review> {

    public ReviewAdapter(@NonNull Context context, @NonNull List<Review> objects) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_review, parent, false);
        }

        Review review = getItem(position);

        TextView textViewUserId = convertView.findViewById(R.id.textViewUserId);
        TextView textViewReviewText = convertView.findViewById(R.id.textViewReviewText);
        ImageView imageViewReview = convertView.findViewById(R.id.imageViewReview);

        if (review != null) {
            textViewUserId.setText(review.getUserId().substring(0, 4) + "****");
            textViewReviewText.setText(review.getReviewText());

            // 이미지 URL이 있을 경우, 이미지를 로드하고 없으면 ImageView를 숨김
            if (review.getImageUrl() != null) {
                imageViewReview.setVisibility(View.VISIBLE);
                Glide.with(getContext())
                        .load(review.getImageUrl())
                        .into(imageViewReview);
            } else {
                imageViewReview.setVisibility(View.GONE);  // 이미지가 없을 경우 숨김
            }
        }

        return convertView;
    }
}
