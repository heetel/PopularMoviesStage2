package com.heetel.android.popularmovies;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.aurelhubert.ahbottomnavigation.notification.AHNotification;
import com.heetel.android.popularmovies.data.MovieContract;
import com.heetel.android.popularmovies.search.SearchFragment;
import com.heetel.android.popularmovies.utilities.FavouritesUtil;
import com.heetel.android.popularmovies.utilities.NetworkUtils;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

// DONE: Rotation: Search --> Fragment
// TODO: Settings: Language
// TODO: Detailscreen: new Layout
// TODO: Detailscreen: new vote View
// TODO: Sharing
// TODO: Launch Screen

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
        implements
        FavouritesUtil.FavouritesUtilCallback {

    private static final String KEY_SEARCH = "key-search";
    private final String TAG = MainActivity.class.getSimpleName();

    private static final String QUICKSTART_POPULAR = "com.heetel.android.popularmovies.QUICKSTART_POPULAR";
    private static final String QUICKSTART_TOP_RATED = "com.heetel.android.popularmovies.QUICKSTART_TOP_RATED";
    private static final String QUICKSTART_FAVOURITES = "com.heetel.android.popularmovies.QUICKSTART_FAVOURITES";

    MovieAdapter mAdapter;

    public static int sColumnCount = 2;

    //keys for saveInstanceBundle
    private static final String KEY_ACTIVE_TABLE = "key-active-table";

    //internal table keys
//    public static final int CODE_POPULAR = 1231;
//    public static final int CODE_TOP_RATED = 1232;
//    public static final int CODE_FAVOURITES = 1233;
//    private static int sActiveTable;

    private int currentIndex, newFavourites;
    private static MovieListFragment movieFragments[] = new MovieListFragment[3];
    private SearchFragment searchFragment;
    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;
    private AHBottomNavigation bottomNavigation;
    private MaterialSearchView searchView;
    private String lastSearch;

    FavouritesUtil favouritesUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (!isOnline()) showNoConnectionDialog();

        initSearchView();

        initBottomNavigation();

        favouritesUtil = new FavouritesUtil(this);

        handleIntent(getIntent());

        //restore lastly shown table and scroll position in RecyclerView
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_ACTIVE_TABLE)) {
                currentIndex = savedInstanceState.getInt(KEY_ACTIVE_TABLE);
                switchCurrentFragment(currentIndex);
                Log.i(TAG, "savedInstanceState restored");
            } else if (savedInstanceState.containsKey(KEY_SEARCH)) {
                searchFor(savedInstanceState.getString(KEY_SEARCH));
                Log.i(TAG, "lastSearch restored");
            }
//            if (savedInstanceState.containsKey(KEY_CURRENT_SCROLLPOSITION)) {
//                int scrollPosition = savedInstanceState.getInt(KEY_CURRENT_SCROLLPOSITION);
//                sRestoredScrollPosition = scrollPosition;
//                Log.i(TAG, "savedInstanceState restored scrollposition: " + scrollPosition);
//            }
        }
    }

    @Override
    protected void onResume() {
        favouritesUtil.checkCount();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
            closeSearch();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (searchView.isSearchOpen() && lastSearch != null) {
            outState.putString(KEY_SEARCH, lastSearch);
            Log.i(TAG, "lastSearch saved");
        }
        super.onSaveInstanceState(outState);
    }

    private void searchFor(String query) {
        Log.i(TAG, query);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (searchFragment != null) transaction.remove(searchFragment);
        else transaction.setCustomAnimations(R.animator.search_in, R.animator.search_out);
        searchFragment = SearchFragment.newInstance(query);
        transaction.add(R.id.fragment_container, searchFragment);
        transaction.commit();

        lastSearch = query;
    }

    private void closeSearch() {
        if (searchFragment == null) return;
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.fade_in, R.animator.search_out);
        transaction.remove(searchFragment);
        transaction.commit();
        searchFragment = null;
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
                switchCurrentFragment(position);
                return true;
            }
        });

        bottomNavigation.setNotificationBackgroundColor(getColor(R.color.mColorLabel));

        fragmentManager = getFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();

        movieFragments[0] = MovieListFragment.newInstance(0);
        movieFragments[1] = MovieListFragment.newInstance(1);
        movieFragments[2] = MovieListFragment.newInstance(2);
        fragmentTransaction.replace(R.id.fragment_container, movieFragments[0]);

        fragmentTransaction.commit();
    }

    private void initSearchView() {
        searchView = (MaterialSearchView) findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            final String TAG = "MaterialSearchView";

            @Override
            public boolean onQueryTextSubmit(String query) {
                searchFor(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.i(TAG, newText);
                return false;
            }
        });
        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {

            }

            @Override
            public void onSearchViewClosed() {
                closeSearch();
            }
        });
        searchView.setVoiceSearch(true);
    }

    private void switchCurrentFragment(int index) {
        currentIndex = index;

        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
        fragmentTransaction.replace(R.id.fragment_container, movieFragments[index]);
        fragmentTransaction.commit();

        if (index == 2) {
            bottomNavigation.setNotification("", 2);
            newFavourites = 0;
        }
    }

    private void switchBottomNavigationColored() {
        if (currentIndex == 0) NetworkUtils.setPopular();
        else if (currentIndex == 1) NetworkUtils.setTopRated();

        if (bottomNavigation.isColored()) bottomNavigation.setColored(false);
        else bottomNavigation.setColored(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem menuItemSearch = menu.findItem(R.id.action_search);
        searchView.setMenuItem(menuItemSearch);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            clearDB();
            return true;
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
        Log.i(TAG, currentIndex + " clearDB()");
        //don't delete favourites
        if (currentIndex == 2) return;
        int rows = getContentResolver().delete(getActiveTableUri(currentIndex), null, null);
        movieFragments[currentIndex].refresh();
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
            case 0:
                return MovieContract.MovieEntry.CONTENT_URI;
            case 1:
                return MovieContract.MovieEntry.CONTENT_URI_TOP_RATED;
            case 2:
                return MovieContract.MovieEntry.CONTENT_URI_FAVOURITES;
            default:
                return MovieContract.MovieEntry.CONTENT_URI;
        }
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
//                        loadFromDB();
                    }
                }).show();
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

    @Override
    public void newFavourite() {
        Log.i(TAG, "newFavourite()");

        newFavourites++;

        AHNotification notification = new AHNotification.Builder()
                .setText(String.valueOf(newFavourites))
                .build();

        bottomNavigation.setNotification(notification, 2);
    }

    private void handleIntent(Intent intent) {

        //In case the App started from Nougat launcher shortcut, apply table name to query
        String action = intent.getAction();
        Log.i(TAG, "handle intent action: " + action);
        switch (getIntent().getAction()) {
            case QUICKSTART_POPULAR:
//                setPopular();
                switchCurrentFragment(0);
                break;
            case QUICKSTART_TOP_RATED:
//                setTopRated();
                switchCurrentFragment(1);
                break;
            case QUICKSTART_FAVOURITES:
//                setFavourites();
//                Log.e(TAG, "switch favourites");
                switchCurrentFragment(2);
                break;
            default:
//                setPopular();
//                switchCurrentFragment(0);
        }
        bottomNavigation.setCurrentItem(currentIndex);
    }
}