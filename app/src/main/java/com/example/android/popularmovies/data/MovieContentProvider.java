package com.example.android.popularmovies.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by Julian Heetel on 09.03.2017.
 */

public class MovieContentProvider extends ContentProvider {

    private static final String TAG = MovieContentProvider.class.getSimpleName();

    public static final int MOVIES = 100;
    public static final int MOVIES_WITH_ID = 101;

    public static final int TOP_RATED = 200;
    public static final int TOP_RATED_WITH_ID = 201;

    public static final int FAVOURITES = 300;
    public static final int FAVOURITES_WITH_ID = 301;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    public static UriMatcher buildUriMatcher() {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        //add Uri for all movies
        uriMatcher.addURI(MovieContract.AUTHORITY, MovieContract.PATH_MOVIES, MOVIES);
        //add Uri for one specific movie
        uriMatcher.addURI(MovieContract.AUTHORITY, MovieContract.PATH_MOVIES + "/#", MOVIES_WITH_ID);

        uriMatcher.addURI(MovieContract.AUTHORITY, MovieContract.PATH_TOP_RATED, TOP_RATED);
        uriMatcher.addURI(
                MovieContract.AUTHORITY, MovieContract.PATH_TOP_RATED + "/#", TOP_RATED_WITH_ID);

        uriMatcher.addURI(MovieContract.AUTHORITY, MovieContract.PATH_FAVOURITES, FAVOURITES);
        uriMatcher.addURI(
                MovieContract.AUTHORITY, MovieContract.PATH_FAVOURITES + "/#", FAVOURITES_WITH_ID);

        return uriMatcher;
    }

    private MovieDBHelper mMovieDBHelper;

    @Override
    public boolean onCreate() {
        mMovieDBHelper = new MovieDBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        final SQLiteDatabase db = mMovieDBHelper.getReadableDatabase();

        int match = sUriMatcher.match(uri);

        Cursor returnCursor;

        switch (match) {
            case MOVIES:
                returnCursor = db.query(
                        MovieContract.MovieEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case MOVIES_WITH_ID:
                Log.i(TAG, "Movies with ID loading");
                //get ID from uri
                String id = uri.getPathSegments().get(1);
                Log.i(TAG, "_ID: " + id);
                //Create selection and selection with id (SQL WHERE Clause)
                String mSelection = "_id=?";
                String[] mSelectionArgs = new String[]{id};

                returnCursor = db.query(
                        MovieContract.MovieEntry.TABLE_NAME,
                        projection,
                        mSelection,
                        mSelectionArgs,
                        null,
                        null,
                        sortOrder
                );

                Log.i(TAG, "CursorCount: " + returnCursor.getCount());
                break;
            case TOP_RATED:
                returnCursor = db.query(
                        MovieContract.MovieEntry.TABLE_NAME_TOP_RATED,
                        projection,
                        selection,
                        selectionArgs,
                        null, null,
                        sortOrder
                );
                break;
            case TOP_RATED_WITH_ID:
                //get ID from uri
                String idTopRated = uri.getPathSegments().get(1);

                //Create selection and selection with id (SQL WHERE Clause)
                String mSelectionTopRated = "_id=?";
                String[] mSelectionArgsTopRated = new String[]{idTopRated};

                returnCursor = db.query(
                        MovieContract.MovieEntry.TABLE_NAME,
                        projection,
                        mSelectionTopRated,
                        mSelectionArgsTopRated,
                        null,
                        null,
                        sortOrder
                );
                break;
            case FAVOURITES:
                returnCursor = db.query(
                        MovieContract.MovieEntry.TABLE_NAME_FAVOURITES,
                        projection,
                        selection,
                        selectionArgs,
                        null, null,
                        sortOrder
                );
                break;
            case FAVOURITES_WITH_ID:
                //get ID from uri
                String idFavourites = uri.getPathSegments().get(1);

                //Create selection and selection with id (SQL WHERE Clause)
                String mSelectionFavourites = "_id=?";
                String[] mSelectionArgsFavourites = new String[]{idFavourites};

                returnCursor = db.query(
                        MovieContract.MovieEntry.TABLE_NAME_FAVOURITES,
                        projection,
                        mSelectionFavourites,
                        mSelectionArgsFavourites,
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        returnCursor.setNotificationUri(getContext().getContentResolver(), uri);

        return returnCursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {

        final SQLiteDatabase db = mMovieDBHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);

        Uri returnUri;

        switch (match) {
            case MOVIES:
                long id = db.insert(MovieContract.MovieEntry.TABLE_NAME, null, values);
                if (id > 0) {
                    //success
                    returnUri = ContentUris.withAppendedId(
                            MovieContract.MovieEntry.CONTENT_URI, id);
                } else {
                    throw new SQLException("Failed to insert row into: " + uri);
                }
                break;
            case TOP_RATED:
                throw new UnsupportedOperationException("Not implemented");
            case FAVOURITES:
                long idFavourites = db.insert(
                        MovieContract.MovieEntry.TABLE_NAME_FAVOURITES, null, values);
                if (idFavourites > 0) {
                    //success
                    returnUri = ContentUris.withAppendedId(
                            MovieContract.MovieEntry.CONTENT_URI_FAVOURITES, idFavourites);
                } else {
                    throw new SQLException("Failed to insert row into: " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        Log.i(TAG, "inserted " + values.getAsString(MovieContract.MovieEntry.COLUMN_TITLE));
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {

        final SQLiteDatabase db = mMovieDBHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);

        int moviesDeleted;

        switch (match) {
            case MOVIES:
                moviesDeleted = db.delete(
                        MovieContract.MovieEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            case TOP_RATED:
                moviesDeleted = db.delete(
                        MovieContract.MovieEntry.TABLE_NAME_TOP_RATED,
                        selection,
                        selectionArgs
                );
                break;
            case FAVOURITES:
                moviesDeleted = db.delete(
                        MovieContract.MovieEntry.TABLE_NAME_FAVOURITES,
                        selection,
                        selectionArgs
                );
                break;
            case FAVOURITES_WITH_ID:
                //get ID from uri
                String idFavourites = uri.getPathSegments().get(1);

                //Create selection and selection with id (SQL WHERE Clause)
                String mSelectionFavourites = "_id=?";
                String[] mSelectionArgsFavourites = new String[]{idFavourites};

                moviesDeleted = db.delete(
                        MovieContract.MovieEntry.TABLE_NAME_FAVOURITES,
                        mSelectionFavourites,
                        mSelectionArgsFavourites
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (moviesDeleted != 0)
            getContext().getContentResolver().notifyChange(uri, null);

        return moviesDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase db = mMovieDBHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);
        //TODO implement
        throw new UnsupportedOperationException();
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {

        final SQLiteDatabase db = mMovieDBHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);

        switch (match) {
            case MOVIES:
                db.beginTransaction();
                int rowsInserted = 0;

                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(
                                MovieContract.MovieEntry.TABLE_NAME,
                                null,
                                value
                        );

                        if (_id != -1) {
                            //success
                            rowsInserted++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    //after successful transaction
                    db.endTransaction();
                }

                if (rowsInserted > 0)
                    getContext().getContentResolver().notifyChange(uri, null);

                return rowsInserted;
            case TOP_RATED:
                db.beginTransaction();
                rowsInserted = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(
                                MovieContract.MovieEntry.TABLE_NAME_TOP_RATED,
                                null,
                                value
                        );

                        if (_id != -1) {
                            //success
                            rowsInserted++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    //after successful transaction
                    db.endTransaction();
                }

                if (rowsInserted > 0)
                    getContext().getContentResolver().notifyChange(uri, null);

                return rowsInserted;
            case FAVOURITES:
                throw new UnsupportedOperationException("Not implemented.");
            default:
                return super.bulkInsert(uri, values);
        }
    }
}
