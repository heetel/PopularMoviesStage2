package com.example.android.popularmovies.utilities;

import android.content.ContentValues;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Julian Heetel on 10.03.2017.
 *
 */

public class ListUtil {

    private static final String TAG = ListUtil.class.getSimpleName();

    public static final String DELIMITER = "$%";

    public static ContentValues[] makeContentValuesArray(ArrayList<ContentValues> values) {
        ContentValues[] array = new ContentValues[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }

    public static String convertArrayToString(String[] array, String delimiter){
        String str = "";
        for (int i = 0;i<array.length; i++) {
            str = str+array[i];
            // Do not append comma at the end of last element
            if(i<array.length-1){
                str = str+delimiter;
            }
        }
        return str;
    }
    public static String[] convertStringToArray(String str, String delimiter){
//        Log.i(TAG, "toArray input: " + str);
        String[] arr = str.split(delimiter);
//        Log.i(TAG, "output length: " + arr.length);
        return arr;
    }

}
