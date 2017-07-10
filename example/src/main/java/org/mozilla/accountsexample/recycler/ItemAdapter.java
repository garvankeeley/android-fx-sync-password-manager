package org.mozilla.accountsexample.recycler;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.accountsexample.ClipboardDialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mozilla.accountsexample.AppGlobals.REC_HOSTNAME;
import static org.mozilla.accountsexample.AppGlobals.REC_PASSWORD;
import static org.mozilla.accountsexample.AppGlobals.REC_USERNAME;


/**
 * A simple RecyclerView.Adapter class that manages items.
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    private List<ItemAdapter.Item> mItems;

    public static final String keyTitle = REC_HOSTNAME;
    public static final String keySubtitle = REC_USERNAME;

    public static class Item {
        Map<String, String> data = new HashMap<>();
    }

    public ItemAdapter(List<ItemAdapter.Item> items) {
        mItems = items;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        final Item item = mItems.get(position);
        final String title = item.data.get(keyTitle);
        holder.title.setText(title);
        holder.subtitle.setText(item.data.get(keySubtitle));

        final View root = holder.title.getRootView();
        root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardDialog.show(item.data.get(REC_HOSTNAME),
                        item.data.get(REC_USERNAME),
                        item.data.get(REC_PASSWORD),
                        root.getContext());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }


}/**/