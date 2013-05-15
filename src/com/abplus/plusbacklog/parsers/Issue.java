package com.abplus.plusbacklog.parsers;

import com.abplus.plusbacklog.BackLogCache;
import com.abplus.plusbacklog.BacklogIO;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Copyright (C) 2013 ABplus Inc. kazhida
 * All rights reserved.
 * Author:  kazhida
 * Created: 2013/05/09 17:44
 */
public class Issue implements BacklogIO.IdHolder, BacklogIO.KeyHolder, BackLogCache.RootParseable {

    private int id;
    private String key;
    private String summary;
    private String description;
    private String url;
    private String due_date;
    private String start_date;
    private double estimated_hours;
    private double actual_hours;
    private IssueType issueType = new IssueType();
    private IdNamePair priority = new IdNamePair();
    private IdNamePair resolution = new IdNamePair();
    private IdNamePair status = new IdNamePair();
    private IdNamePair components = new IdNamePair();
    private Version versions = new Version();
    private Version milestones = new Version();
    private IdNamePair created_user = new IdNamePair();
    private IdNamePair assigner = new IdNamePair();
    private String created_on;
    private String updated_on;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getKey() {
        return key;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }


    public String getDueDate() {
        return due_date;
    }

    public String getStartDate() {
        return start_date;
    }

    public double getEstimatedHours() {
        return estimated_hours;
    }

    public double getActualHours() {
        return actual_hours;
    }

    public IssueType getIssueType() {
        return issueType;
    }

    public IdNamePair getPriority() {
        return priority;
    }

    public IdNamePair getResolution() {
        return resolution;
    }

    public IdNamePair getStatus() {
        return status;
    }

    public IdNamePair getComponents() {
        return components;
    }

    public Version getVersions() {
        return versions;
    }

    public Version getMilestones() {
        return milestones;
    }

    public IdNamePair getCreatedUser() {
        return created_user;
    }

    public IdNamePair getAssigner() {
        return assigner;
    }

    public String getCreatedOn() {
        return created_on;
    }

    public String getUpdatedOn() {
        return updated_on;
    }

    @Override
    public void parse(String response) throws IOException, XmlPullParserException {
        new IssueParser().parse(response);
    }

    private class IssueParser extends StructParser {
        Issue issue = Issue.this;

        @Override
        protected void parseValueStartTag(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            if (xpp.getName().equals("struct")) {
                if (name.equals("issueType")) {
                    issue.issueType.parse(xpp);
                } else if (name.equals("priority")) {
                    issue.priority.parse(xpp);
                } else if (name.equals("resolution")) {
                    issue.resolution.parse(xpp);
                } else if (name.equals("status")) {
                    issue.status.parse(xpp);
                } else if (name.equals("component")) {
                    issue.components.parse(xpp);
                } else if (name.equals("versions")) {
                    issue.versions.parse(xpp);
                } else if (name.equals("milestones")) {
                    issue.milestones.parse(xpp);
                } else if (name.equals("created_user")) {
                    issue.created_user.parse(xpp);
                } else if (name.equals("assigner")) {
                    issue.assigner.parse(xpp);
                }
            }
        }

        @Override
        protected void parseValueText(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            if (name.equals("id")) {
                issue.id = Integer.parseInt(xpp.getText());
            } else if (name.equals("key")) {
                issue.key = xpp.getText();
            } else if (name.equals("summary")) {
                issue.summary = xpp.getText();
            } else if (name.equals("description")) {
                issue.description = xpp.getText();
            } else if (name.equals("url")) {
                issue.url = xpp.getText();
            } else if (name.equals("due_date")) {
                issue.due_date = xpp.getText();
            } else if (name.equals("start_date")) {
                issue.start_date = xpp.getText();
            } else if (name.equals("estimated_hours")) {
                issue.estimated_hours = Double.parseDouble(xpp.getText());
            } else if (name.equals("actual_hours")) {
                issue.actual_hours = Double.parseDouble(xpp.getText());
            } else if (name.equals("created_on")) {
                issue.created_on = xpp.getText();
            } else if (name.equals("updated_on")) {
                issue.updated_on = xpp.getText();
            }
        }

        @Override
        protected void parseValueEndTag(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            //  なにもしない
        }
    }
}
