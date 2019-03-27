package com.foobnix.ext;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class XmlParser {

	public static XmlPullParser buildPullParser() throws XmlPullParserException {
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setValidating(false);
        factory.setFeature(Xml.FEATURE_RELAXED, true);
        // factory.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
        // factory.setFeature(XmlPullParser.FEATURE_VALIDATION, false);
        XmlPullParser newPullParser = factory.newPullParser();
        return newPullParser;
	}

}
