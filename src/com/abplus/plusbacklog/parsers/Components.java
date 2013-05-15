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
 * Created: 2013/05/15 10:27
 */
public class Components implements BackLogCache.RootParseable {

    private List<Component> components = new ArrayList<Component>();

    public class Component extends IdNamePair {
        //  実質的にはエイリアス
    }

    public int count() {
        return components.size();
    }

    public Component get(int index) {
        return components.get(index);
    }

    @Override
    public void parse(String response) throws IOException, XmlPullParserException {
        components.clear();
        new ComponentsParser().parse(response);
    }

    private class ComponentsParser extends StructParser {

        @Override
        public void parseStruct(XmlPullParser xpp) throws IOException, XmlPullParserException {
            Component component = new Component();
            component.parse(xpp);
            components.add(component);
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
