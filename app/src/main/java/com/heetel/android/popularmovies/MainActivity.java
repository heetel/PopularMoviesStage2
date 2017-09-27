package com.heetel.android.popularmovies;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.aurelhubert.ahbottomnavigation.notification.AHNotification;
import com.heetel.android.popularmovies.data.Movie;
import com.heetel.android.popularmovies.data.MovieContract;
import com.heetel.android.popularmovies.utilities.FavouritesUtil;
import com.heetel.android.popularmovies.utilities.NetworkUtils;
import com.heetel.android.popularmovies.utilities.SearchHelper;

import java.util.ArrayList;

// TODO: Aufräumen: BackgroundTasks jetzt in Fragment
// TODO: Crash bei Rotation
// TODO: Rotation: Fragment und Scrollposition wiederherstellen
// TODO: Bug: Doppelte Fragment Instanzen

/**
 * Created by Julian Heetel
 *
 * This app doesn't need internet connection every time the user wants to see popular movies.
 * On initial launch, there will be 20 items (popular / top rated) loaded from API and saved into
 * ContentProvider. When the user scrolls down close to the end of the list, there will be 20 more
 * items loaded and so on. That data will persist until the user clicks "Refresh" in the options
 * menu or reset the apps storage.
 *
 * When this app starts,
 */
public class MainActivity extends AppCompatActivity
        implements
        android.support.v7.widget.SearchView.OnQueryTextListener,
        SearchHelper.SearchCallbacks,
        SearchAdapter.ListItemCallbackListener,
        FavouritesUtil.FavouritesUtilCallback
{

    private final String TAG = MainActivity.class.getSimpleName();

    private static final String QUICKSTART_POPULAR = "com.example.android.popularmovies.QUICKSTART_POPULAR";
    private static final String QUICKSTART_TOP_RATED = "com.example.android.popularmovies.QUICKSTART_TOP_RATED";
    private static final String QUICKSTART_FAVOURITES = "com.example.android.popularmovies.QUICKSTART_FAVOURITES";

    private RecyclerView rvSearch;
    private ProgressBar searchLoadingIndicator;
    private ImageView ivNoSearchResults;
    MovieAdapter mAdapter;
    SearchAdapter mSearchAdapter;

    public static int sColumnCount = 2;

    //keys for saveInstanceBundle
    private static final String KEY_ACTIVE_TABLE = "key-active-table";

    //internal table keys
    public static final int CODE_POPULAR = 1231;
//    public static final int CODE_TOP_RATED = 1232;
//    public static final int CODE_FAVOURITES = 1233;
//    private static int sActiveTable;

    private int currentIndex, newFavourites;
    private static MovieListFragment movieFragments[] = new MovieListFragment[3];
    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;
    private AHBottomNavigation bottomNavigation;
    private FrameLayout searchContainer;
    LinearLayoutManager layoutManager;

    FavouritesUtil favouritesUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //init RecyclerView for Search results
        rvSearch = (RecyclerView) findViewById(R.id.rv_search);
        rvSearch.setVisibility(View.VISIBLE);
        layoutManager = new LinearLayoutManager(this);
        rvSearch.setLayoutManager(layoutManager);
        rvSearch.setHasFixedSize(true);
        mSearchAdapter = new SearchAdapter(1, this, this);
        rvSearch.setAdapter(mSearchAdapter);
        rvSearch.setVisibility(View.GONE);

        searchContainer = (FrameLayout) findViewById(R.id.search_container);
        searchLoadingIndicator = (ProgressBar) findViewById(R.id.search_loading_indicator);

        ivNoSearchResults = (ImageView) findViewById(R.id.noSearchResults);

        if (!isOnline()) showNoConnectionDialog();

        initBottomNavigation();

        //In case the App started from Nougat launcher shortcut, apply table name to query
        String action = getIntent().getAction();
        Log.i(TAG, "intent action: " + action);
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
                switchCurrentFragment(2);
                break;
            default:
//                setPopular();
                switchCurrentFragment(0);
        }

        //restore lastly shown table and scroll position in RecyclerView
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_ACTIVE_TABLE)) {
                currentIndex = savedInstanceState.getInt(KEY_ACTIVE_TABLE);
                switchCurrentFragment(currentIndex);
                Log.i(TAG, "savedInstanceState restored");
            }
//            if (savedInstanceState.containsKey(KEY_CURRENT_SCROLLPOSITION)) {
//                int scrollPosition = savedInstanceState.getInt(KEY_CURRENT_SCROLLPOSITION);
//                sRestoredScrollPosition = scrollPosition;
//                Log.i(TAG, "savedInstanceState restored scrollposition: " + scrollPosition);
//            }
        }

        favouritesUtil = new FavouritesUtil(this);
    }

    @Override
    protected void onResume() {
        favouritesUtil.checkCount();
        super.onResume();
    }

    @SuppressLint({"NewApi", "LocalSuppress"})
    private void initBottomNavigation() {
        bottomNavigation = (AHBottomNavigation) findViewById(R.id.bottom_navigation);
//        viewPager = (AHBottomNavigationViewPager) findViewById(R.id.view_pager);

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
//                switch (position) {
//                    case 0:
//                        setPopular();
//                        break;
//                    case 1:
//                        setTopRated();
//                        break;
//                    case 2:
//                        setFavourites();
//                        break;
//                }
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

    private void switchBottomNavigationColored(){
        if (currentIndex == 0) NetworkUtils.setPopular();
        else if (currentIndex == 1) NetworkUtils.setTopRated();

        if (bottomNavigation.isColored()) bottomNavigation.setColored(false);
        else bottomNavigation.setColored(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.main, menu);

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

                final ObjectAnimator alpha = ObjectAnimator.ofFloat(searchContainer, "alpha", 1f, 0f);
                final ObjectAnimator scaleX = ObjectAnimator.ofFloat(searchContainer, "scaleX", 1f, 1.2f);
                final ObjectAnimator scaleY = ObjectAnimator.ofFloat(searchContainer, "scaleY", 1f, 1.2f);

                final ObjectAnimator reverseY = ObjectAnimator.ofFloat(searchContainer, "scaleY", 1.2f, 1f);
                reverseY.setDuration(0);

                AnimatorSet set = new AnimatorSet();
                set.playTogether(alpha, scaleX, scaleY);
                set.setDuration(300);
                set.start();
                set.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {}
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        searchContainer.setVisibility(View.GONE);
                        animation.removeListener(this);
                        Log.i(TAG, "animations: " + ((AnimatorSet)animation).getChildAnimations().size());
                        ((ObjectAnimator)((AnimatorSet)animation).getChildAnimations().get(0)).reverse();
                        ((ObjectAnimator)((AnimatorSet)animation).getChildAnimations().get(1)).reverse();
                        ((ObjectAnimator)((AnimatorSet)animation).getChildAnimations().get(2)).reverse();
                        reverseY.start();
                        mSearchAdapter.setData(null);
                    }
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        searchContainer.setVisibility(View.GONE);
                        animation.removeListener(this);
                        ((ObjectAnimator)((AnimatorSet)animation).getChildAnimations().get(0)).reverse();
                        ((ObjectAnimator)((AnimatorSet)animation).getChildAnimations().get(1)).reverse();
                        ((ObjectAnimator)((AnimatorSet)animation).getChildAnimations().get(2)).reverse();
                        reverseY.start();
                        mSearchAdapter.setData(null);
                    }
                    @Override
                    public void onAnimationRepeat(Animator animation) {}
                });
                /*ivNoSearchResults.setVisibility(View.GONE);*/
//                fragmentContainer.setVisibility(View.VISIBLE);
                return true;
            }
        });

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

    @Override
    public boolean onQueryTextSubmit(String query) {
//        pbLoadingIndicator.setVisibility(View.VISIBLE);
//        fragmentContainer.setVisibility(View.INVISIBLE);

        searchContainer.setVisibility(View.VISIBLE);
        searchLoadingIndicator.setVisibility(View.VISIBLE);

        SearchHelper searchHelper = new SearchHelper(this, query, this);
        searchHelper.search();

        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.i(TAG, "onQueryTextChange()" + newText);
        return false;
    }

    /**
     * This method is called when the search is finished in the background and deliveres the result
     * @param results search result
     */
    @Override
    public void onSearch(ArrayList<ContentValues> results) {
        Log.i(TAG, "onSearch : " + results.size());
        searchLoadingIndicator.setVisibility(View.GONE);

        if (results.size() <=0) {
            ivNoSearchResults.setVisibility(View.VISIBLE);
            return;
        }

//        fragmentContainer.setVisibility(View.GONE);
        rvSearch.setVisibility(View.VISIBLE);
        mSearchAdapter.setData(results);
        rvSearch.scrollToPosition(0);
//        bottomNavigation.setSelected(true);
        // Check if no view has focus:
        /*View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }*/

        rvSearch.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {

                    @Override
                    public boolean onPreDraw() {
                        Log.i(TAG, "onPreDraw() ");
                        rvSearch.getViewTreeObserver().removeOnPreDrawListener(this);

                        for (int i = 0; i < rvSearch.getChildCount(); i++) {
                            View v = rvSearch.getChildAt(i);
                            v.setAlpha(0.0f);
                            v.animate().alpha(1.0f)
                                    .setDuration(300)
                                    .setStartDelay(i * 125)
                                    .start();
                        }

                        return true;
                    }
                });


        Log.i(TAG, "visibility: " + rvSearch.getVisibility());
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

//        ActivityOptionsCompat activityOptionsCompat =
//                ActivityOptionsCompat.makeSceneTransitionAnimation(
//                        this, layoutManager.findViewByPosition(position), "poster");
//        ActivityCompat.startActivity(this, intent, activityOptionsCompat.toBundle());
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
}