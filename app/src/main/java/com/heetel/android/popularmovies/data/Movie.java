package com.heetel.android.popularmovies.data;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Julian Heetel on 08.08.2017.
 */

public class Movie implements Parcelable {

    public String movieId;
    public String title;
    public String originalTitle;
    public String overview;
    public String releaseDate;
    public String voteAverage;
    public String posterPath;
    public String backdropPath;
    public String[] videosNames;
    public String[] videosKeys;
    public String[] reviewsAuthors;
    public String[] reviewsContents;

    public Movie(String movieId, String title, String originalTitle, String overview,
                 String releaseDate, String voteAverage, String posterPath, String backdropPath) {
        this.movieId = movieId;
        this.title = title;
        this.originalTitle = originalTitle;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.voteAverage = voteAverage;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
    }


    public Movie(String movieId, String title, String originalTitle, String overview,
                 String releaseDate, String voteAverage, String posterPath, String backdropPath,
                 String[] videosNames, String[] videosKeys, String[] reviewsAuthors,
                 String[] reviewsContents) {
        this.movieId = movieId;
        this.title = title;
        this.originalTitle = originalTitle;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.voteAverage = voteAverage;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.videosNames = videosNames;
        this.videosKeys = videosKeys;
        this.reviewsAuthors = reviewsAuthors;
        this.reviewsContents = reviewsContents;
    }

    public Movie(Parcel in) {
        this.movieId = in.readString();
        this.title = in.readString();
        this.originalTitle = in.readString();
        this.overview = in.readString();
        this.releaseDate = in.readString();
        this.voteAverage = in.readString();
        this.posterPath = in.readString();
        this.backdropPath = in.readString();
//        this.videosNames = in.readStringArray();
//        this.videosKeys = in.readString();
        // to be continued...
    }

    public Movie(ContentValues data) {
        this.movieId = data.getAsString(MovieContract.MovieEntry.COLUMN_MOVIE_ID);
        this.title = data.getAsString(MovieContract.MovieEntry.COLUMN_TITLE);
        this.originalTitle = data.getAsString(MovieContract.MovieEntry.COLUMN_ORIGINAL_TITLE);
        this.overview = data.getAsString(MovieContract.MovieEntry.COLUMN_OVERVIEW);
        this.releaseDate = data.getAsString(MovieContract.MovieEntry.COLUMN_RELEASE_DATE);
        this.voteAverage = data.getAsString(MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE);
        this.posterPath = data.getAsString(MovieContract.MovieEntry.COLUMN_POSTER_PATH);
        this.backdropPath = data.getAsString(MovieContract.MovieEntry.COLUMN_BACKDROP_PATH);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(movieId);
        dest.writeString(title);
        dest.writeString(originalTitle);
        dest.writeString(overview);
        dest.writeString(releaseDate);
        dest.writeString(voteAverage);
        dest.writeString(posterPath);
        dest.writeString(backdropPath);
    }

    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel source) {
            return new Movie(source);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };
}
