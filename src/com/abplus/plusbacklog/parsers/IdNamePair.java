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
 * Created: 2013/05/09 8:41
 */
public class IdNamePair implements BacklogIO.IdHolder, BacklogIO.NameHolder, BackLogCache.Parseable {
    int id;
    String name;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void parse(XmlPullParser xpp) throws IOException, XmlPullParserException {
        new PairParser().parseStruct(xpp);
    }

    protected class PairParser extends StructParser {
        IdNamePair pair = IdNamePair.this;

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
            if (name.equals("id")) {
                pair.id = Integer.parseInt(xpp.getText());
            } else if (name.equals("name")) {
                pair.name = xpp.getText();
            }
        }
    }
}
