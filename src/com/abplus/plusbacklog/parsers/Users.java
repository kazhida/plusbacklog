package com.abplus.plusbacklog.parsers;

import com.abplus.plusbacklog.BackLogCache;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C) 2014 ABplus Inc. kazhida
 * All rights reserved.
 * Created by kazhida on 2014/09/02.
 */
public class Users implements BackLogCache.RootParseable {

    private List<User> users = new ArrayList<User>();

    public class User extends IdNamePair {
        //  実質的にはエイリアス
    }

    public int count() {
        return users.size();
    }

    public User get(int index) {
        return users.get(index);
    }

    @Override
    public void parse(String response) throws IOException, XmlPullParserException {
        users.clear();
        new UsersParser().parse(response);
    }

    private class UsersParser extends StructParser {

        @Override
        public void parseStruct(XmlPullParser xpp) throws IOException, XmlPullParserException {
            User component = new User();
            component.parse(xpp);
            users.add(component);
        }

        @Override
        protected void parseValueStartTag(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            //  ここにはこない。
        }

        @Override
        protected void parseValueText(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            //  ここにはこない。
        }

        @Override
        protected void parseValueEndTag(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            //  ここにはこない。
        }
    }
}
