package com.example.android.popularmovies;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class DetailActivity extends AppCompatActivity {

    private final static String TAG = DetailActivity.class.getSimpleName();

    public final static String MOVIE_TITLE = "movie_title";
    public final static String MOVIE_VOTE_AVERAGE = "movie_vote_average";
    public final static String MOVIE_OVERVIEW = "movie_overview";
    public final static String MOVIE_RELEASE_DATE = "movie_release_date";
    public final static String MOVIE_POSTER_PATH = "movie_poster_path";
    public final static String MOVIE_ORIGINAL_TITLE = "movie_original_title";


    ImageView ivThumbnail;
    TextView tvMovieTitle, tvMovieVoteAverage, tvMovieReleaseDate, tvMovieOverview, tvOriginalTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        loadDataFromIntent();
        adjustThumbnailSize();

        ScrollView scrollView = (ScrollView) findViewById(R.id.d_scrollview);
        scrollView.smoothScrollTo(0,0);
    }

    private void loadDataFromIntent() {
        Intent intent = getIntent();

        tvMovieTitle = (TextView) findViewById(R.id.d_tv_movie_title);
        tvMovieVoteAverage = (TextView) findViewById(R.id.d_tv_vote_average);
        tvMovieReleaseDate = (TextView) findViewById(R.id.d_tv_release_date);
        tvMovieOverview = (TextView) findViewById(R.id.d_tv_movie_overview);
        tvOriginalTitle = (TextView) findViewById(R.id.d_tv_original_title);
        ivThumbnail = (ImageView) findViewById(R.id.d_iv_thumbnail);

        if (intent.hasExtra(MOVIE_TITLE))
            tvMovieTitle.setText(intent.getStringExtra(MOVIE_TITLE));
        if (intent.hasExtra(MOVIE_VOTE_AVERAGE))
            tvMovieVoteAverage.setText(intent.getStringExtra(MOVIE_VOTE_AVERAGE));
        if (intent.hasExtra(MOVIE_RELEASE_DATE))
            tvMovieReleaseDate.setText(intent.getStringExtra(MOVIE_RELEASE_DATE));
        if (intent.hasExtra(MOVIE_OVERVIEW))
            tvMovieOverview.setText(intent.getStringExtra(MOVIE_OVERVIEW));
        if (intent.hasExtra(MOVIE_ORIGINAL_TITLE))
            tvOriginalTitle.setText(intent.getStringExtra(MOVIE_ORIGINAL_TITLE));
        if (intent.hasExtra(MOVIE_POSTER_PATH)) {
            Picasso.with(DetailActivity.this).load(
                    "https://image.tmdb.org/t/p/w500" +
                            intent.getStringExtra(MOVIE_POSTER_PATH))
                    .into(ivThumbnail);
        }
    }

    private void adjustThumbnailSize() {
        RelativeLayout rlThumbnail = (RelativeLayout) findViewById(R.id.layout_thumbnail);
        ViewGroup.LayoutParams layoutParams = rlThumbnail.getLayoutParams();
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int displayWidth = displaymetrics.widthPixels;
        ViewGroup.LayoutParams params = ivThumbnail.getLayoutParams();
        Log.i(TAG, "thumbnail: " + params.width + "x" + layoutParams.height);
        params.width = displayWidth / 2;
        layoutParams.height = (int) ((displayWidth / 2) * 1.45);
        ivThumbnail.setLayoutParams(params);
        rlThumbnail.setLayoutParams(layoutParams);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
