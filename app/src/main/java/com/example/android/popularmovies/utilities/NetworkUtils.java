/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.popularmovies.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.example.android.popularmovies.data.MovieContract.MovieEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

/**
 * These utilities will be used to communicate with the network.
 *
 * Get json from following Uri
 *
 * https://api.themoviedb.org/3/movie/popular?api_key=&language=en-US&page=1
 *
 */
public class NetworkUtils {

    private static final String TAG = NetworkUtils.class.getSimpleName();

    private final static String THEMOVIEDB_BASE_URL_POPULAR =
            "https://api.themoviedb.org/3/movie/popular";
    private final static String THEMOVIEDB_BASE_URL_TOP_RATED =
            "https://api.themoviedb.org/3/movie/top_rated";
    private final static String THEMOVIEDB_BASE_URL_MOVIE =
            "https://api.themoviedb.org/3/movie/";

    private final static String THEMOVIEDB_PATH_VIDEOS = "videos";
    private final static String THEMOVIEDB_PATH_REVIEWS = "reviews";

    //used to switch between upper urls.
    private static String active_url = THEMOVIEDB_BASE_URL_POPULAR;

    private final static String PARAM_API = "api_key";
    private final static String API = "";//insert your API Key here.
    private final static String LANGUAGE_KEY = "language";
    private final static String PAGE_KEY = "page";

    public static void setPopular() {
        active_url = THEMOVIEDB_BASE_URL_POPULAR;
    }

    public static void setTopRated() {
        active_url = THEMOVIEDB_BASE_URL_TOP_RATED;
    }

    public static boolean isPopular() {
        return active_url.equals(THEMOVIEDB_BASE_URL_POPULAR);
    }

    private static URL buildVideosUrl(String movidId) {
        if (TextUtils.isEmpty(API))
            return null;

        Uri builtUri = Uri.parse(THEMOVIEDB_BASE_URL_MOVIE).buildUpon()
                .appendPath(movidId)
                .appendPath(THEMOVIEDB_PATH_VIDEOS)
                .appendQueryParameter(PARAM_API, API)
                .appendQueryParameter(LANGUAGE_KEY, getLanguage()).build();

        try {
            return new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static URL buildReviewsUrl(String movieId) {
        if (TextUtils.isEmpty(API)) return null;

        Uri builtUri = Uri.parse(THEMOVIEDB_BASE_URL_MOVIE).buildUpon()
                .appendPath(movieId)
                .appendPath(THEMOVIEDB_PATH_REVIEWS)
                .appendQueryParameter(PARAM_API, API)
                .appendQueryParameter(LANGUAGE_KEY, getLanguage()).build();

        try {
            return new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ContentValues getDetailsFromMovieId(String movieId)
            throws IOException{
        URL videosUrl = buildVideosUrl(movieId);
        if (videosUrl == null) return null;

        ContentValues details = null;

        HttpURLConnection connection = (HttpURLConnection) videosUrl.openConnection();
        try {
            InputStream in = connection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            if (scanner.hasNext()) {
                String scan = scanner.next();
                details = getVideosFromJSON(scan);
            }
        } finally {
            connection.disconnect();
        }

        //load reviews and append to ContentValues

        URL reviewsUrl = buildReviewsUrl(movieId);
        if (reviewsUrl == null) return details;
        Log.i(TAG, reviewsUrl.toString());

        connection = (HttpURLConnection) reviewsUrl.openConnection();
        try {
            InputStream in = connection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            if (scanner.hasNext()) {
                String scan = scanner.next();
                ContentValues values = appendReviewsToContentValues(details, scan);
//                Log.i(TAG, values.getAsString(MovieEntry.COLUMN_REVIEWS_AUTHORS));
                return values;
            }
        } finally {
            connection.disconnect();
        }

        return details;
    }

    private static ContentValues getVideosFromJSON(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray results = jsonObject.getJSONArray("results");

            ContentValues contentValues = new ContentValues();

            String[] names = new String[results.length()];
            String[] keys = new String[results.length()];

            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);

                String name = result.getString("name");
                String key = result.getString("key");
                names[i] = name;
                keys[i] = key;
            }
            String delimiter = ",";
            contentValues.put(MovieEntry.COLUMN_VIDEOS_NAMES,
                    ListUtil.convertArrayToString(names, delimiter));
            contentValues.put(MovieEntry.COLUMN_VIDEOS_KEYS,
                    ListUtil.convertArrayToString(keys, delimiter));

            return contentValues;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static ContentValues appendReviewsToContentValues(ContentValues values, String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject((jsonString));
            JSONArray results = jsonObject.getJSONArray("results");

            String[] authors = new String[results.length()];
            String[] contents = new String[results.length()];

            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);

                String author = result.getString("author");
                Log.i(TAG, "author: " +author);
                String content = result.getString("content");
                authors[i] = author;
                contents[i] = content;
            }
            values.put(MovieEntry.COLUMN_REVIEWS_AUTHORS,
                    ListUtil.convertArrayToString(authors, ListUtil.DELIMITER));
            Log.i(TAG, ListUtil.convertArrayToString(authors, ListUtil.DELIMITER));
            values.put(MovieEntry.COLUMN_REVIEWS_CONTENTS,
                    ListUtil.convertArrayToString(contents, ListUtil.DELIMITER));
            return values;
        } catch (JSONException e) {
            e.printStackTrace();
            return values;
        }
    }

    /**
     * Builds the URL used to query TheMovieDB.
     *
     * @return The URL to use to query the movie server.
     */
    public static URL buildUrl(int page) {

        if (TextUtils.isEmpty(API))
            return null;

        String language = getLanguage();
        Log.i("lang: ", language);
        Uri builtUri = Uri.parse(active_url).buildUpon()
                .appendQueryParameter(PARAM_API, API)
                .appendQueryParameter(LANGUAGE_KEY, language)
                .appendQueryParameter(PAGE_KEY, String.valueOf(page))
                .build();
        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    private static String getLanguage() {
        return Locale.getDefault().getLanguage();
    }

    /**
     * This method returns the Movies from result from the HTTP response.
     *
     * @param url The URL to fetch the HTTP response from.
     * @return The Movies as an Array of the HTTP response.
     * @throws IOException Related to network and stream reading
     */
    public static ArrayList<ContentValues> getMoviesFromHttpUrl(Context context, URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            if (scanner.hasNext()) {
                String scan = scanner.next();
                return getMovieArrayFromJSON(context, scan);
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    /**
     * This Method makes an ArrayList of Movie objects from a json String.
     *
     * @param jsonString the json to parse the movies
     * @return ArrayList of Movie objects containing title, date, etc.
     */
    private static ArrayList<ContentValues> getMovieArrayFromJSON(Context context, String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray results = jsonObject.getJSONArray("results");

            ArrayList<ContentValues> movies = new ArrayList<>();

            for (int i = 0; i < results.length(); i++) {
                //get movie-object from results array.
                JSONObject m = results.getJSONObject(i);

                // get Strings from movie-object.
                int movieID = m.getInt("id");
                String title = m.getString("title");
                String originalTitle = m.getString("original_title");
                String releaseDate = m.getString("release_date");
                String voteAverage = m.getString("vote_average");
                String overview = m.getString("overview");
                String posterPath = m.getString("poster_path");
                String backdropPath = m.getString("backdrop_path");

                ContentValues contentValues = new ContentValues();
                contentValues.put(MovieEntry.COLUMN_MOVIE_ID, movieID);
                contentValues.put(MovieEntry.COLUMN_TITLE, title);
                contentValues.put(MovieEntry.COLUMN_ORIGINAL_TITLE, originalTitle);
                contentValues.put(MovieEntry.COLUMN_RELEASE_DATE, releaseDate);
                contentValues.put(MovieEntry.COLUMN_VOTE_AVERAGE, voteAverage);
                contentValues.put(MovieEntry.COLUMN_OVERVIEW, overview);
                contentValues.put(MovieEntry.COLUMN_POSTER_PATH, posterPath);
                contentValues.put(MovieEntry.COLUMN_BACKDROP_PATH, backdropPath);

                movies.add(contentValues);
            }

            return movies;

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

}