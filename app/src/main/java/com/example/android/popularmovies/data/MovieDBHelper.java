package com.example.android.popularmovies.data;

import com.example.android.popularmovies.data.MovieContract.MovieEntry;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Julian Heetel on 09.03.2017.
 */

public class MovieDBHelper extends SQLiteOpenHelper {

    // The name of the database
    private static final String DATABASE_NAME = "moviesDb.db";

    // If you change the database schema, you must increment the database version
    private static final int VERSION = 9;

    public MovieDBHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        final String CREATE_TABLE = getCreateString(MovieEntry.TABLE_NAME);

        final String CREATE_TABLE_TOP_RATED = getCreateString(MovieEntry.TABLE_NAME_TOP_RATED);

        final String CREATE_TABLE_FAVOURITES = getCreateString(MovieEntry.TABLE_NAME_FAVOURITES);

        db.execSQL(CREATE_TABLE);
        db.execSQL(CREATE_TABLE_TOP_RATED);
        db.execSQL(CREATE_TABLE_FAVOURITES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MovieEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + MovieEntry.TABLE_NAME_TOP_RATED);
        db.execSQL("DROP TABLE IF EXISTS " + MovieEntry.TABLE_NAME_FAVOURITES);
        onCreate(db);
        Log.i("Database", "upgrade");
    }

    private String getCreateString(String tableName) {
        return "CREATE TABLE " + tableName + " (" +
                MovieEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MovieEntry.COLUMN_CREATE_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                MovieEntry.COLUMN_MOVIE_ID + " INTEGER UNIQUE, " +
                MovieEntry.COLUMN_TITLE + " TEXT, " +
                MovieEntry.COLUMN_ORIGINAL_TITLE + " TEXT, " +
                MovieEntry.COLUMN_OVERVIEW + " TEXT, " +
                MovieEntry.COLUMN_RELEASE_DATE + " TEXT, " +
                MovieEntry.COLUMN_VOTE_AVERAGE + " TEXT, " +
                MovieEntry.COLUMN_POSTER_PATH + " TEXT, " +
                MovieEntry.COLUMN_BACKDROP_PATH + " TEXT, " +
                MovieEntry.COLUMN_VIDEOS_NAMES + " TEXT, " +
                MovieEntry.COLUMN_VIDEOS_KEYS + " TEXT, " +
                MovieEntry.COLUMN_REVIEWS_AUTHORS + " TEXT, " +
                MovieEntry.COLUMN_REVIEWS_CONTENTS + " TEXT);";
    }
}
