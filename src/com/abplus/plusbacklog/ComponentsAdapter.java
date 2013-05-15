package com.abplus.plusbacklog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.abplus.plusbacklog.parsers.Components;

/**
 * Copyright (C) 2013 ABplus Inc. kazhida
 * All rights reserved.
 * Author:  kazhida
 * Created: 2013/05/15 11:11
 */
public class ComponentsAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater inflater;
    private Components components;

    ComponentsAdapter(Context context, LayoutInflater inflater, Components components) {
        super();
        this.context = context;
        this.inflater = inflater;
        this.components = components;
    }

    @Override
    public int getCount() {
        return components.count() + 1;
    }

    @Override
    public Object getItem(int position) {
        if (position == 0) {
            return null;
        } else {
            return components.get(position - 1);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView result = (TextView)convertView;

        if (result == null) {
            result = (TextView)inflater.inflate(R.layout.spinner_item, null);
        }
        if (position == 0) {
            result.setText(context.getText(R.string.none));
            result.setTag(null);
        } else {
            Components.Component component = components.get(position - 1);
            result.setText(component.getName());
            result.setTag(component);
        }

        return result;
    }
}
