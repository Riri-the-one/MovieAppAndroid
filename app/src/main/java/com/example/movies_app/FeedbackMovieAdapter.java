package com.example.movies_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * Liste horizontale de films (profil : aimés / non aimés).
 */
public class FeedbackMovieAdapter extends RecyclerView.Adapter<FeedbackMovieAdapter.Holder> {

    private final List<FeedbackMovie> movies = new ArrayList<>();

    public void setMovies(List<FeedbackMovie> list) {
        movies.clear();
        if (list != null) {
            movies.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feedback_movie, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        FeedbackMovie movie = movies.get(position);
        holder.textTitle.setText(movie.getTitle());
        Glide.with(holder.itemView.getContext())
                .load(movie.getImageUrl())
                .placeholder(R.color.md_surface_variant)
                .error(R.color.md_surface_variant)
                .centerCrop()
                .into(holder.imagePoster);
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView imagePoster;
        final TextView textTitle;

        Holder(@NonNull View itemView) {
            super(itemView);
            imagePoster = itemView.findViewById(R.id.imageFeedbackPoster);
            textTitle = itemView.findViewById(R.id.textFeedbackTitle);
        }
    }
}
