package com.abplus.plusbacklog;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.abplus.plusbacklog.parsers.IssueTypes;

/**
 * Copyright (C) 2013 ABplus Inc. kazhida
 * All rights reserved.
 * Author:  kazhida
 * Created: 2013/05/15 10:16
 */
public class IssueTypesAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private IssueTypes issueTypes;

    IssueTypesAdapter(LayoutInflater inflater, IssueTypes issueTypes) {
        super();
        this.inflater = inflater;
        this.issueTypes = issueTypes;
    }

    @Override
    public int getCount() {
        return issueTypes.count();
    }

    @Override
    public Object getItem(int position) {
        return issueTypes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView result = (TextView)convertView;

        if (result == null) {
            result = (TextView)inflater.inflate(R.layout.spinner_item, parent, false);
        }
        IssueTypes.IssueType issueType = issueTypes.get(position);
        result.setText(issueType.getName());
        result.setBackgroundColor(issueType.getColorAsInt());
        result.setTextColor(Color.WHITE);
        result.setTag(issueType);

        return result;
    }
}
