package com.heetel.android.popularmovies.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.heetel.android.popularmovies.R;

/**
 * Created by Julian Heetel on 03.10.2017.
 *
 */

public class RatingView extends View {


    private static final String TAG = RatingView.class.getSimpleName();
    private static final int DEFAULT_STROKE_WIDTH = 10;
    private int mColorUnfilled;
    private int mRating;
    private int mColor;
    private Paint mPaint, mTextPaint;
    private int mWidth;
    private int mHeight;
    private float mAngle;
    private int centerX;
    private int centerY;
    private int mStrokeWidth;
    private int mTextColor;
    private int mTextHeight;
    private Paint mPaintBackground;
    private boolean mShowText;

    public RatingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.RatingView,
                0, 0);
        try {
            mRating = a.getInteger(R.styleable.RatingView_rating, 50);
            mColor = a.getColor(R.styleable.RatingView_color, Color.WHITE);
            mColorUnfilled = a.getColor(R.styleable.RatingView_colorUnfilled, Color.parseColor("#50ffffff"));
            mTextHeight = a.getDimensionPixelSize(R.styleable.RatingView_textSize, -1);
            mShowText = a.getBoolean(R.styleable.RatingView_showTextInCircle, true);
            mStrokeWidth = a.getDimensionPixelSize(R.styleable.RatingView_strokeWidth, DEFAULT_STROKE_WIDTH);
        } finally {
            a.recycle();
        }
        Log.i(TAG, "init RatingView: " + mRating);
        Log.i(TAG, "init textHeight: " + mTextHeight);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mColor);
        mPaint.setStrokeWidth(mStrokeWidth);


        mPaintBackground = new Paint(mPaint);
        mPaintBackground.setColor(mColorUnfilled);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextColor = Color.WHITE;
        mTextPaint.setColor(mTextColor);
//        mTextHeight = 54;
        mTextPaint.setTextSize(mTextHeight);
        mTextPaint.setTextAlign(Paint.Align.CENTER);



        centerX = (mWidth / 2) - (mStrokeWidth * 2);
        centerY = (mHeight / 2) - (mStrokeWidth * 2);
        mAngle = (((float) mRating) / 100f) * 360f;

        /*
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mTextColor);
        if (mTextHeight == 0) {
            mTextHeight = mTextPaint.getTextSize();
        } else {
            mTextPaint.setTextSize(mTextHeight);
        }

        mPiePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPiePaint.setStyle(Paint.Style.FILL);
        mPiePaint.setTextSize(mTextHeight);

        mShadowPaint = new Paint(0);
        mShadowPaint.setColor(0xff101010);
        mShadowPaint.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL));
        */
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.i(TAG, "onSizeChanged("+w+", "+h);
        mWidth = w;
        mHeight = h;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(mStrokeWidth, mStrokeWidth, mWidth - mStrokeWidth, mHeight - mStrokeWidth, 270, 360 , false, mPaintBackground);
        canvas.drawArc(mStrokeWidth, mStrokeWidth, mWidth - mStrokeWidth, mHeight - mStrokeWidth, 270, mAngle, false, mPaint);
        if (mShowText)
            canvas.drawText(String.valueOf(mRating) + "%", mWidth / 2, (mHeight / 2) + (mTextPaint.getTextSize() / 2.0f), mTextPaint);
    }

    public void setColor(int color) {
        mColor = color;
        mPaint.setColor(mColor);
        invalidate();
        requestLayout();
    }

    public int getRating() {
        return mRating;
    }

    public int getAngle() {
        return (int) mAngle;
    }

    public void setRating(int rating) {
        mAngle = (rating / 100f) * 360f;
        mRating = rating;
        invalidate();
        requestLayout();
    }

    @Override
    public int getBaseline() {
        return (mHeight / 2) + (mTextHeight / 2);
    }
}