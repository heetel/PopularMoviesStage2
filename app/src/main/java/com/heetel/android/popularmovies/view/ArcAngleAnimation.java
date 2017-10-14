package com.heetel.android.popularmovies.view;

import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by Julian Heetel on 04.10.2017.
 *
 */

public class ArcAngleAnimation extends Animation {

    private RatingView ratingView;

    private float oldAngle;
    private float newAngle;

    public ArcAngleAnimation(RatingView ratingView, int newAngle) {
        this.oldAngle = ratingView.getAngle();
        this.newAngle = newAngle;
        this.ratingView = ratingView;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation) {
        float angle = 0 + ((newAngle - oldAngle) * interpolatedTime);

        ratingView.setRating((int)angle);
//        ratingView.requestLayout();
    }
}