package com.sourceservermanager;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Matthew on 1/27/2016.
 */
public class MyRecyclerViewAdapter extends RecyclerView
        .Adapter<MyRecyclerViewAdapter
        .DataObjectHolder>  {
    private static String LOG_TAG = "MyRecyclerViewAdapter";
    private ArrayList<ServerDataObject> mDataset;
    private static MyClickListener myClickListener;
    private static MyClickListener myLongClickListener;

    public static class DataObjectHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener {

        private ServerDataObject sdoItem;
        private TextView nickname;
        private TextView host;
        private TextView port;

        public DataObjectHolder(View itemView) {
            super(itemView);

            nickname = (TextView) itemView.findViewById(R.id.nicknameTV);
            host = (TextView) itemView.findViewById(R.id.hostTV);
            port = (TextView) itemView.findViewById(R.id.portTV);

            Log.i(LOG_TAG, "Adding Listener");
            itemView.setOnClickListener(this);

            /*itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    // item clicked
                    Log.i(LOG_TAG, "CLICKED ITEM");
                }
            });*/
        }

        public void setItem(ServerDataObject item) {
            sdoItem = item;
        }

        @Override
        public void onClick(View v) {
            Log.i(LOG_TAG, "CLICKED ITEM");
            myClickListener.onItemClick(getAdapterPosition(), v, sdoItem);
        }

        /*@Override
        public boolean onLongClick(View v) {
            Log.i(LOG_TAG, "LONG CLICKED ITEM");
            myLongClickListener.onItemClick(getAdapterPosition(), v, sdoItem);

            return true;
        }*/
    }

    public void setOnItemClickListener(MyClickListener myClickListener) {
        this.myClickListener = myClickListener;
    }

    /*public void setOnItemLongClickListener(MyClickListener myClickListener) {
        this.myLongClickListener = myClickListener;
    }*/

    public MyRecyclerViewAdapter(ArrayList<ServerDataObject> myDataset) {
        mDataset = myDataset;
    }

    @Override
    public DataObjectHolder onCreateViewHolder(ViewGroup parent,
                                               int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_view_row, parent, false);

        DataObjectHolder dataObjectHolder = new DataObjectHolder(view);
        return dataObjectHolder;
    }

    @Override
    public void onBindViewHolder(DataObjectHolder holder, int position) {
        holder.nickname.setText(mDataset.get(position).getNickname());
        holder.host.setText(mDataset.get(position).getHost());
        holder.port.setText(mDataset.get(position).getPort());
        holder.setItem(mDataset.get(position));

        holder.itemView.setLongClickable(true);
    }

    public void addItem(ServerDataObject dataObj, int index) {
        mDataset.add(index, dataObj);
        notifyItemInserted(index);
    }

    public void deleteItem(int index) {
        mDataset.remove(index);
        notifyItemRemoved(index);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public long getItemId (int position) {
        return mDataset.get(position).getID();
    }



    public interface MyClickListener {
        public void onItemClick(int position, View v, ServerDataObject sdo);
    }
}
