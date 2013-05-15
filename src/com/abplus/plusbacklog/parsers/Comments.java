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
 * Created: 2013/05/09 14:59
 */
public class Comments implements BackLogCache.RootParseable {

    private List<Comment> comments = new ArrayList<Comment>();

    public class Comment implements BacklogIO.IdHolder {
        private int id;
        private String content;
        private IdNamePair created_user = new IdNamePair();
        private String created_on;
        private String updated_on;

        @Override
        public int getId() {
            return id;
        }

        public String getContent() {
            return content;
        }

        public IdNamePair getCreatedUser() {
            return created_user;
        }

        public String getCreatedOn() {
            return created_on;
        }

        public String getUpdatedOn() {
            return updated_on;
        }
    }

    public int count() {
        return comments.size();
    }

    public Comment get(int index) {
        //  逆順
        int position = count() - index - 1;
        return comments.get(position);
    }

    @Override
    public void parse(String response) throws IOException, XmlPullParserException {
        new CommentsParser().parse(response);
    }

    private class CommentsParser extends StructParser {
        Comment comment = null;

        @Override
        public void parseStruct(XmlPullParser xpp) throws IOException, XmlPullParserException {
            comment = new Comment();
            super.parseStruct(xpp);
            comments.add(comment);
            comment = null;
        }

        @Override
        protected void parseValueStartTag(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            if (comment != null && xpp.getName().equals("struct")) {
                if (name.equals("created_user")) {
                    comment.created_user.parse(xpp);
                }
            }
        }

        @Override
        protected void parseValueText(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            if (comment != null) {
                if (name.equals("id")) {
                    comment.id = Integer.parseInt(xpp.getText());
                } else if (name.equals("content")) {
                    comment.content = xpp.getText();
                } else if (name.equals("created_on")) {
                    comment.created_on = xpp.getText();
                } else if (name.equals("updated_on")) {
                    comment.updated_on = xpp.getText();
                }
            }
        }

        @Override
        protected void parseValueEndTag(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            //なにもしない
        }
    }
}
