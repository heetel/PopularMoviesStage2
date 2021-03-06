package com.heetel.android.popularmovies.search;

import android.content.ContentValues;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.heetel.android.popularmovies.R;
import com.heetel.android.popularmovies.data.MovieContract;
//import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import java.util.ArrayList;

/**
 * Created by Julian Heetel on 30.07.2017.
 *
 */

class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {
    private static final String TAG = SearchAdapter.class.getSimpleName();

    private int mNumberItems;
    private Context mContext;
    private ArrayList<ContentValues> mData;

    final private ListItemCallbackListener mOnClickListener;

    interface ListItemCallbackListener {
        void onListItemClick(int position);
    }

    SearchAdapter(int mNumberItems, ListItemCallbackListener listItemCallbackListener, Context context) {
        super();
        this.mNumberItems = mNumberItems;
        this.mOnClickListener = listItemCallbackListener;
        this.mContext = context;
    }

    @Override
    public SearchViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutId = R.layout.search_item;
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(layoutId, parent, false);


        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SearchViewHolder holder, int position) {
//        Log.i(TAG, "bind: " + position);
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mNumberItems;
    }

    class SearchViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView tvTitle, tvYear;
        ImageView ivPoster;

        SearchViewHolder(View itemView) {
            super(itemView);

            tvTitle = (TextView) itemView.findViewById(R.id.si_title);
            tvYear = (TextView) itemView.findViewById(R.id.si_year);
//            tvOriginalTitle = (TextView) itemView.findViewById(R.id.si_originaltitle);
            ivPoster = (ImageView) itemView.findViewById(R.id.si_poster);

            itemView.setOnClickListener(this);
        }

        void bind(int index) {
            String title;
            if (mData == null) title = "no data found";
            else title = mData.get(index).getAsString(MovieContract.MovieEntry.COLUMN_TITLE);
            String year = null;
            if (mData != null) {
                year = mData.get(index).getAsString(MovieContract.MovieEntry.COLUMN_RELEASE_DATE);
            }
            if (year != null && year.length() > 4) year = year.substring(0, 4);
            tvYear.setText(year);
            tvTitle.setText(title);

            String url = "https://image.tmdb.org/t/p/w500" +
                    mData.get(index).getAsString(MovieContract.MovieEntry.COLUMN_POSTER_PATH);

            Glide.with(mContext)
                    .load(url)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.placeholder_poster)
                    )
                    .into(ivPoster);
        }

        @Override
        public void onClick(View v) {
            mOnClickListener.onListItemClick(getAdapterPosition());
        }
    }

    void setData(ArrayList<ContentValues> data) {
        mData = data;
        if (data != null) mNumberItems = data.size();
        else mNumberItems = 0;
        Log.i(TAG, "setData: " + mNumberItems);
        notifyDataSetChanged();
    }

    ArrayList<ContentValues> getData() {
        return mData;
    }
}
