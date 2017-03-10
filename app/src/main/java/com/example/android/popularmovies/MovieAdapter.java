package com.example.android.popularmovies;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import com.example.android.popularmovies.data.MovieContract.MovieEntry;

/**
 *
 * Created by Julian Heetel on 18.01.2017.
 */

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    public static final String TAG = MovieAdapter.class.getSimpleName();

    private int mNumberItems;

    private Context context;

    private Cursor mCursor;

    final private ListItemCallbackListener mOnClickListener;

    public interface ListItemCallbackListener {
        void onListItemClick(Cursor cursor, int clickedItemIndex);
        void onLoadMore();
    }

    public MovieAdapter(int numberItems, ListItemCallbackListener listener, Context context) {
        super();
        this.mNumberItems = numberItems;
        this.mOnClickListener = listener;
        this.context = context;
    }

    @Override
    public MovieViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForLisItem = R.layout.movie_list_item;
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = layoutInflater
                .inflate(layoutIdForLisItem, parent, shouldAttachToParentImmediately);
        MovieViewHolder viewHolder = new MovieViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(MovieViewHolder holder, int position) {
        Log.d(TAG, "#" + position);
        if(position == (mNumberItems - 5)) {
            Log.i(TAG, "the end is near");
            mOnClickListener.onLoadMore();
        }
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mNumberItems;
    }

    class MovieViewHolder extends RecyclerView.ViewHolder  implements View.OnClickListener {

        TextView tvListItemNumber;
        ImageView ivThumbnail;

        public MovieViewHolder(View itemView) {
            super(itemView);

            tvListItemNumber = (TextView) itemView.findViewById(R.id.tv_list_item_number);
            ivThumbnail = (ImageView) itemView.findViewById(R.id.iv_thumbnail);

            itemView.setOnClickListener(this);

            adjustThumbnailHeight();
        }

        void bind(int listIndex) {
//            Log.i("MovieViewHolder", "bind called: n" + listIndex);
            if (mCursor == null) {
                tvListItemNumber.setText(String.valueOf(listIndex));
            } else {
                tvListItemNumber.setVisibility(View.GONE);

//                Movie mov = movies.get(listIndex);
//                tvListItemNumber.setText(mov.getTitle());

                mCursor.moveToPosition(listIndex);

                int columnIndexPosterPath = mCursor.getColumnIndex(MovieEntry.COLUMN_POSTER_PATH);

                String posterPath = mCursor.getString(columnIndexPosterPath);

                Picasso.with(context).load(
                        "https://image.tmdb.org/t/p/w500" +
                        posterPath)
                        .into(ivThumbnail);

                //Log.i(TAG, mov.getPosterPath());

                int imageHeight = ivThumbnail.getHeight();
                int imageWidth = ivThumbnail.getWidth();
//                Log.i("image", imageWidth + "x" + imageHeight);
                //LayoutParams
                //ivThumbnail.setLayoutParams();
            }
        }

        private void adjustThumbnailHeight() {
            // DONE adjust item height height = width * 1.5

            FrameLayout layout = (FrameLayout) itemView.findViewById(R.id.layout_item);
            ViewGroup.LayoutParams params = layout.getLayoutParams();
            int height = params.height;
//            Log.i(TAG, "layout height: " + height);
//            Log.i(TAG, "thumbnail height: " + ivThumbnail.getHeight());
//            Log.i(TAG, "thumbnail width : " + ivThumbnail.getWidth());

            DisplayMetrics displaymetrics = new DisplayMetrics();
            ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            height = displaymetrics.heightPixels;
            int width = displaymetrics.widthPixels;
//            Log.i(TAG,"display: "+height+"x"+width);

            double newWidthD = (width / 2) * 1.5;
            int newWidthInt = (int) newWidthD;

            params.height = newWidthInt;

            layout.setLayoutParams(params);
        }

        @Override
        public void onClick(View view) {
            int clickedPosition = getAdapterPosition();
            mOnClickListener.onListItemClick(mCursor, clickedPosition);
        }


    }

    public void setMovies(Cursor cursor) {

        if (cursor != null) {
            mCursor = cursor;
            mNumberItems = cursor.getCount();

            notifyDataSetChanged();
        }


    }
}
