package com.example.android.popularmovies;

import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.example.android.popularmovies.data.MovieContract.MovieEntry;
import com.example.android.popularmovies.databinding.ActivityDetailBinding;
import com.squareup.picasso.Picasso;

public class DetailActivity extends AppCompatActivity {

    private final static String TAG = DetailActivity.class.getSimpleName();

    public static final String INDEX_KEY = "index_key";
//    public final static String MOVIE_TITLE = "movie_title";
//    public final static String MOVIE_VOTE_AVERAGE = "movie_vote_average";
//    public final static String MOVIE_OVERVIEW = "movie_overview";
//    public final static String MOVIE_RELEASE_DATE = "movie_release_date";
//    public final static String MOVIE_POSTER_PATH = "movie_poster_path";
//    public final static String MOVIE_ORIGINAL_TITLE = "movie_original_title";

    private static int mIndex;

    ActivityDetailBinding mDataBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_detail);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        loadDataFromDB();
//        adjustThumbnailSize();

//        ScrollView scrollView = (ScrollView) findViewById(R.id.d_scrollview);
//        scrollView.smoothScrollTo(0,0);
    }

    private void loadDataFromDB() {
        Intent intent = getIntent();
        if (!intent.hasExtra(INDEX_KEY))
            return;
        mIndex = intent.getIntExtra(INDEX_KEY, 0);

        new MovieTask().execute();
    }

    private class MovieTask extends AsyncTask<Uri, Void, Cursor> {

        @Override
        protected Cursor doInBackground(Uri... params) {
            Log.i(TAG, "background: " + params.length);
            return getContentResolver().query(
                    MovieEntry.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
            );

        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            updateUI(cursor);
        }
    }

    private void updateUI(Cursor cursor) {
        cursor.moveToPosition(mIndex);

        String title = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_TITLE));
        String backdropPath = cursor.getString(
                cursor.getColumnIndex(MovieEntry.COLUMN_BACKDROP_PATH));

        mDataBinding.textViewTitle.setText(title);

        Picasso.with(this).load(
                "https://image.tmdb.org/t/p/w500" +
                        backdropPath)
                .into(mDataBinding.imageViewBackdrop);

//        Log.i(TAG, backdropPath);
        scaleBackdrop();
    }

    private void scaleBackdrop() {

        ViewGroup.LayoutParams params = mDataBinding.frameLayoutBackdrop.getLayoutParams();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = displaymetrics.widthPixels;
        Log.i(TAG, "width = " + width);
        int height = ((int) (width * 0.562));
        params.height = height;
        Log.i(TAG, "height = " + height);

        mDataBinding.frameLayoutBackdrop.setLayoutParams(params);

    }

//    private void adjustThumbnailSize() {
//        RelativeLayout rlThumbnail = (RelativeLayout) findViewById(R.id.layout_thumbnail);
//        ViewGroup.LayoutParams layoutParams = rlThumbnail.getLayoutParams();
//        DisplayMetrics displaymetrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
//        int displayWidth = displaymetrics.widthPixels;
//        ViewGroup.LayoutParams params = ivThumbnail.getLayoutParams();
//        Log.i(TAG, "thumbnail: " + params.width + "x" + layoutParams.height);
//        params.width = displayWidth / 2;
//        layoutParams.height = (int) ((displayWidth / 2) * 1.45);
//        ivThumbnail.setLayoutParams(params);
//        rlThumbnail.setLayoutParams(layoutParams);
//    }

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
