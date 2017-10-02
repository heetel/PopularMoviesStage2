package com.heetel.android.popularmovies.search;

import android.app.Fragment;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.heetel.android.popularmovies.DetailActivity;
import com.heetel.android.popularmovies.R;
import com.heetel.android.popularmovies.data.Movie;

import java.util.ArrayList;

/**
 * Created by Julian Heetel on 02.10.2017.
 *
 */

public class SearchFragment extends Fragment
        implements SearchAdapter.ListItemCallbackListener,
                    SearchHelper.SearchCallbacks{

    private static final String TAG = SearchFragment.class.getSimpleName();
    private static final String KEY_QUERY = "key-query";

    private RecyclerView rvSearch;
    private SearchAdapter mSearchAdapter;
    private ProgressBar searchLoadingIndicator;
    private ImageView ivNoSearchResults;

    public static SearchFragment newInstance(String query) {
        
        Bundle args = new Bundle();
        args.putString(KEY_QUERY, query);
        SearchFragment fragment = new SearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search, container, false);

        initView(view);
        getActivity().getLoaderManager();

        query(getArguments().getString(KEY_QUERY));

        return view;
    }

    private void initView(View view) {
        //init RecyclerView for Search results
        rvSearch = (RecyclerView) view.findViewById(R.id.rv_search);
        rvSearch.setVisibility(View.VISIBLE);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        rvSearch.setLayoutManager(layoutManager);
        rvSearch.setHasFixedSize(true);
        mSearchAdapter = new SearchAdapter(1, this, getActivity());
        rvSearch.setAdapter(mSearchAdapter);
        rvSearch.setVisibility(View.GONE);

        searchLoadingIndicator = (ProgressBar) view.findViewById(R.id.search_loading_indicator);
        ivNoSearchResults = (ImageView)view.findViewById(R.id.noSearchResults);

        searchLoadingIndicator.setVisibility(View.VISIBLE);
    }

    public void query(String query) {
        Log.e(TAG, "activity: " + getActivity());
        SearchHelper searchHelper = new SearchHelper(getActivity(), query, this);
        searchHelper.search();
    }

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
                            v.setScaleX(1.3f);
                            v.setScaleY(1.3f);
                            v.animate().alpha(1.0f)
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(300)
                                    .setStartDelay(i * 150)
                                    .start();
                        }

                        return true;
                    }
                });


//        Log.i(TAG, "visibility: " + rvSearch.getVisibility());
    }

    /**
     * SeachItem click
     * @param position clicked position
     */
//    @Override
    public void onListItemClick(int position) {
        Log.i(TAG, "onListItemClick : " + position);
        ArrayList<ContentValues> data = mSearchAdapter.getData();
        Movie movie = new Movie(data.get(position));
        Intent intent = new Intent(getActivity(), DetailActivity.class);
        intent.putExtra(DetailActivity.INTENT_MOVIE_KEY, movie);
        startActivity(intent);

//        ActivityOptionsCompat activityOptionsCompat =
//                ActivityOptionsCompat.makeSceneTransitionAnimation(
//                        this, layoutManager.findViewByPosition(position), "poster");
//        ActivityCompat.startActivity(this, intent, activityOptionsCompat.toBundle());
    }
}
