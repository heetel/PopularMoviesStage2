package com.heetel.android.popularmovies.utilities;

import android.graphics.Color;

/**
 * Created by Julian Heetel on 14.10.2017.
 *
 */

public class ColorUtil {

    public static int manipulateColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
                Math.min(r, 255),
                Math.min(g, 255),
                Math.min(b, 255));
    }

}
