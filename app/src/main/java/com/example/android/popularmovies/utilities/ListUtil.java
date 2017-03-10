package com.example.android.popularmovies.utilities;

import android.content.ContentValues;

import java.util.ArrayList;

/**
 * Created by Julian Heetel on 10.03.2017.
 */

public class ListUtil {

    public static ContentValues[] makeContentValuesArray(ArrayList<ContentValues> values) {
        ContentValues[] array = new ContentValues[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }

}
