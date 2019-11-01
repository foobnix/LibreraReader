package org.librera;

/*
Copyright (c) 2002 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import java.util.Enumeration;
import java.util.Properties;

/**
 * Converts a Property file data into LinkedJSONObject and back.
 * @author JSON.org
 * @version 2015-05-05
 */
public class Property {
    /**
     * Converts a property file object into a LinkedJSONObject. The property file object is a table of name value pairs.
     * @param properties java.util.Properties
     * @return LinkedJSONObject
     * @throws org.librera.JSONException
     */
    public static LinkedJSONObject toJSONObject(Properties properties) throws org.librera.JSONException {
        // can't use the new constructor for Android support
        // LinkedJSONObject jo = new LinkedJSONObject(properties == null ? 0 : properties.size());
        LinkedJSONObject jo = new LinkedJSONObject();
        if (properties != null && !properties.isEmpty()) {
            Enumeration<?> enumProperties = properties.propertyNames();
            while(enumProperties.hasMoreElements()) {
                String name = (String)enumProperties.nextElement();
                jo.put(name, properties.getProperty(name));
            }
        }
        return jo;
    }

    /**
     * Converts the LinkedJSONObject into a property file object.
     * @param jo LinkedJSONObject
     * @return java.util.Properties
     * @throws org.librera.JSONException
     */
    public static Properties toProperties(LinkedJSONObject jo)  throws JSONException {
        Properties  properties = new Properties();
        if (jo != null) {
        	// Don't use the new entrySet API to maintain Android support
            for (final String key : jo.keySet()) {
                Object value = jo.opt(key);
                if (!LinkedJSONObject.NULL.equals(value)) {
                    properties.put(key, value.toString());
                }
            }
        }
        return properties;
    }
}
