package com.example.android.popularmovies.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;

import java.net.URL;
import java.util.ArrayList;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import com.example.android.popularmovies.data.MovieContract;

/**
 * Created by Julian Heetel on 09.03.2017.
 */

public class NetworkLoader implements LoaderManager.LoaderCallbacks<ArrayList<ContentValues>> {

    private static final String TAG = NetworkLoader.class.getSimpleName();

    private Context mContext;

    private static int NETWORK_LOADER_ID = 221;
    public static final String PAGE_KEY = "page-key";

    //TODO Check if online

    private final CallbackListener mCallbackListener;

    public interface CallbackListener {
        void onLoadingFinished();
    }

    public NetworkLoader(CallbackListener listener, LoaderManager loaderManager, int page) {
        mContext = (Context) listener;
        mCallbackListener = listener;

        //Put page into Bundle
        Bundle bundle = new Bundle();
        bundle.putInt(PAGE_KEY, page);

        //Initialize/start Loader
        Loader<Integer> movieLoader = loaderManager.getLoader(NETWORK_LOADER_ID);
        if (movieLoader == null) {
            loaderManager.initLoader(NETWORK_LOADER_ID, bundle, this);
        } else {
            loaderManager.restartLoader(NETWORK_LOADER_ID, bundle, this);
        }
    }

    @Override
    public Loader<ArrayList<ContentValues>> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<ArrayList<ContentValues>>(mContext) {

            //Cached data
            ArrayList<ContentValues> cachedMovies;

            @Override
            protected void onStartLoading() {
                if (args == null)
                    return;

                if (cachedMovies != null) {
                    deliverResult(cachedMovies);
                } else {
                    forceLoad();
                }
            }

            @Override
            public ArrayList<ContentValues> loadInBackground() {
                //check for page number
                if (!args.containsKey(PAGE_KEY))
                    return null;

                int page = args.getInt(PAGE_KEY);

                //build URL and get Response
                URL movieRequestUrl = NetworkUtils.buildUrl(page);
                try {
                    return NetworkUtils.getMoviesFromHttpUrl(mContext, movieRequestUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public void deliverResult(ArrayList<ContentValues> data) {
                cachedMovies = data;
                super.deliverResult(data);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<ContentValues>> loader, ArrayList<ContentValues> movies) {

        //bulkInsert movies to DB
        ContentValues[] contentValues = ListUtil.makeContentValuesArray(movies);

        int rowsInserted = mContext.getContentResolver()
                .bulkInsert(MovieContract.MovieEntry.CONTENT_URI, contentValues);

        Log.i(TAG, rowsInserted + " rows inserted to DB.");

        mCallbackListener.onLoadingFinished();
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<ContentValues>> loader) {

    }
}
