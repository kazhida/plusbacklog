package com.abplus.plusbacklog;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.abplus.plusbacklog.parsers.Projects;

/**
 * Copyright (C) 2013 ABplus Inc. kazhida
 * All rights reserved.
 * Author:  kazhida
 * Created: 2013/05/15 10:08
 */
public class ProjectsAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private Projects projects;

    ProjectsAdapter(LayoutInflater inflater, Projects projects) {
        super();

        this.inflater = inflater;
        this.projects = projects;
    }


    @Override
    public int getCount() {
        if (projects == null) {
            return 0;
        } else {
            return projects.count();
        }
    }

    @Override
    public Object getItem(int position) {
        if (projects == null) {
            return null;
        } else {
            return projects.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout result = (LinearLayout)convertView;

        if (result == null) {
            result = (LinearLayout)inflater.inflate(R.layout.project_item, parent, false);
        }
        Projects.Project project = projects.get(position);
        TextView keyView = (TextView)result.findViewById(R.id.project_key);
        TextView nameView = (TextView)result.findViewById(R.id.project_name);

        keyView.setText(project.getKey());
        nameView.setText(project.getName());

        if (project.isArchived()) {
            keyView.setTextColor(Color.LTGRAY);
        } else {
            keyView.setTextColor(Color.BLACK);
        }

        result.setTag(project);

        return result;
    }

    int keyIndexOf(String key) {
        if (key == null) {
            return -1;
        } else {
            for (int i = 0; i < projects.count(); i++) {
                if (key.equals(projects.get(i).getKey())) return i;
            }
            return -1;
        }
    }
}
