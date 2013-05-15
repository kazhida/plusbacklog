package com.abplus.plusbacklog.parsers;

import com.abplus.plusbacklog.BackLogCache;
import com.abplus.plusbacklog.BacklogIO;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C) 2013 ABplus Inc. kazhida
 * All rights reserved.
 * Author:  kazhida
 * Created: 2013/05/09 8:07
 */
public class Projects implements BackLogCache.RootParseable {

    List<Project> projects = new ArrayList<Project>();

    public class Project implements BacklogIO.IdHolder, BacklogIO.NameHolder, BacklogIO.KeyHolder {
        private int id;
        private String name;
        private String key;
        private String url;
        private boolean archived;

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
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


    @Override
    public void parse(String response) throws IOException, XmlPullParserException {
        new ProjectsParser().parse(response);
    }

    private class ProjectsParser extends StructParser {
        Project project = null;

        @Override
        public void parseStruct(XmlPullParser xpp) throws IOException, XmlPullParserException {
            project = new Project();
            super.parseStruct(xpp);
            projects.add(project);
            project = null;
        }

        @Override
        protected void parseValueStartTag(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            //なにもしない
        }

        @Override
        protected void parseValueEndTag(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            //なにもしない
        }

        @Override
        protected void parseValueText(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            if (project != null) {
                if (name.equals("id")) {
                    project.id = Integer.parseInt(xpp.getText());
                } else if (name.equals("name")) {
                    project.name = xpp.getText();
                } else if (name.equals("key")) {
                    project.key = xpp.getText();
                } else if (name.equals("url")) {
                    project.url = xpp.getText();
                } else if (name.equals("archived")) {
                    project.archived = xpp.getText().equals("1");
                }
            }
        }
    }
}
