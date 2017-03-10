package com.example.android.popularmovies;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.example.android.popularmovies.utilities.NetworkLoader;
import com.example.android.popularmovies.utilities.NetworkUtils;

import java.util.ArrayList;

/**
 * API-Key:
 */
public class MainActivity extends AppCompatActivity
        implements MovieAdapter.ListItemCallbackListener, LoaderManager.LoaderCallbacks<Cursor>,
                    NetworkLoader.CallbackListener {

    private final String TAG = MainActivity.class.getSimpleName();

    private final int MOVIE_FROM_DB_LOADER_ID = 220;

    private RecyclerView rvMovies;
    private ProgressBar pbLoadingIndicator;
    private Menu menu;
    MovieAdapter mAdapter;
    ArrayList<Movie> mMovies;
    private Context mContext;

    private static int page = 1;

    private static final int NUM_LIST_ITEMS = 20;

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

        loadFromNetwork();
    }

    @Override
    public void onListItemClick(Cursor cursor, int clickedItemIndex) {
        Log.d(TAG, "onListItemClick called #" + clickedItemIndex);

        Context context = MainActivity.this;
        Intent intent = new Intent(context, DetailActivity.class);

        startActivity(intent);
    }

    /**
     * This method gets called when the User scrolls down to the end of RecyclerView
     *
     */
    @Override
    public void onLoadMore() {
        page ++;
        loadFromNetwork();
    }



    private void loadFromDB() {
        Log.i(TAG, "loadFromDB called");
        getSupportLoaderManager().restartLoader(MOVIE_FROM_DB_LOADER_ID, null, this);
    }

    private void loadFromNetwork() {
        new NetworkLoader(this, getSupportLoaderManager(), page);
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
            int rows = getContentResolver().delete(MovieContract.MovieEntry.CONTENT_URI, null, null);
            mAdapter.setMovies(null);
            Log.i(TAG, rows + " rows deleted");
        } else if (item.getItemId() == R.id.action_load_from_network) {
            loadFromNetwork();
        } else if (item.getItemId() == R.id.action_load_from_db) {
            loadFromDB();
        }
        return super.onOptionsItemSelected(item);
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
                    mMovies = null;
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
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new AsyncTaskLoader<Cursor>(this) {

            @Override
            protected void onStartLoading() {
                Log.i(TAG, "onStartLoading");
                pbLoadingIndicator.setVisibility(View.VISIBLE);
                forceLoad();
            }

            @Override
            public Cursor loadInBackground() {
                Log.i(TAG, "loadInBackground");
                return getContentResolver().query(
                        MovieContract.MovieEntry.CONTENT_URI,
                        null,
                        null,
                        null,
                        null
                );
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.i(TAG, "onLoadFinished, Cursor: " + data.getCount());// Count is same as before until App relaunch
        pbLoadingIndicator.setVisibility(View.INVISIBLE);

        mAdapter.setMovies(data);

        loader.commitContentChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * This Callback method is called when the Networkloader finished.
     */
    @Override
    public void onLoadingFinished() {
        //Refresh UI by loading data from DB
        loadFromDB();
    }
}