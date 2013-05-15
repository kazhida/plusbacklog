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
 * Created: 2013/05/10 9:54
 */
public class UserIcon implements BacklogIO.IdHolder, BackLogCache.RootParseable {
    private int id;
    private String content_type;
    private String data;    //BASE64
    private String updated_on;

    @Override
    public int getId() {
        return id;
    }

    public String getContentType() {
        return content_type;
    }

    public String getData() {
        return data;
    }

    public String getUpdatedOn() {
        return updated_on;
    }

    @Override
    public void parse(String response) throws IOException, XmlPullParserException {
        new UserIconParser().parse(response);
    }

    private class UserIconParser extends StructParser {
        UserIcon icon = UserIcon.this;

        @Override
        protected void parseValueStartTag(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            //  なにもしない
        }

        @Override
        protected void parseValueText(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            if (name.equals("id")) {
                icon.id = Integer.parseInt(xpp.getText());
            } else if (name.equals("content_type")) {
                icon.content_type = xpp.getText();
            } else if (name.equals("data")) {
                icon.data = xpp.getText();
            } else if (name.equals("updated_on")) {
                icon.updated_on = xpp.getText();
            }
        }

        @Override
        protected void parseValueEndTag(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            //  なにもしない
        }
    }
}
