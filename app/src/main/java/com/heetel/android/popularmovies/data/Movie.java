package com.heetel.android.popularmovies.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.heetel.android.popularmovies.utilities.ListUtil;

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
        this.videosKeys = ListUtil.convertStringToArray(data.getAsString(
                MovieContract.MovieEntry.COLUMN_VIDEOS_KEYS), ListUtil.DELIMITER);
        // TODO fertig machen
    }

    public Movie(Cursor cursor) {
        this.title = cursor.getString(cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_TITLE));
        this.movieId = cursor.getString(cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_MOVIE_ID));
        this.releaseDate = cursor.getString(cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_RELEASE_DATE));
        this.voteAverage = cursor.getString(cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE));
        this.originalTitle = cursor.getString(cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_ORIGINAL_TITLE));
        this.backdropPath = cursor.getString(cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_BACKDROP_PATH));
        this.posterPath = cursor.getString(cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_POSTER_PATH));
        this.overview = cursor.getString(cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_OVERVIEW));
        this.videosNames = ListUtil.convertStringToArray(cursor.getString(
                cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_VIDEOS_NAMES)), ListUtil.DELIMITER);
        this.videosKeys = ListUtil.convertStringToArray(cursor.getString(
                cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_VIDEOS_KEYS)), ListUtil.DELIMITER);
        this.reviewsAuthors = ListUtil.convertStringToArray(cursor.getString(
                cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_REVIEWS_AUTHORS)), ListUtil.DELIMITER);
        this.reviewsContents = ListUtil.convertStringToArray(cursor.getString(
                cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_REVIEWS_CONTENTS)), ListUtil.DELIMITER);
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

    public ContentValues getContentValues() {
        ContentValues mValues = new ContentValues();

        mValues = new ContentValues();
        mValues.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID, movieId);
        mValues.put(MovieContract.MovieEntry.COLUMN_TITLE, title);
        mValues.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE, releaseDate);
        mValues.put(MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE, voteAverage);
        mValues.put(MovieContract.MovieEntry.COLUMN_ORIGINAL_TITLE, originalTitle);
        mValues.put(MovieContract.MovieEntry.COLUMN_BACKDROP_PATH, backdropPath);
        mValues.put(MovieContract.MovieEntry.COLUMN_POSTER_PATH, posterPath);
        mValues.put(MovieContract.MovieEntry.COLUMN_OVERVIEW, overview);
        mValues.put(MovieContract.MovieEntry.COLUMN_VIDEOS_NAMES, ListUtil.convertArrayToString(videosNames, ","));
        mValues.put(MovieContract.MovieEntry.COLUMN_VIDEOS_KEYS, ListUtil.convertArrayToString(videosKeys, ","));
        mValues.put(MovieContract.MovieEntry.COLUMN_REVIEWS_AUTHORS, ListUtil.convertArrayToString(reviewsAuthors, ","));
        mValues.put(MovieContract.MovieEntry.COLUMN_REVIEWS_CONTENTS, ListUtil.convertArrayToString(reviewsContents, ","));

        return mValues;
    }
}
