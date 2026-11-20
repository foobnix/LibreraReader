package com.foobnix.ext.office;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A piece of an XML document as a small tree: names keep their prefix ("w:p"), as the parser is
 * not namespace aware. Read one block at a time out of a large document, so the whole of it is
 * never held at once.
 */
final class XmlNode {

    final String name;
    private String[] attributes;
    private List<XmlNode> children;
    private StringBuilder text;

    private XmlNode(String name) {
        this.name = name;
    }

    /** The element the parser stands on, read to its end. */
    static XmlNode read(XmlPullParser xpp) throws XmlPullParserException, IOException {
        final XmlNode node = new XmlNode(xpp.getName());
        final int count = xpp.getAttributeCount();
        if (count > 0) {
            node.attributes = new String[count * 2];
            for (int i = 0; i < count; i++) {
                node.attributes[i * 2] = xpp.getAttributeName(i);
                node.attributes[i * 2 + 1] = xpp.getAttributeValue(i);
            }
        }
        int event;
        while ((event = xpp.next()) != XmlPullParser.END_TAG) {
            if (event == XmlPullParser.START_TAG) {
                if (node.children == null) {
                    node.children = new ArrayList<XmlNode>(4);
                }
                node.children.add(read(xpp));
            } else if (event == XmlPullParser.TEXT || event == XmlPullParser.ENTITY_REF
                    || event == XmlPullParser.CDSECT) {
                final String value = xpp.getText();
                if (value != null) {
                    if (node.text == null) {
                        node.text = new StringBuilder();
                    }
                    node.text.append(value);
                }
            } else if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
        }
        return node;
    }

    String attr(String name) {
        if (attributes == null) {
            return null;
        }
        for (int i = 0; i < attributes.length; i += 2) {
            if (attributes[i].equals(name)) {
                return attributes[i + 1];
            }
        }
        return null;
    }

    String text() {
        return text == null ? "" : text.toString();
    }

    List<XmlNode> children() {
        return children == null ? Collections.<XmlNode>emptyList() : children;
    }

    XmlNode child(String name) {
        if (children != null) {
            for (XmlNode child : children) {
                if (child.name.equals(name)) {
                    return child;
                }
            }
        }
        return null;
    }

    /** The first element of this name anywhere below. */
    XmlNode find(String name) {
        if (children != null) {
            for (XmlNode child : children) {
                if (child.name.equals(name)) {
                    return child;
                }
                final XmlNode found = child.find(name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** The value of a child's attribute: {@code <w:jc w:val="center"/>} gives "center". */
    String childAttr(String child, String attribute) {
        final XmlNode node = child(child);
        return node == null ? null : node.attr(attribute);
    }
}
