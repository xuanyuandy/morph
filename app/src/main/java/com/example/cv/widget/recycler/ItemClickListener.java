package com.example.cv.widget.recycler;

import androidx.recyclerview.widget.RecyclerView;

public interface ItemClickListener {
    void onItemClick(RecyclerView.ViewHolder holder);

    void onLongClick(RecyclerView.ViewHolder holder);
}
