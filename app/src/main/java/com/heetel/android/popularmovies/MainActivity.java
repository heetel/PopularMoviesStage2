package com.heetel.android.popularmovies;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.heetel.android.popularmovies.data.Movie;
import com.heetel.android.popularmovies.data.MovieContract;
import com.heetel.android.popularmovies.utilities.ListUtil;
import com.heetel.android.popularmovies.utilities.NetworkUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.heetel.android.popularmovies.utilities.SearchHelper;

import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Julian Heetel
 * <p>
 * This app doesn't need internet connection every time the user wants to see popular movies.
 * On initial launch, there will be 20 items (popular / top rated) loaded from API and saved into
 * ContentProvider. When the user scrolls down close to the end of the list, there will be 20 more
 * items loaded and so on. That data will persist until the user clicks "Refresh" in the options
 * menu or reset the apps storage.
 * <p>
 * When this app starts,
 */
public class MainActivity extends AppCompatActivity
        implements MovieAdapter.ListItemCallbackListener,
        LoaderManager.LoaderCallbacks<ArrayList<ContentValues>>,
        android.support.v7.widget.SearchView.OnQueryTextListener,
        SearchHelper.SearchCallbacks,
        SearchAdapter.ListItemCallbackListener
{

    private final String TAG = MainActivity.class.getSimpleName();

    private static final String QUICKSTART_POPULAR = "com.example.android.popularmovies.QUICKSTART_POPULAR";
    private static final String QUICKSTART_TOP_RATED = "com.example.android.popularmovies.QUICKSTART_TOP_RATED";
    private static final String QUICKSTART_FAVOURITES = "com.example.android.popularmovies.QUICKSTART_FAVOURITES";

    private RecyclerView rvMovies, rvSearch;
    private ProgressBar pbLoadingIndicator;
    private ImageView ivNoSearchResults;
    private Menu menu;
    private AHBottomNavigation bottomNavigation;
    MovieAdapter mAdapter;
    SearchAdapter mSearchAdapter;

    private static int page = 1;
    private static final String PAGE_KEY = "page-key";

    public static int sColumnCount = 2;

    //number of list items to setup RecyclerViewAdapter
    private static final int NUM_LIST_ITEMS = 20;

    //API provides 20 results per page
    private static final int RESULTS_PER_PAGE = 20;

    //keys for saveInstanceBundle
    private static final String KEY_CURRENT_SCROLLPOSITION = "key-current-scollposition";
    private static final String KEY_ACTIVE_TABLE = "key-active-table";

    //internal table keys
    public static final int CODE_POPULAR = 1231;
    public static final int CODE_TOP_RATED = 1232;
    public static final int CODE_FAVOURITES = 1233;
    private static int sActiveTable;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private static int sCurrentScrollPosition = 0;
    private static int sRestoredScrollPosition = 0;

    private static GridLayoutManager mGridLayoutManager;

//    private String[] mPlanetTitles;
//    private DrawerLayout mDrawerLayout;
//    private ListView mDrawerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //initialize RecyclerView and Adapter
        rvMovies = (RecyclerView) findViewById(R.id.rv_movies);
        //RecyclerView displays 2 or 3 Columns depending on devices orientation
        sColumnCount = 2;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            sColumnCount = 3;

        mGridLayoutManager = new GridLayoutManager(this, sColumnCount);
        rvMovies.setLayoutManager(mGridLayoutManager);

        rvMovies.setHasFixedSize(true);

        mAdapter = new MovieAdapter(NUM_LIST_ITEMS, this, MainActivity.this);
        rvMovies.setAdapter(mAdapter);

        pbLoadingIndicator = (ProgressBar) findViewById(R.id.pb_loading_indicator);

        handleIntent(getIntent());

        //In case the App started from Nougat launcher shortcut, apply table name to query
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
                setFavourites();
                break;
            default:
                setPopular();
        }

        //restore lastly shown table and scroll position in RecyclerView
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
        checkPlayServices();

        //Query ContentProvider
        loadFromDB();

        //init RecyclerView for Search results
        rvSearch = (RecyclerView) findViewById(R.id.rv_search);
        rvSearch.setVisibility(View.VISIBLE);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvSearch.setLayoutManager(layoutManager);
        rvSearch.setHasFixedSize(true);
        mSearchAdapter = new SearchAdapter(1, this, this);
        rvSearch.setAdapter(mSearchAdapter);
        rvSearch.setVisibility(View.GONE);

        ivNoSearchResults = (ImageView) findViewById(R.id.noSearchResults);

        initBottomNavigation();
    }

    @SuppressLint({"NewApi", "LocalSuppress"})
    private void initBottomNavigation() {
        bottomNavigation = (AHBottomNavigation) findViewById(R.id.bottom_navigation);

        //Create Items
        AHBottomNavigationItem item1 = new AHBottomNavigationItem(getString(R.string.popular), getDrawable(R.drawable.ic_popular), getColor(R.color.color_tab_1));
        AHBottomNavigationItem item2 = new AHBottomNavigationItem(getString(R.string.top_rated), getDrawable(R.drawable.ic_top_rated), getColor(R.color.color_tab_2));
        AHBottomNavigationItem item3 = new AHBottomNavigationItem(getString(R.string.favourites), getDrawable(R.drawable.ic_shortcut_favourite), getColor(R.color.color_tab_3));

        //Add Items
        bottomNavigation.addItem(item1);
        bottomNavigation.addItem(item2);
        bottomNavigation.addItem(item3);

        //Set Colors
        bottomNavigation.setBackgroundColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bottomNavigation.setAccentColor(getColor(R.color.colorAccent));
        }
        bottomNavigation.setInactiveColor(Color.parseColor("#747474"));

        // Force to tint the drawable (useful for font with icon for example)
//        bottomNavigation.setForceTint(true);
        // Display color under navigation bar (API 21+)
        bottomNavigation.setTranslucentNavigationEnabled(true);

        // Manage titles
        bottomNavigation.setTitleState(AHBottomNavigation.TitleState.SHOW_WHEN_ACTIVE);
//        bottomNavigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_SHOW);
//        bottomNavigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_HIDE);

        // Use colored navigation with circle reveal effect
//        bottomNavigation.setColored(true);

//        bottomNavigation.disableItemAtPosition(2);

        bottomNavigation.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected) {
                Log.i(TAG, "onPositionChange() " + position + wasSelected);
                switch (position) {
                    case 0:
                        setPopular();
                        break;
                    case 1:
                        setTopRated();
                        break;
                    case 2:
                        setFavourites();
                        break;
                }
                loadFromDB();
                updateSelectorTitle();
                return true;
            }
        });
        bottomNavigation.setOnNavigationPositionListener(new AHBottomNavigation.OnNavigationPositionListener() {
            @Override
            public void onPositionChange(int y) {
                Log.i(TAG, "onPositionChange()");
            }
        });
    }

    private void switchBottomNavigationColored(){
        if (bottomNavigation.isColored()) bottomNavigation.setColored(false);
        else bottomNavigation.setColored(true);
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
        //save current shown table and scroll position in RecyclerView
        outState.putInt(KEY_ACTIVE_TABLE, sActiveTable);
        Log.i(TAG, "saved active table");
        int scroll = sCurrentScrollPosition;
        outState.putInt(KEY_CURRENT_SCROLLPOSITION, scroll);
        Log.i(TAG, "saved scroll position: " + scroll);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /**
     * Callback method from Adapter that is called when the user clicks a movie
     *
     * @param movieId movie_id for DetailActivity to query the right movie data
     */
    @Override
    public void onListItemClick(String movieId, String title, int position) {
        Intent intent = new Intent(this, DetailActivity.class);

        intent.putExtra(DetailActivity.INTENT_MOVIE_ID_KEY, movieId);

        intent.putExtra(DetailActivity.INTENT_TABLE_KEY, sActiveTable);

        intent.putExtra(DetailActivity.INTENT_MOVIE_TITLE_KEY, title);

        ActivityOptionsCompat activityOptionsCompat =
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this, mGridLayoutManager.findViewByPosition(position), "poster");
        ActivityCompat.startActivity(this, intent, activityOptionsCompat.toBundle());

//        startActivity(intent);
    }

    /**
     * This method gets called when the User scrolls down to the end of RecyclerView
     */
    @Override
    public void onLoadMore() {
        if (sActiveTable == CODE_FAVOURITES) return;
        loadFromNetwork();
    }

    /**
     * This Callback method from MovieAdapter is called for every single movie, the Adapter
     * loads. It gives the position of RecyclerView that will be needed to save instance bundle
     *
     * @param position current position of RecyclerView
     */
    @Override
    public void updatePosition(int position) {
        sCurrentScrollPosition = position;
    }

    /**
     * +     * Check the device to make sure it has the Google Play Services APK. If
     * +     * it doesn't, display a dialog that allows users to download the APK from
     * +     * the Google Play Store or enable it in the device's system settings.
     * +
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        Log.i(TAG, "checkPlayServices OK");
        return true;
    }

    /**
     * Query data from ContentProvider in background task and
     * Update the UI.
     */
    private void loadFromDB() {
        Log.i(TAG, "loadFromDB called");

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

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
//        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);
//        searchView.setBackgroundColor(Color.WHITE);
        MenuItem menuItem = menu.findItem(R.id.action_search);
        MenuItemCompat.setOnActionExpandListener(menuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                Log.i(TAG, "onMenuItemActionExpand");
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                Log.i(TAG, "onMenuItemActionCollapse");
                rvSearch.setVisibility(View.GONE);
                rvMovies.setVisibility(View.VISIBLE);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            clearDB();
            loadFromDB();
            return true;
        /*} else if (item.getItemId() == R.id.action_filter) {
            showPopup();*/
        } else if (item.getItemId() == R.id.action_remove_all_favourites) {
            showRemoveFavouritesDialog();
        } else if (item.getItemId() == R.id.action_switch_colored) {
            switchBottomNavigationColored();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Delete current table in ContentProvider
     * (Favourites won't be deleted.)
     */
    private void clearDB() {
        //don't delete favourites
        if (sActiveTable == CODE_FAVOURITES) return;
        int rows = getContentResolver().delete(getActiveTableUri(sActiveTable), null, null);
        mAdapter.setMovies(null);
        page = 1;
        Log.i(TAG, rows + " rows deleted");
    }

    /**
     * Get the Content URI for the table depending on the constant key in MainActivity.
     * Default is the Content URI for table popular.
     *
     * @param key Key that should match one of MainActivitys table keys.
     * @return Content Uri depending on key
     */
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

    /**
     * Show options menu to select table to be displayed.
     */
    public void showPopup() {
        /*final View menuItemView = findViewById(R.id.action_filter);
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
        popupMenu.show();*/
    }

    /**
     * Sets current table to popular
     */
    private void setPopular() {
        NetworkUtils.setPopular();
        sActiveTable = CODE_POPULAR;
    }

    /**
     * Sets current table to top rated
     */
    private void setTopRated() {
        NetworkUtils.setTopRated();
        sActiveTable = CODE_TOP_RATED;
    }

    /**
     * Sets current table to favourites
     */
    private void setFavourites() {
        sActiveTable = CODE_FAVOURITES;
    }

    /**
     * Check if device is connected to the internet.
     *
     * @return true if device is connected to the internet, else false.
     */
    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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
        return new AsyncTaskLoader<ArrayList<ContentValues>>(this) {

            //Cached data
            ArrayList<ContentValues> cachedMovies;

            @Override
            protected void onStartLoading() {
                if (args == null) return;

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
        pbLoadingIndicator.setVisibility(View.INVISIBLE);

        //insert data into ContentProvider
        if (movies != null) {
            //bulkInsert movies to DB
            ContentValues[] contentValues = ListUtil.makeContentValuesArray(movies);
            int rowsInserted = 0;
            switch (sActiveTable) {
                case CODE_POPULAR:
                    rowsInserted = getContentResolver()
                            .bulkInsert(MovieContract.MovieEntry.CONTENT_URI, contentValues);
                    break;
                case CODE_TOP_RATED:
                    rowsInserted = getContentResolver().bulkInsert(
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

    @Override
    public boolean onQueryTextSubmit(String query) {
        pbLoadingIndicator.setVisibility(View.VISIBLE);
        rvMovies.setVisibility(View.GONE);

        SearchHelper searchHelper = new SearchHelper(this, query, this);
        searchHelper.search();

        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.i(TAG, "onQueryTextChange()" + newText);
        return false;
    }

    @Override
    public void onSearch(ArrayList<ContentValues> results) {
        Log.i(TAG, "onSearch");
        pbLoadingIndicator.setVisibility(View.GONE);

        if (results.size() <=0) {
            ivNoSearchResults.setVisibility(View.VISIBLE);
            return;
        }

        rvSearch.setVisibility(View.VISIBLE);
        mSearchAdapter.setData(results);
        rvSearch.scrollToPosition(0);
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * SeachItem click
     * @param position clicked position
     */
    @Override
    public void onListItemClick(int position) {
        Log.i(TAG, "onListItemClick : " + position);
        ArrayList<ContentValues> data = mSearchAdapter.getData();
        Movie movie = new Movie(data.get(position));
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.INTENT_MOVIE_KEY, movie);
        startActivity(intent);
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
                    // query in reverse order (DESC) to display most recent added favourite first
                    return getContentResolver().query(
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
            pbLoadingIndicator.setVisibility(View.INVISIBLE);
            mAdapter.setMovies(cursor);
            if (sRestoredScrollPosition != 0) {
                rvMovies.scrollToPosition(sRestoredScrollPosition);
                sRestoredScrollPosition = 0;
            }
            if (cursor != null && sActiveTable != CODE_FAVOURITES) {
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

    /**
     * Update the options items title depending on current table
     */
    private void updateSelectorTitle() {
        /*MenuItem miFilter = menu.findItem(R.id.action_filter);
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
        }*/
    }

    /**
     * Dialog for the user to confirm deleting all favourites
     */
    private void showRemoveFavouritesDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.remove_all_favourites) + "?")
                .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeFavourites();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create().show();
    }

    /**
     * Delete favourites table of ContentProvider
     */
    private void removeFavourites() {
        getContentResolver().delete(
                MovieContract.MovieEntry.CONTENT_URI_FAVOURITES,
                null,
                null
        );
        mAdapter.setMovies(null);
        Toast.makeText(this, getString(R.string.favourites_removed), Toast.LENGTH_SHORT).show();
        // make Snackbar instead of Toast
    }

    private void handleIntent(Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Toast.makeText(this, "Not yet implemented.", Toast.LENGTH_SHORT).show();
        }

    }
}