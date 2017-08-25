package com.heetel.android.popularmovies;

import android.content.ContentValues;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.heetel.android.popularmovies.data.MovieContract;

import java.util.ArrayList;

/**
 * Created by Julian Heetel on 30.07.2017.
 *
 */

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {
    private static final String TAG = SearchAdapter.class.getSimpleName();

    private int mNumberItems;
    private Context mContext;
    private ArrayList<ContentValues> mData;

    final private ListItemCallbackListener mOnClickListener;

    public interface ListItemCallbackListener {
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

    class SearchViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView tvTitle, tvYear, tvOriginalTitle;

        SearchViewHolder(View itemView) {
            super(itemView);

            tvTitle = (TextView) itemView.findViewById(R.id.si_title);
            tvYear = (TextView) itemView.findViewById(R.id.si_year);
            tvOriginalTitle = (TextView) itemView.findViewById(R.id.si_originaltitle);

            itemView.setOnClickListener(this);
        }

        void bind(int index) {
            String title;
            if (mData == null) title = "no data found";
            else title = mData.get(index).getAsString(MovieContract.MovieEntry.COLUMN_TITLE);
            String year = mData.get(index).getAsString(MovieContract.MovieEntry.COLUMN_RELEASE_DATE);
            if (year.length() > 4) year = year.substring(0, 4);
            tvYear.setText(" · " + year);
            tvTitle.setText(title);

            String originalTitle = mData.get(index).getAsString(MovieContract.MovieEntry.COLUMN_ORIGINAL_TITLE);
//            tvOriginalTitle.setText(originalTitle);
        }

        @Override
        public void onClick(View v) {
            mOnClickListener.onListItemClick(getAdapterPosition());
        }
    }

    void setData(ArrayList<ContentValues> data) {
        mData = data;
        mNumberItems = data.size();
        Log.i(TAG, "setData: " + mNumberItems);
        notifyDataSetChanged();
    }

    ArrayList<ContentValues> getData() {
        return mData;
    }
}
