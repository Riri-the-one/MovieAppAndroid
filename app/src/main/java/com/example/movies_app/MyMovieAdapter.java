package com.example.movies_app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter de la liste des films populaires (TMDB).
 */
public class MyMovieAdapter extends RecyclerView.Adapter<MyMovieAdapter.ViewHolder> {

    private final Context context;
    private List<MyMovieData> movies = new ArrayList<>();

    public MyMovieAdapter(Context context) {
        this.context = context;
    }

    /** Remplace toute la liste (première page ou filtre recherche). */
    public void setMovies(List<MyMovieData> movies) {
        this.movies = movies != null ? new ArrayList<>(movies) : new ArrayList<>();
        notifyDataSetChanged();
    }

    /** Ajoute des films (scroll infini, pages suivantes). */
    public void appendMovies(List<MyMovieData> more) {
        if (more == null || more.isEmpty()) {
            return;
        }
        int start = movies.size();
        movies.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.movie_item_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyMovieData movie = movies.get(position);

        holder.textViewName.setText(movie.getTitle());
        holder.textViewDate.setText(movie.getReleaseYear());
        holder.textViewRatingBadge.setText(movie.getFormattedRating());

        // Affiche TMDB via Glide
        String posterUrl = TmdbApi.getPosterUrl(movie.getPosterPath());
        Glide.with(context)
                .load(posterUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.color.md_surface_variant)
                .error(R.color.md_surface_variant)
                .centerCrop()
                .into(holder.movieImage);

        holder.itemView.setOnClickListener(v -> openMovieDetail(movie));
    }

    /** Ouvre l'écran de détails avec toutes les données TMDB. */
    private void openMovieDetail(MyMovieData movie) {
        Intent intent = new Intent(context, MovieDetailActivity.class);
        intent.putExtra(MovieDetailActivity.EXTRA_MOVIE_ID, movie.getId());
        intent.putExtra(MovieDetailActivity.EXTRA_MOVIE_NAME, movie.getTitle());
        intent.putExtra(MovieDetailActivity.EXTRA_MOVIE_DATE, movie.getReleaseDate());
        intent.putExtra(MovieDetailActivity.EXTRA_MOVIE_OVERVIEW, movie.getOverview());
        intent.putExtra(MovieDetailActivity.EXTRA_MOVIE_RATING, movie.getVoteAverage());
        intent.putExtra(MovieDetailActivity.EXTRA_POSTER_PATH, movie.getPosterPath());
        intent.putExtra(MovieDetailActivity.EXTRA_BACKDROP_PATH, movie.getBackdropPath());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView movieImage;
        TextView textViewName;
        TextView textViewDate;
        TextView textViewRatingBadge;
        TextView textViewOverviewPreview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            movieImage = itemView.findViewById(R.id.imageView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewDate = itemView.findViewById(R.id.textViewDate);
            textViewRatingBadge = itemView.findViewById(R.id.textViewRatingBadge);
            textViewOverviewPreview = itemView.findViewById(R.id.textViewOverviewPreview);
        }
    }
}
