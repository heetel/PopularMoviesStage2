package com.heetel.android.popularmovies.utilities;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.heetel.android.popularmovies.data.MovieContract;

/**
 * Created by Julian Heetel on 27.09.2017.
 *
 */

public class FavouritesUtil {

    private Context context;
    private FavouritesUtilCallback favouritesUtilCallback;
    private int favouritesCount;
    private boolean init;

    public FavouritesUtil(FavouritesUtilCallback favouritesUtilCallback) {
        this.context = ((Context) favouritesUtilCallback);
        this.favouritesUtilCallback = favouritesUtilCallback;
    }

    public void checkCount() {
        new FavouritesCountTask().execute(context);
    }

    private class FavouritesCountTask extends AsyncTask<Context, Void, Integer> {

        @Override
        protected Integer doInBackground(Context... params) {
            if (params == null) return 0;
            Cursor result = params[0].getContentResolver().query(
                    MovieContract.MovieEntry.CONTENT_URI_FAVOURITES,
                    null,
                    null,
                    null,
                    null);
            if (result == null) return 0;
            return result.getCount();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            if (init && favouritesCount < integer)
                favouritesUtilCallback.newFavourite();
            init = true;
            favouritesCount = integer;
        }
    }

    public interface FavouritesUtilCallback {
        void newFavourite();
    }

}
