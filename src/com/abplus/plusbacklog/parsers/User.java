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
 * Created: 2013/05/09 8:07
 */
public class User implements BacklogIO.IdHolder, BacklogIO.NameHolder, BackLogCache.RootParseable, BackLogCache.Parseable {
    private int     id;
    private String  name;
    private String  lang;
    private String  updated_on;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getLang() {
        return lang;
    }

    public String getUpdatedOn() {
        return updated_on;
    }

    @Override
    public void parse(String response) throws IOException, XmlPullParserException {
        new UserParser().parse(response);
    }

    @Override
    public void parse(XmlPullParser xpp) throws IOException, XmlPullParserException {
        new UserParser().parseStruct(xpp);
    }

    private class UserParser extends StructParser {
        User user = User.this;

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
            if (user != null) {
                if (name.equals("id")) {
                    user.id = Integer.parseInt(xpp.getText());
                } else if (name.equals("name")) {
                    user.name = xpp.getText();
                } else if (name.equals("lang")) {
                    user.lang = xpp.getText();
                } else if (name.equals("updated_on")) {
                    user.updated_on = xpp.getText();
                }
            }
        }
    }
}
