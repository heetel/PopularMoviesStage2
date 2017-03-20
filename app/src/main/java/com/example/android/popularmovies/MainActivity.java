package com.example.android.popularmovies;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
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

    private static final String QUICKSTART_POPULAR = "com.example.android.popularmovies.QUICKSTART_POPULAR";
    private static final String QUICKSTART_TOP_RATED = "com.example.android.popularmovies.QUICKSTART_TOP_RATED";
    private static final String QUICKSTART_FAVOURITES = "com.example.android.popularmovies.QUICKSTART_FAVOURITES";

    private RecyclerView rvMovies;
    private ProgressBar pbLoadingIndicator;
    private Menu menu;
    MovieAdapter mAdapter;
    private Context mContext;

    private static int page = 1;
    private static final String PAGE_KEY = "page-key";

    public static int sColumnCount = 2;

    private static final int NUM_LIST_ITEMS = 20;
    private static final int RESULTS_PER_PAGE = 20;

    private static final String KEY_CURRENT_SCROLLPOSITION = "key-current-scollposition";

    private static final String KEY_ACTIVE_TABLE = "key-active-table";
    public static final int CODE_POPULAR = 1231;
    public static final int CODE_TOP_RATED = 1232;
    public static final int CODE_FAVOURITES = 1233;
    private static int sActiveTable = CODE_POPULAR;

    private static int sCurrentScrollPosition = 0;
    private static int sRestoredScrollPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvMovies = (RecyclerView) findViewById(R.id.rv_movies);
        pbLoadingIndicator = (ProgressBar) findViewById(R.id.pb_loading_indicator);

        sColumnCount = 2;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            sColumnCount = 3;

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, sColumnCount);
        rvMovies.setLayoutManager(gridLayoutManager);

        rvMovies.setHasFixedSize(true);

        mAdapter = new MovieAdapter(NUM_LIST_ITEMS, this, MainActivity.this);
        rvMovies.setAdapter(mAdapter);

        //start at beginning
        page = 1;

        mContext = this;

        String action = getIntent().getAction();
        Log.i(TAG, "intent action: " + action);
        switch (getIntent().getAction()) {
            case QUICKSTART_POPULAR:
                setPopular();
                break;
            case QUICKSTART_TOP_RATED:
                setTopRated();
                break;
            case QUICKSTART_FAVOURITES:
                Log.i(TAG, "Quickstart Favourites");
                setFavourites();
                break;
            default:
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_ACTIVE_TABLE)) {
                sActiveTable = savedInstanceState.getInt(KEY_ACTIVE_TABLE);
                Log.i(TAG, "savedInstanceState restored");
            }
            if (savedInstanceState.containsKey(KEY_CURRENT_SCROLLPOSITION)) {
                int scrollPosition = savedInstanceState.getInt(KEY_CURRENT_SCROLLPOSITION);
                sRestoredScrollPosition = scrollPosition;
                Log.i(TAG, "savedInstanceState restored scrollposition: " + scrollPosition);
            }
        }

        loadFromDB();
    }

    @Override
    protected void onResume() {
        if (sActiveTable == CODE_FAVOURITES) {
            Log.i(TAG, "onResume: refresh favourites");
            loadFromDB();
        }
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ACTIVE_TABLE, sActiveTable);
        Log.i(TAG, "saved active table");
        int scroll = sCurrentScrollPosition;
        outState.putInt(KEY_CURRENT_SCROLLPOSITION, scroll);
        Log.i(TAG, "saved scroll position: " + scroll);
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
        if (sActiveTable == CODE_FAVOURITES) return;
        loadFromNetwork();
    }

    @Override
    public void updatePosition(int position) {
        sCurrentScrollPosition = position;
    }

    private void loadFromDB() {
        Log.i(TAG, "loadFromDB called");

        new MovieFromDBTask().execute();
    }

    private void loadFromNetwork() {

        //check internet connection
        if (!isOnline()) {
            showNoConnectionDialog();
        } else {
            //Put page into Bundle
            Bundle bundle = new Bundle();
            bundle.putInt(PAGE_KEY, page);

            //Initialize/start Loader
            int NETWORK_LOADER_ID = 221;
            Loader<Integer> movieLoader = getSupportLoaderManager().getLoader(NETWORK_LOADER_ID);
            if (movieLoader == null) {
                getSupportLoaderManager().initLoader(NETWORK_LOADER_ID, bundle, this);
            } else {
                getSupportLoaderManager().restartLoader(NETWORK_LOADER_ID, bundle, this);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu = menu;
        updateSelectorTitle();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected called");
        if (item.getItemId() == R.id.action_refresh) {
            Log.i(TAG, "refresh called");
            clearDB();
            loadFromDB();
            return true;
        } else if (item.getItemId() == R.id.action_filter) {
            Log.i(TAG, "action_filter clicked");
            showPopup();
        } else if (item.getItemId() == R.id.action_clear_db) {
            clearDB();
        } else if (item.getItemId() == R.id.action_load_from_network) {
            loadFromNetwork();
        } else if (item.getItemId() == R.id.action_load_from_db) {
            loadFromDB();
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearDB() {
        int rows = getContentResolver().delete(getActiveTableUri(sActiveTable), null, null);
        mAdapter.setMovies(null);
        page = 1;
        Log.i(TAG, rows + " rows deleted");
    }

    public static Uri getActiveTableUri(int key) {
        switch (key) {
            case CODE_POPULAR:
                return MovieContract.MovieEntry.CONTENT_URI;
            case MainActivity.CODE_TOP_RATED:
                return MovieContract.MovieEntry.CONTENT_URI_TOP_RATED;
            case MainActivity.CODE_FAVOURITES:
                return MovieContract.MovieEntry.CONTENT_URI_FAVOURITES;
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
                    setPopular();
                } else if (menuItem.getItemId() == R.id.action_top_rated) {
                    Log.i(TAG, "MenuItem top rated clicked");
                    setTopRated();
                } else if (menuItem.getItemId() == R.id.action_favourites) {
                    Log.i(TAG, "MenuItem Favourites clicked");
                    setFavourites();
                }
                loadFromDB();
                updateSelectorTitle();
                return false;
            }
        });
        popupMenu.show();
    }

    private void setPopular() {
        NetworkUtils.setPopular();
        sActiveTable = CODE_POPULAR;
    }

    private void setTopRated() {
        NetworkUtils.setTopRated();
        sActiveTable = CODE_TOP_RATED;
    }

    private void setFavourites() {
        sActiveTable = CODE_FAVOURITES;
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

    private class MovieFromDBTask extends AsyncTask<Uri, Void, Cursor> {

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
                    return  getContentResolver().query(
                            MovieContract.MovieEntry.CONTENT_URI_FAVOURITES,
                            null,
                            null,
                            null,
                            null
                    );
                default:
                    return null;
            }
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            pbLoadingIndicator.setVisibility(View.INVISIBLE);
            mAdapter.setMovies(cursor);
            if (sRestoredScrollPosition != 0) {
                rvMovies.scrollToPosition(sRestoredScrollPosition);
                sRestoredScrollPosition = 0;
            }
            if (cursor!= null && sActiveTable != CODE_FAVOURITES) {
                page = (cursor.getCount() / RESULTS_PER_PAGE);
                page++;
                Log.i(TAG, "page updated to " + page);

                if (page == 1) {
                    //DB is still empty
                    loadFromNetwork();
                }
            }

//            updateSelectorTitle();
        }
    }

    private void updateSelectorTitle() {
        MenuItem miFilter = menu.findItem(R.id.action_filter);
        switch (sActiveTable) {
            case CODE_POPULAR:
                miFilter.setTitle(R.string.popular);
                break;
            case CODE_TOP_RATED:
                miFilter.setTitle(R.string.top_rated);
                break;
            case CODE_FAVOURITES:
                miFilter.setTitle(R.string.favourites);
                break;
            default:
                miFilter.setTitle(R.string.popular);
        }
    }
}