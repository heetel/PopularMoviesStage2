package com.heetel.android.popularmovies.search;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;

import com.heetel.android.popularmovies.utilities.NetworkUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Julian Heetel on 30.07.2017.
 *
 */

public class SearchHelper implements LoaderManager.LoaderCallbacks<ArrayList<ContentValues>>{

    private static final String KEY_QUERY = "key_query";
    private static final int LOADER_ID = 421;
    private static final String TAG = SearchHelper.class.getSimpleName();

    private Context context;
    private String query;
    private SearchCallbacks searchCallbacks;

    SearchHelper(Context context, String query, SearchCallbacks searchCallbacks) {
        this.context = context;
        this.query = query;
        this.searchCallbacks = searchCallbacks;
    }

    interface SearchCallbacks {
        void onSearch(ArrayList<ContentValues> results);
    }

    public void search() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_QUERY, query);
        Loader<Integer> queryLoader = ((Activity) context).getLoaderManager().getLoader(LOADER_ID);
        if (queryLoader == null) {
            ((Activity) context).getLoaderManager().initLoader(LOADER_ID, bundle, this);
        } else {
            ((Activity) context).getLoaderManager().restartLoader(LOADER_ID, bundle, this);
        }
    }

    public Loader<ArrayList<ContentValues>> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<ArrayList<ContentValues>>(context) {

            ArrayList<ContentValues> cachedData;

            @Override
            protected void onStartLoading() {
                if (args == null) return;

                if (cachedData != null) {
                    deliverResult(cachedData);
                } else {
                    forceLoad();
                }
            }

            @Override
            public ArrayList<ContentValues> loadInBackground() {
                if (!args.containsKey(KEY_QUERY)) return null;

                String query = args.getString(KEY_QUERY);

                URL queryUrl = NetworkUtils.buildSearchUrl(query);

                try {
                    return NetworkUtils.getMoviesFromHttpUrl(queryUrl);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public void deliverResult(ArrayList<ContentValues> data) {
                cachedData = data;
                super.deliverResult(data);
            }
        };
    }

    @Override
    public void onLoadFinished(android.content.Loader<ArrayList<ContentValues>> loader, ArrayList<ContentValues> data) {
        searchCallbacks.onSearch(data);
    }

    @Override
    public void onLoaderReset(android.content.Loader<ArrayList<ContentValues>> loader) {

    }


}
