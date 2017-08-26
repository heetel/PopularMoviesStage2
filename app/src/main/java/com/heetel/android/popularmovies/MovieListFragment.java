package com.heetel.android.popularmovies;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.heetel.android.popularmovies.data.MovieContract;
import com.heetel.android.popularmovies.utilities.ListUtil;
import com.heetel.android.popularmovies.utilities.NetworkUtils;

import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Julian Heetel on 25.08.2017.
 *
 */

public class MovieListFragment extends Fragment
        implements MovieAdapter.ListItemCallbackListener,
        LoaderManager.LoaderCallbacks<ArrayList<ContentValues>> {

    private static final String TAG = MovieListFragment.class.getSimpleName();
    //API provides 20 results per page
    private static final int RESULTS_PER_PAGE = 20;

    private static int page = 1;

    private int index, currentScrollPosition, restoredScroll;
    private ProgressBar loadingIndicator;
    private MovieAdapter adapter;
    private GridLayoutManager layoutManager;
    private RecyclerView recyclerView;


    public static MovieListFragment newInstance(int index) {
        MovieListFragment movieListFragment = new MovieListFragment();
        Bundle b = new Bundle();
        b.putInt("index", index);
        movieListFragment.setArguments(b);
        return movieListFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        index = getArguments().getInt("index");

        Log.e(TAG, "onCreateView() : " + index + " | id : " + this);

        View view = inflater.inflate(R.layout.fragment_popular, container, false);
        initView(view);
        loadFromDB();

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("scroll")) {
                restoredScroll = savedInstanceState.getInt("scroll");
                Log.i(TAG, "restored scroll : " + currentScrollPosition);
            }
        }

        return view;
    }

    @Override
    public void onResume() {
        if (index == 2) {
            Log.i(TAG, "onResume: refresh favourites");
            loadFromDB();
        }
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("scroll", currentScrollPosition);
        Log.i(TAG, "saved scroll : " + currentScrollPosition);
        super.onSaveInstanceState(outState);
    }

    private void initView(View view) {
        // initalize RecyclerView
        recyclerView = (RecyclerView) view.findViewById(R.id.rv_movies);
        //RecyclerView displays 2 or 3 Columns depending on devices orientation
        int columnCount = 2;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            columnCount = 3;

        layoutManager = new GridLayoutManager(getActivity(), columnCount);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        adapter = new MovieAdapter(this, getActivity());
        recyclerView.setAdapter(adapter);

        loadingIndicator = (ProgressBar) view.findViewById(R.id.loading_indicator);
    }

    @Override
    public void onListItemClick(String movieId, String title, int position) {
        Intent intent = new Intent(getActivity(), DetailActivity.class);

        intent.putExtra(DetailActivity.INTENT_MOVIE_ID_KEY, movieId);

        intent.putExtra(DetailActivity.INTENT_TABLE_KEY, index);

        intent.putExtra(DetailActivity.INTENT_MOVIE_TITLE_KEY, title);

        ActivityOptionsCompat activityOptionsCompat =
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(), layoutManager.findViewByPosition(position), "poster");
        ActivityCompat.startActivity(getActivity(), intent, activityOptionsCompat.toBundle());
    }

    @Override
    public void onLoadMore() {
        if (index == 2) return;
        loadFromNetwork();
    }

    @Override
    public void updatePosition(int position) {
        Log.i(TAG, "scroll : " + position);
        currentScrollPosition = position;
    }

    /**
     * Query data from ContentProvider in background task and
     * Update the UI.
     */
    private void loadFromDB() {
        new MovieFromDBTask().execute();
    }

    /**
     * Load data from API in background task into ContentProvider
     */
    private void loadFromNetwork() {

        //check internet connection
        if (!isOnline()) {
            showNoConnectionDialog();
        } else {
            //Put page into Bundle
            Bundle bundle = new Bundle();
            bundle.putInt("page", page);

            //Initialize/start Loader
            int NETWORK_LOADER_ID = 221;
            Loader<Integer> movieLoader = getActivity().getLoaderManager().getLoader(NETWORK_LOADER_ID);
            if (movieLoader == null) {
                getActivity().getLoaderManager().initLoader(NETWORK_LOADER_ID, bundle, this);
            } else {
                getActivity().getLoaderManager().restartLoader(NETWORK_LOADER_ID, bundle, this);
            }
        }
    }

    /**
     * Check if device is connected to the internet.
     *
     * @return true if device is connected to the internet, else false.
     */
    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /**
     * Display Dialog to indicate the user, that the device is offline.
     */
    private void showNoConnectionDialog() {
        String noConnection = getResources().getString(R.string.no_connection);
        String tryAgain = getResources().getString(R.string.try_again);
        String requestInternet = getResources().getString(R.string.request_internet_connection);
        new AlertDialog.Builder(getActivity())
                .setTitle(noConnection)
                .setMessage(requestInternet)
                .setPositiveButton(tryAgain, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        loadFromDB();
                    }
                }).show();
    }

    public void refresh() {
        adapter.setMovies(null);
        page = 1;
        loadFromDB();
    }

    private class MovieFromDBTask extends AsyncTask<Uri, Void, Cursor> {

        @Override
        protected void onPreExecute() {
            loadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected Cursor doInBackground(Uri... params) {
            if (getActivity() == null) return null;
            switch (index) {
                case 0:
                    return getActivity().getContentResolver().query(
                            MovieContract.MovieEntry.CONTENT_URI,
                            null,
                            null,
                            null,
                            null
                    );
                case 1:
                    return getActivity().getContentResolver().query(
                            MovieContract.MovieEntry.CONTENT_URI_TOP_RATED,
                            null,
                            null,
                            null,
                            null
                    );
                case 2:
                    // query in reverse order (DESC) to display most recent added favourite first
                    return getActivity().getContentResolver().query(
                            MovieContract.MovieEntry.CONTENT_URI_FAVOURITES,
                            null,
                            null,
                            null,
                            "_ID DESC"
                    );
                default:
                    return null;
            }
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            loadingIndicator.setVisibility(View.INVISIBLE);
            adapter.setMovies(cursor);
            if (restoredScroll != 0) {
                recyclerView.scrollToPosition(restoredScroll);
//                restoredScroll = 0;
            }
            if (cursor != null && index < 2) {
                //calculate page number to load from API on next load.
                page = (cursor.getCount() / RESULTS_PER_PAGE);
                page++;
                Log.i(TAG, "page updated to " + page);

                // if loaded data from ContentProvider is empty, load data from API
                if (page == 1) {
                    //DB is still empty
                    loadFromNetwork();
                }
            }
        }
    }

    @Override
    public Loader<ArrayList<ContentValues>> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<ArrayList<ContentValues>>(getActivity()) {

            //Cached data
            ArrayList<ContentValues> cachedMovies;

            @Override
            protected void onStartLoading() {
                if (args == null) return;

                if (cachedMovies != null) {
                    deliverResult(cachedMovies);
                } else {
                    loadingIndicator.setVisibility(View.VISIBLE);
                    forceLoad();
                }
            }

            @Override
            public ArrayList<ContentValues> loadInBackground() {
                //check for page number
                if (!args.containsKey("page"))
                    return null;

                int page = args.getInt("page");

                //build URL and get Response
                URL movieRequestUrl = NetworkUtils.buildUrl(page);
                try {
                    return NetworkUtils.getMoviesFromHttpUrl(movieRequestUrl);
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
        loadingIndicator.setVisibility(View.INVISIBLE);

        //insert data into ContentProvider
        if (movies != null) {
            //bulkInsert movies to DB
            ContentValues[] contentValues = ListUtil.makeContentValuesArray(movies);
            int rowsInserted = 0;
            switch (index) {
                case 0:
                    rowsInserted = getActivity().getContentResolver()
                            .bulkInsert(MovieContract.MovieEntry.CONTENT_URI, contentValues);
                    break;
                case 1:
                    rowsInserted = getActivity().getContentResolver().bulkInsert(
                            MovieContract.MovieEntry.CONTENT_URI_TOP_RATED, contentValues);
                    break;
            }

            Log.i(TAG, rowsInserted + " rows inserted to DB.");

            //update the UI
            loadFromDB();
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<ContentValues>> loader) {
    }
}
