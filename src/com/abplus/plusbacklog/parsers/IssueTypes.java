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
 * Created: 2013/05/15 10:53
 */
public class IssueTypes implements BackLogCache.RootParseable {

    List<IssueType> issueTypes = new ArrayList<IssueType>();

    public class IssueType extends IdNamePair {
        private String color;

        public String getColor() {
            return color;
        }

        public int getColorAsInt() {
            String c = color.substring(1, color.length());
            return Integer.parseInt(c, 16) + 0xFF000000;
        }

        @Override
        public void parse(XmlPullParser xpp) throws IOException, XmlPullParserException {
            new IssueTypeParser().parseStruct(xpp);
        }

        private class IssueTypeParser extends PairParser {
            @Override
            protected void parseValueText(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
                if (name.equals("color")) {
                    IssueType issueType = (IssueType)pair;
                    issueType.color  = xpp.getText();
                } else {
                    super.parseValueText(name, xpp);
                }
            }
        }
    }

    public int count() {
        return issueTypes.size();
    }

    public IssueType get(int index) {
        return issueTypes.get(index);
    }

    @Override
    public void parse(String response) throws IOException, XmlPullParserException {
        new IssueTypesParser().parse(response);
    }

    private class IssueTypesParser extends StructParser {

        @Override
        public void parseStruct(XmlPullParser xpp) throws IOException, XmlPullParserException {
            IssueType issueType = new IssueType();
            issueType.parse(xpp);
            issueTypes.add(issueType);
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
