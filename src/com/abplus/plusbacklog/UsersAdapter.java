package com.abplus.plusbacklog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.abplus.plusbacklog.parsers.Users;

/**
 * Copyright (C) 2014 ABplus Inc. kazhida
 * All rights reserved.
 * Created by kazhida on 2014/09/02.
 */
public class UsersAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater inflater;
    private Users users;

    UsersAdapter(Context context, LayoutInflater inflater, Users users) {
        super();
        this.context = context;
        this.inflater = inflater;
        this.users = users;
    }

    @Override
    public int getCount() {
        return users.count() + 1;
    }

    @Override
    public Object getItem(int position) {
        if (position == 0) {
            return null;
        } else {
            return users.get(position - 1);
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
            result = (TextView)inflater.inflate(R.layout.spinner_item, parent, false);
        }
        if (position == 0) {
            result.setText(context.getText(R.string.no_user));
            result.setTag(null);
        } else {
            Users.User user = users.get(position - 1);
            result.setText(user.getName());
            result.setTag(user);
        }

        return result;
    }
}
