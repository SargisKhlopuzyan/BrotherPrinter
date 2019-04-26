package com.sargis.kh.printer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.brother.ptouch.sdk.LabelInfo;

import java.util.List;

public class CustomArrayAdapter extends ArrayAdapter<LabelInfo.QL700> {

    private final LayoutInflater mInflater;
    private final List<LabelInfo.QL700> items;

    public CustomArrayAdapter(@NonNull Context context, @NonNull List<LabelInfo.QL700> objects) {
        super(context, R.layout.layout_spinner_list, 0, objects);
        mInflater = LayoutInflater.from(context);
        items = objects;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    @Override
    public @NonNull View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    private View createItemView(int position, View convertView, ViewGroup parent){
        final View view = mInflater.inflate(R.layout.layout_spinner_list, parent, false);
        TextView textView = view.findViewById(R.id.text_view);
        textView.setText(items.get(position).toString() + " : " + items.get(position).ordinal());
        return view;
    }

}