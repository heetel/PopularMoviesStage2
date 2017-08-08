package com.heetel.android.popularmovies.data;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Julian Heetel on 09.03.2017.
 *
 */

public class MovieContract {

    static final String AUTHORITY = "com.heetel.android.popularmovies";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    static final String PATH_MOVIES = "movies";

    static final String PATH_TOP_RATED = "top-rated";

    static final String PATH_FAVOURITES = "favourites";

    static final String PATH_SEARCH_RESULTS = "search-results";

    public static final class MovieEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MOVIES).build();

        public static final Uri CONTENT_URI_TOP_RATED =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TOP_RATED).build();

        public static final Uri CONTENT_URI_SEARCH_RESULTS =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_FAVOURITES).build();

        public static final Uri CONTENT_URI_FAVOURITES =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SEARCH_RESULTS).build();

        static final String TABLE_NAME = "movies";
        static final String TABLE_NAME_TOP_RATED = "movies_top_rated";
        static final String TABLE_NAME_FAVOURITES = "movies_favourites";
        static final String TABLE_NAME_SEARCH_RESULTS = "movies_search_results";

        static final String COLUMN_CREATE_DATE = "create_date";
        public static final String COLUMN_MOVIE_ID = "movie_id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_ORIGINAL_TITLE = "original_title";
        public static final String COLUMN_OVERVIEW = "overview";
        public static final String COLUMN_RELEASE_DATE = "release_date";
        public static final String COLUMN_VOTE_AVERAGE = "vote_average";
        public static final String COLUMN_POSTER_PATH = "poster_path";
        public static final String COLUMN_BACKDROP_PATH = "backdrop_path";
        public static final String COLUMN_VIDEOS_NAMES = "videos_names";
        public static final String COLUMN_VIDEOS_KEYS = "videos_keys";
        public static final String COLUMN_REVIEWS_AUTHORS = "reviews_authors";
        public static final String COLUMN_REVIEWS_CONTENTS = "reviews_contents";

    }

}
