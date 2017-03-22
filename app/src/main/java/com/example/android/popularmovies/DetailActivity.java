package com.example.android.popularmovies;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.android.popularmovies.data.MovieContract.MovieEntry;
import com.example.android.popularmovies.databinding.ActivityDetailBinding;
import com.example.android.popularmovies.databinding.VideoItemBinding;
import com.example.android.popularmovies.utilities.ListUtil;
import com.example.android.popularmovies.utilities.NetworkUtils;
import com.squareup.picasso.Picasso;

import java.io.IOException;

/**
 * Created by Julian Heetel
 *
 * The DetailActivity loads the movie information from ContentProvider using index and table key
 * from the Intent that started this Activity.
 * In case the videos and reviews are not yet in the Database of the ContentProvider, this Activity
 * loads the videos and reviews with a AsyncTaskLoader from the TheMovieDB API into the
 * ContentProvider and then updates the UI.
 */
public class DetailActivity extends AppCompatActivity
    implements LoaderManager.LoaderCallbacks<ContentValues>{

    private final static String TAG = DetailActivity.class.getSimpleName();

    //Keys for Intent
    public static final String INTENT_TABLE_KEY = "intent_table_key";
    public static final String INTENT_MOVIE_ID_KEY = "intent_movie_id_key";

    //for AsyncTaskLoader
    private final static int LOADER_ID = 420;
    //Column key to query ContentProvider
    private static final String MOVIE_ID_KEY = "movie_id_key";

    private boolean sIsFavourite;
    private ContentValues mValues;
    private String mMovieId;
    private String[] mVideoKeys;
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

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            getWindow().setNavigationBarColor(getResourceColor(R.color.navigationBarColor));

        loadDataFromDB();
    }

    /**
     * Load trailers and reviews from API
     */
    private void loadDetails() {
        Bundle bundle = new Bundle();
        bundle.putString(MOVIE_ID_KEY, mMovieId);
        getSupportLoaderManager().initLoader(LOADER_ID, bundle, this);
    }

    /**
     * Load data from ContentProvider
     */
    private void loadDataFromDB() {
        Intent intent = getIntent();
        if (!intent.hasExtra(INTENT_MOVIE_ID_KEY) || !intent.hasExtra(INTENT_TABLE_KEY))
            return;
        mMovieId = intent.getStringExtra(INTENT_MOVIE_ID_KEY);

        int mTableKey = intent.getIntExtra(INTENT_TABLE_KEY, MainActivity.CODE_POPULAR);

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
                    MovieEntry.COLUMN_MOVIE_ID + "=?",
                    new String[]{mMovieId},
                    null
            );

        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            Log.i(TAG, "onPostExecute: cursor length: " + cursor.getCount());
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

    /**
     * Update UI with data from cursor
     * If tha data doesn't contain trailers and reviews, loadDetails() will be called.
     * @param cursor data
     */
    private void updateUI(Cursor cursor) {
        if (cursor == null) return;

        cursor.moveToFirst();

        String title = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_TITLE));
        String releaseDate = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_RELEASE_DATE));
        String vote = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_VOTE_AVERAGE));
        String originalTitle = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_ORIGINAL_TITLE));
        String backdropPath = cursor.getString(
                cursor.getColumnIndex(MovieEntry.COLUMN_BACKDROP_PATH));
        String posterPath = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_POSTER_PATH));
        String overview = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_OVERVIEW));

        String videoNamesArray = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_VIDEOS_NAMES));

        if (videoNamesArray == null) {
            loadDetails();
        } else {
            String[] mVideoNames = ListUtil.convertStringToArray(videoNamesArray, ",");
            String videoKeysArray = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_VIDEOS_KEYS));
            mVideoKeys = ListUtil.convertStringToArray(videoKeysArray, ",");

            VideoItemBinding[] mVideoItems = new VideoItemBinding[]{
                    mDataBinding.videoItem1,
                    mDataBinding.videoItem2,
                    mDataBinding.videoItem3,
                    mDataBinding.videoItem4,
                    mDataBinding.videoItem5
            };

            mDataBinding.videosLabel.setVisibility(View.VISIBLE);

            Log.i(TAG, "mVideoKeys length: " + mVideoKeys.length);
            for (int i = 0; i < mVideoKeys.length && i < mVideoItems.length; i++) {
                mVideoItems[i].videoItemFrameLayout.setVisibility(View.VISIBLE);
                mVideoItems[i].textViewVideoName.setText(mVideoNames[i]);
            }

            if (TextUtils.isEmpty(mVideoKeys[0])) {
                mVideoItems[0].videoItemFrameLayout.setVisibility(View.GONE);
                mDataBinding.noVideos.setVisibility(View.VISIBLE);
            }

            String reviewAuthorsArray = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_REVIEWS_AUTHORS));
            String[] reviewAuthors = ListUtil.convertStringToArray(reviewAuthorsArray, ListUtil.DELIMITER);
            String reviewContensArray = cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_REVIEWS_CONTENTS));
            String[] reviewContents = ListUtil.convertStringToArray(reviewContensArray, ListUtil.DELIMITER);
            Log.i(TAG, reviewAuthorsArray);

            Log.i(TAG, "reviewAuthors length: " + reviewAuthors.length);
            if (reviewAuthors.length > 0) {
                mDataBinding.reviewsLabel.setVisibility(View.VISIBLE);
                mDataBinding.reviews.setVisibility(View.VISIBLE);


                String reviews = "";
                for (int i = 0; i < reviewAuthors.length; i++) {
                    reviews += "<b>" + reviewAuthors[i] + ":  </b><br>" +
                            reviewContents[i] + "<br><br>";
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mDataBinding.reviews.setText(Html.fromHtml(reviews, Html.FROM_HTML_MODE_COMPACT));
                } else {
                    //noinspection deprecation
                    mDataBinding.reviews.setText(Html.fromHtml(reviews));
                }
            }

            if (TextUtils.isEmpty(reviewContents[0])) {
                mDataBinding.reviews.setVisibility(View.GONE);
                mDataBinding.noReviews.setVisibility(View.VISIBLE);
            }
        }

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

        //Apply custom font
        Typeface font = Typeface.createFromAsset(getAssets(), "Ubuntu-L.ttf");
        mDataBinding.textViewTitle.setTypeface(font);

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

    /**
     * Adjust height of backdrop ImageView depending on screen width
     */
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
            width = mDataBinding.frameLayoutBackdrop.getMeasuredWidth();
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

    public void onClickVideo(View view) {
        Log.i(TAG, "View: " + view.getId());
        if (mVideoKeys == null) return;

        switch (view.getId()) {
            case R.id.videoItem1:
                watchYoutubeVideo(mVideoKeys[0]);
                break;
            case R.id.videoItem2:
                watchYoutubeVideo(mVideoKeys[1]);
                break;
            case R.id.videoItem3:
                watchYoutubeVideo(mVideoKeys[2]);
                break;
            case R.id.videoItem4:
                watchYoutubeVideo(mVideoKeys[3]);
                break;
            case R.id.videoItem5:
                watchYoutubeVideo(mVideoKeys[4]);
                break;
        }
    }

    public void onClickFavourite(View view) {
        if (sIsFavourite) {
            mDataBinding.imageButtonFavourite.setImageResource(R.drawable.ic_star_border_blue);
            sIsFavourite = false;
            Toast.makeText(this, getString(R.string.removed_from_favourites),
                    Toast.LENGTH_SHORT).show();
            int rows = getContentResolver().delete(
                    MovieEntry.CONTENT_URI_FAVOURITES,
                    MovieEntry.COLUMN_MOVIE_ID + "=?",
                    new String[]{mMovieId}
            );
            Log.i(TAG, rows + " rows deleted");
        } else {
            mDataBinding.imageButtonFavourite.setImageResource(R.drawable.ic_star_blue);
            Toast.makeText(this, getString(R.string.added_to_favourites),
                    Toast.LENGTH_SHORT).show();
            sIsFavourite = true;
            Uri uri = getContentResolver().insert(MovieEntry.CONTENT_URI_FAVOURITES, mValues);
            Log.i(TAG, "Added to Favourites: " + uri);
        }
    }

public Loader<ContentValues> onCreateLoader(int id, final Bundle args) {
    return new AsyncTaskLoader<ContentValues>(this) {

        //cached data
        ContentValues cachedDetails;

        @Override
        protected void onStartLoading() {
            if (args == null) return;
            if (!args.containsKey(MOVIE_ID_KEY)) return;

            if (cachedDetails != null) {
                deliverResult(cachedDetails);
            } else {
                mDataBinding.pbDetailLoadingIndicator.setVisibility(View.VISIBLE);
                forceLoad();
            }
        }

        @Override
        public ContentValues loadInBackground() {
            String movieId = args.getString(MOVIE_ID_KEY);

            try {
                return NetworkUtils.getDetailsFromMovieId(movieId);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public void deliverResult(ContentValues data) {
            cachedDetails = data;
            super.deliverResult(data);
        }
    };
}

    @Override
    public void onLoadFinished(Loader<ContentValues> loader, ContentValues data) {
        mDataBinding.pbDetailLoadingIndicator.setVisibility(View.INVISIBLE);
        if (data == null) return;

        String where = "movie_id=?";
        String[] selectionArgs = new String[]{mMovieId};

        int rowsPopular = getContentResolver().update(
                MovieEntry.CONTENT_URI,
                data,
                where,
                selectionArgs
        );
        int rowsTopRated = getContentResolver().update(
                MovieEntry.CONTENT_URI_TOP_RATED,
                data,
                where,
                selectionArgs
        );
        int rowsFavourites = getContentResolver().update(
                MovieEntry.CONTENT_URI_FAVOURITES,
                data,
                where,
                selectionArgs
        );
        Log.i(TAG, "rows updated popular: " + rowsPopular);
        Log.i(TAG, "rows updated top rated: " + rowsTopRated);
        Log.i(TAG, "rows updated favourites: " + rowsFavourites);

        loadDataFromDB();
    }

    @Override
    public void onLoaderReset(Loader<ContentValues> loader) {
    }

    private void watchYoutubeVideo(String id){
        if (TextUtils.isEmpty(id)) return;

        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://www.youtube.com/watch?v=" + id));
        try {
            startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            startActivity(webIntent);
        }
    }

    private int getResourceColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return ContextCompat.getColor(this, color);
        else
            //noinspection deprecation
            return getResources().getColor(color);
    }
}
