package com.example.android.popularmovies;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.android.popularmovies.data.MovieContract.MovieEntry;
import com.example.android.popularmovies.databinding.ActivityDetailBinding;
import com.squareup.picasso.Picasso;

public class DetailActivity extends AppCompatActivity {

    private final static String TAG = DetailActivity.class.getSimpleName();

    public static final String INDEX_KEY = "index_key";
    public static final String TABLE_KEY = "table_key";
//    public final static String MOVIE_TITLE = "movie_title";
//    public final static String MOVIE_VOTE_AVERAGE = "movie_vote_average";
//    public final static String MOVIE_OVERVIEW = "movie_overview";
//    public final static String MOVIE_RELEASE_DATE = "movie_release_date";
//    public final static String MOVIE_POSTER_PATH = "movie_poster_path";
//    public final static String MOVIE_ORIGINAL_TITLE = "movie_original_title";

    private  int mIndex;
    private  boolean sIsFavourite;
    private  ContentValues mValues;
    private  String mMovieId;

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
        if (!intent.hasExtra(INDEX_KEY) || !intent.hasExtra(TABLE_KEY))
            return;
        mIndex = intent.getIntExtra(INDEX_KEY, 0);

        int mTableKey = intent.getIntExtra(TABLE_KEY, MainActivity.CODE_POPULAR);

        Uri uri = MainActivity.getActiveTableUri(mTableKey);

        new MovieTask().execute(uri);
    }

    private class MovieTask extends AsyncTask<Uri, Void, Cursor> {

        @Override
        protected Cursor doInBackground(Uri... params) {
            if (params.length < 1)
                throw new IllegalArgumentException("Missing ContentUri to load from");

            return getContentResolver().query(
                    params[0],
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

    private class CheckFavouritesTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... strings) {
            if (strings.length < 1)
                return null;

            String mSelection = MovieEntry.COLUMN_MOVIE_ID + "=?";

            boolean isFavourite;

            Cursor cursor = getContentResolver().query(MovieEntry.CONTENT_URI_FAVOURITES,
                    null,
                    mSelection,
                    strings,
                    null);

            if (cursor == null)
                return false;

            isFavourite = cursor.getCount() > 0;

            cursor.close();

            return isFavourite;
        }

        @Override
        protected void onPostExecute(Boolean isFavourite) {
            if (isFavourite) {
                sIsFavourite = true;
                mDataBinding.imageButtonFavourite.setImageResource(R.drawable.ic_star_blue);
            }
        }
    }

    private void updateUI(Cursor cursor) {
        cursor.moveToPosition(mIndex);

        mMovieId = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_MOVIE_ID));
        String title = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_TITLE));
        String releaseDate = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_RELEASE_DATE));
        String vote = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_VOTE_AVERAGE));
        String originalTitle = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_ORIGINAL_TITLE));
        String backdropPath = cursor.getString(
                cursor.getColumnIndex(MovieEntry.COLUMN_BACKDROP_PATH));
        String posterPath = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_POSTER_PATH));
        String overview = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_OVERVIEW));

        mDataBinding.textViewTitle.setText(title);
        mDataBinding.originalTitle.setText(originalTitle);
        mDataBinding.vote.setText(vote);
        mDataBinding.date.setText(releaseDate);
        mDataBinding.overview.setText(overview);

        Picasso.with(this).load(
                "https://image.tmdb.org/t/p/w500" +
                        backdropPath)
                .into(mDataBinding.imageViewBackdrop);

        Picasso.with(this).load(
                "https://image.tmdb.org/t/p/w500" +
                        posterPath)
                .into(mDataBinding.imageViewPoster);

        scaleBackdrop();

        //Check if shown movie is a favourite and update the favourite ImageButton
        new CheckFavouritesTask().execute(mMovieId);

        //put data into ContentValues
        mValues = new ContentValues();
        mValues.put(MovieEntry.COLUMN_MOVIE_ID, mMovieId);
        mValues.put(MovieEntry.COLUMN_TITLE, title);
        mValues.put(MovieEntry.COLUMN_RELEASE_DATE, releaseDate);
        mValues.put(MovieEntry.COLUMN_VOTE_AVERAGE, vote);
        mValues.put(MovieEntry.COLUMN_ORIGINAL_TITLE, originalTitle);
        mValues.put(MovieEntry.COLUMN_BACKDROP_PATH, backdropPath);
        mValues.put(MovieEntry.COLUMN_POSTER_PATH, posterPath);
        mValues.put(MovieEntry.COLUMN_OVERVIEW, overview);
    }

    private void scaleBackdrop() {

        ViewGroup.LayoutParams params = mDataBinding.frameLayoutBackdrop.getLayoutParams();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int orientation = getResources().getConfiguration().orientation;

        Log.i(TAG, "Orientation: " + orientation);
        int width;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            width = displaymetrics.widthPixels;
        }
        else {
            width = ((int) (displaymetrics.widthPixels * 0.7));
        }
        Log.i(TAG, "width = " + width);
        int height = ((int) (width * 0.562));
        params.height = height;
        Log.i(TAG, "height = " + height);

        mDataBinding.frameLayoutBackdrop.setLayoutParams(params);

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

    public void onClickFavourite(View view) {
        if (sIsFavourite) {
            mDataBinding.imageButtonFavourite.setImageResource(R.drawable.ic_star_border_blue);
            sIsFavourite = false;
            Toast.makeText(this, "Removed from Favourites", Toast.LENGTH_SHORT).show();
            int rows = getContentResolver().delete(
                    MovieEntry.CONTENT_URI_FAVOURITES,
                    MovieEntry.COLUMN_MOVIE_ID + "=?",
                    new String[]{mMovieId}
            );
            Log.i(TAG, rows + " rows deleted");
        } else {
            mDataBinding.imageButtonFavourite.setImageResource(R.drawable.ic_star_blue);
            Toast.makeText(this, "Added to Favourites", Toast.LENGTH_SHORT).show();
            sIsFavourite = true;
            Uri uri = getContentResolver().insert(MovieEntry.CONTENT_URI_FAVOURITES, mValues);
            Log.i(TAG, "Added to Favourites: " + uri);
        }
    }
}
