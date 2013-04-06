package com.abplus.plusbacklog;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C) 2013 ABplus Inc. kazhida
 * All rights reserved.
 * Author:  kazhida
 * Created: 2013/04/06 13:52
 */
public class SelectionCache {
    private String space_id;
    private String user_id;
    private String password;
    private Activity activity;
    private Handler handler = new Handler();

    public SelectionCache(Activity activity, String space_id, String user_id, String password) {
        this.activity = activity;
        this.space_id = space_id;
        this.user_id = user_id;
        this.password = password;
    }

    public String getSpaceId() {
        return space_id;
    }

    public String getUserId() {
        return user_id;
    }

    public String getPassword() {
        return password;
    }

    public void loadProjects(Runnable notify) {
        //todo: プロジェクトを読み込む

        //  通知する
        handler.post(notify);
    }

    public void loadIssueTypes(Project current, Runnable notify) {
        //todo: イシュータイプを読み込む

        //  通知する
        handler.post(notify);
    }

    public BaseAdapter getProjectsAdapter() {
        return new ProjectsAdapter();
    }

    public BaseAdapter getIssueTypeAdapter(Project current) {
        return new IssueTypeAdapter(current);
    }

    public class IssueType {
        private int id;
        private String name;
        private String color;

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }
    }

    public class Project {
        private List<IssueType> types = new ArrayList<IssueType>();
        private int id;
        private String name;
        private String key;
        private String url;
        private boolean archived;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getKey() {
            return key;
        }

        public String getUrl() {
            return url;
        }

        public boolean isArchived() {
            return archived;
        }
    }

    List<Project> projects = new ArrayList<Project>();

    class ProjectsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return projects.size();
        }

        @Override
        public Object getItem(int position) {
            return projects.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView result = (TextView)convertView;

            if (result == null) {
                result = (TextView)activity.getLayoutInflater().inflate(R.layout.spinner_item, null);
            }
            Project project = projects.get(position);
            result.setText(project.getKey());
            result.setTag(project);

            return result;
        }
    }

    class IssueTypeAdapter extends BaseAdapter {
        private Project project;

        IssueTypeAdapter(Project project) {
            super();

            this.project = project;
        }

        @Override
        public int getCount() {
            return project.types.size();
        }

        @Override
        public Object getItem(int position) {
            return project.types.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView result = (TextView)convertView;

            if (result == null) {
                result = (TextView)activity.getLayoutInflater().inflate(R.layout.spinner_item, null);
            }
            IssueType issueType = project.types.get(position);
            result.setText(issueType.getName());
            result.setTag(issueType);

            return result;
        }
    }
}
