package com.abplus.plusbacklog.parsers;

import com.abplus.plusbacklog.BackLogCache;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C) 2013 ABplus Inc. kazhida
 * All rights reserved.
 * Author:  kazhida
 * Created: 2013/05/09 9:36
 */
public class Users implements BackLogCache.RootParseable {
    List<User> users = new ArrayList<User>();

    @Override
    public void parse(String response) throws IOException, XmlPullParserException {
        new UsersParser().parse(response);
    }

    private class UsersParser extends StructParser {

        @Override
        public void parseStruct(XmlPullParser xpp) throws IOException, XmlPullParserException {
            User user = new User();
            user.parse(xpp);
            users.add(user);
        }

        @Override
        protected void parseValueStartTag(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            //  ここにはこない。
        }

        @Override
        protected void parseValueEndTag(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            //  ここにはこない。
        }

        @Override
        protected void parseValueText(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            //  ここにはこない。
        }
    }
}
