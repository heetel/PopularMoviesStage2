package com.example.android.popularmovies;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.example.android.popularmovies.data.MovieContract;
import com.example.android.popularmovies.utilities.ListUtil;
import com.example.android.popularmovies.utilities.NetworkUtils;

import java.net.URL;
import java.util.ArrayList;

/**
 * API-Key:
 */
public class MainActivity extends AppCompatActivity
        implements MovieAdapter.ListItemCallbackListener,
        LoaderManager.LoaderCallbacks<ArrayList<ContentValues>> {

    private final String TAG = MainActivity.class.getSimpleName();

    private RecyclerView rvMovies;
    private ProgressBar pbLoadingIndicator;
    private Menu menu;
    MovieAdapter mAdapter;
    ArrayList<Movie> mMovies;
    private Context mContext;

    private static int page = 1;
    private static int NETWORK_LOADER_ID = 221;
    private static final String PAGE_KEY = "page-key";

    private static final int NUM_LIST_ITEMS = 20;
    private static final int RESULTS_PER_PAGE = 20;

    public static final int CODE_POPULAR = 1231;
    public static final int CODE_TOP_RATED = 1232;
    public static final int CODE_FAVOURITES = 1233;
    private static int sActiveTable = CODE_POPULAR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvMovies = (RecyclerView) findViewById(R.id.rv_movies);
        pbLoadingIndicator = (ProgressBar) findViewById(R.id.pb_loading_indicator);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        rvMovies.setLayoutManager(gridLayoutManager);

        rvMovies.setHasFixedSize(true);

        mAdapter = new MovieAdapter(NUM_LIST_ITEMS, this, MainActivity.this);
        rvMovies.setAdapter(mAdapter);

        //start at beginning
        page = 1;

        mContext = this;

        loadFromDB();
    }

    @Override
    public void onListItemClick(Cursor cursor, int clickedItemIndex) {
        Log.d(TAG, "onListItemClick called #" + clickedItemIndex);

        Context context = MainActivity.this;
        Intent intent = new Intent(context, DetailActivity.class);

        intent.putExtra(DetailActivity.INDEX_KEY, clickedItemIndex);

        intent.putExtra(DetailActivity.TABLE_KEY, sActiveTable);

        startActivity(intent);
    }

    /**
     * This method gets called when the User scrolls down to the end of RecyclerView
     *
     */
    @Override
    public void onLoadMore() {
//        page ++;
        loadFromNetwork();
    }



    private void loadFromDB() {
        Log.i(TAG, "loadFromDB called");
//        getSupportLoaderManager().restartLoader(MOVIE_FROM_DB_LOADER_ID, null, this);
        new MovieFromDBTask().execute();
    }

    private void loadFromNetwork() {
        //TODO Check if online

//        //load data from DB to update the page number
//        loadFromDB();

        //Put page into Bundle
        Bundle bundle = new Bundle();
        bundle.putInt(PAGE_KEY, page);

        //Initialize/start Loader
        Loader<Integer> movieLoader = getSupportLoaderManager().getLoader(NETWORK_LOADER_ID);
        if (movieLoader == null) {
            getSupportLoaderManager().initLoader(NETWORK_LOADER_ID, bundle, this);
        } else {
            getSupportLoaderManager().restartLoader(NETWORK_LOADER_ID, bundle, this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!NetworkUtils.isPopular()) {
            MenuItem miFilter = menu.findItem(R.id.action_filter);
            String topRated = getResources().getString(R.string.top_rated);
            miFilter.setTitle(topRated);
        }
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected called");
        if (item.getItemId() == R.id.action_refresh) {
            Log.i(TAG, "refresh called");
            loadFromDB();
            return true;
        } else if (item.getItemId() == R.id.action_filter) {
            Log.i(TAG, "action_filter clicked");
            showPopup();
        } else if (item.getItemId() == R.id.action_clear_db) {
            int rows = getContentResolver().delete(getActiveTableUri(sActiveTable), null, null);
            mAdapter.setMovies(null);
            page = 1;
            Log.i(TAG, rows + " rows deleted");
        } else if (item.getItemId() == R.id.action_load_from_network) {
            loadFromNetwork();
        } else if (item.getItemId() == R.id.action_load_from_db) {
            loadFromDB();
        }
        return super.onOptionsItemSelected(item);
    }

    public static Uri getActiveTableUri(int key) {
        switch (key) {
            case CODE_POPULAR:
                return MovieContract.MovieEntry.CONTENT_URI;
            case MainActivity.CODE_TOP_RATED:
                return MovieContract.MovieEntry.CONTENT_URI_TOP_RATED;
            case MainActivity.CODE_FAVOURITES:
                //TODO implement favourites
                return null;
            default:
                return MovieContract.MovieEntry.CONTENT_URI;
        }
    }

    public void showPopup() {
        final View menuItemView = findViewById(R.id.action_filter);
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(MainActivity.this, menuItemView);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.filter_actions, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_popular) {
                    Log.i(TAG, "MenuItem popular clicked");
                    MenuItem miFilter = menu.findItem(R.id.action_filter);
                    String popular = getResources().getString(R.string.popular);
                    miFilter.setTitle(popular);
                    NetworkUtils.setPopular();
                    page = 1;
//                    mMovies = null;
                    sActiveTable = CODE_POPULAR;
                    loadFromDB();
                    rvMovies.scrollToPosition(0);
                } else if (menuItem.getItemId() == R.id.action_top_rated) {
                    Log.i(TAG, "MenuItem top rated clicked");
                    MenuItem miFilter = menu.findItem(R.id.action_filter);
                    String topRated = getResources().getString(R.string.top_rated);
                    miFilter.setTitle(topRated);
                    NetworkUtils.setTopRated();
                    page = 1;
                    mMovies = null;
                    sActiveTable = CODE_TOP_RATED;
                    loadFromDB();
                    rvMovies.scrollToPosition(0);
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void showNoConnectionDialog() {
        String noConnection = getResources().getString(R.string.no_connection);
        String tryAgain = getResources().getString(R.string.try_again);
        String requestInternet = getResources().getString(R.string.request_internet_connection);
        new AlertDialog.Builder(this)
                .setTitle(noConnection)
                .setMessage(requestInternet)
                .setPositiveButton(tryAgain, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        loadFromDB();
                    }
                }).show();
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
                    pbLoadingIndicator.setVisibility(View.VISIBLE);
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
    pbLoadingIndicator.setVisibility(View.INVISIBLE);

        if (movies != null) {
            //bulkInsert movies to DB
            ContentValues[] contentValues = ListUtil.makeContentValuesArray(movies);
            int rowsInserted = 0;
            switch (sActiveTable) {
                case CODE_POPULAR:
                    rowsInserted = mContext.getContentResolver()
                            .bulkInsert(MovieContract.MovieEntry.CONTENT_URI, contentValues);
                    break;
                case CODE_TOP_RATED:
                    rowsInserted = mContext.getContentResolver().bulkInsert(
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

    public class MovieFromDBTask extends AsyncTask<Uri, Void, Cursor> {

        @Override
        protected void onPreExecute() {
            pbLoadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected Cursor doInBackground(Uri... params) {
            switch (sActiveTable) {
                case CODE_POPULAR:
                    return getContentResolver().query(
                            MovieContract.MovieEntry.CONTENT_URI,
                            null,
                            null,
                            null,
                            null
                    );
                case CODE_TOP_RATED:
                    return getContentResolver().query(
                            MovieContract.MovieEntry.CONTENT_URI_TOP_RATED,
                            null,
                            null,
                            null,
                            null
                    );
                case CODE_FAVOURITES:
                    //TODO implement favourites
                    return null;
                default:
                    return null;
            }
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            pbLoadingIndicator.setVisibility(View.INVISIBLE);
            mAdapter.setMovies(cursor);
            if (cursor!= null) {
                page = (cursor.getCount() / RESULTS_PER_PAGE);
                page++;
                Log.i(TAG, "page updated to " + page);

                if (page == 1) {
                    //DB is still empty
                    loadFromNetwork();
                }
            }
        }
    }
}