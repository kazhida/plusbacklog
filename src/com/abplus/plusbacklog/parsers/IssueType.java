package com.abplus.plusbacklog.parsers;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Copyright (C) 2013 ABplus Inc. kazhida
 * All rights reserved.
 * Author:  kazhida
 * Created: 2013/05/09 17:48
 */
public class IssueType extends IdNamePair {
    private String color;

    public String getColor() {
        return color;
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
