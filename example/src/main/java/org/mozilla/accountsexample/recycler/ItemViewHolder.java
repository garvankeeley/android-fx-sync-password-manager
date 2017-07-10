package org.mozilla.accountsexample.recycler;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;


public class ItemViewHolder extends RecyclerView.ViewHolder {

    public TextView title;
    public TextView subtitle;

    public ItemViewHolder(View itemView) {
        super(itemView);
        this.title = (TextView) itemView.findViewById(android.R.id.text1);
        this.subtitle = (TextView) itemView.findViewById(android.R.id.text2);
    }


}