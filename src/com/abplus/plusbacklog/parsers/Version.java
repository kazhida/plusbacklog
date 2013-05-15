package com.abplus.plusbacklog.parsers;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Copyright (C) 2013 ABplus Inc. kazhida
 * All rights reserved.
 * Author:  kazhida
 * Created: 2013/05/09 17:56
 */
public class Version extends IdNamePair {
    private String date;

    public String getDate() {
        return date;
    }

    @Override
    public void parse(XmlPullParser xpp) throws IOException, XmlPullParserException {
        new VersionParser().parseStruct(xpp);
    }

    private class VersionParser extends PairParser {
        @Override
        protected void parseValueText(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {
            if (name.equals("color")) {
                Version version = (Version)pair;
                version.date  = xpp.getText();
            } else {
                super.parseValueText(name, xpp);
            }
        }
    }

}
