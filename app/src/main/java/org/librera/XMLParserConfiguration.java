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

/**
 * Configuration object for the XML parser.
 * @author AylwardJ
 *
 */
public class XMLParserConfiguration {
    /** Original Configuration of the XML Parser. */
    public static final XMLParserConfiguration ORIGINAL = new XMLParserConfiguration();
    /** Original configuration of the XML Parser except that values are kept as strings. */
    public static final XMLParserConfiguration KEEP_STRINGS = new XMLParserConfiguration(true);
    /**
     * When parsing the XML into JSON, specifies if values should be kept as strings (true), or if
     * they should try to be guessed into JSON values (numeric, boolean, string)
     */
    public final boolean keepStrings;
    /**
     * The name of the key in a JSON Object that indicates a CDATA section. Historically this has
     * been the value "content" but can be changed. Use <code>null</code> to indicate no CDATA
     * processing.
     */
    public final String cDataTagName;
    /**
     * When parsing the XML into JSON, specifies if values with attribute xsi:nil="true"
     * should be kept as attribute(false), or they should be converted to null(true)
     */
    public final boolean convertNilAttributeToNull;

    /**
     * Default parser configuration. Does not keep strings, and the CDATA Tag Name is "content".
     */
    public XMLParserConfiguration () {
        this(false, "content", false);
    }

    /**
     * Configure the parser string processing and use the default CDATA Tag Name as "content".
     * @param keepStrings <code>true</code> to parse all values as string.
     *      <code>false</code> to try and convert XML string values into a JSON value.
     */
    public XMLParserConfiguration (final boolean keepStrings) {
        this(keepStrings, "content", false);
    }

    /**
     * Configure the parser string processing to try and convert XML values to JSON values and
     * use the passed CDATA Tag Name the processing value. Pass <code>null</code> to
     * disable CDATA processing
     * @param cDataTagName<code>null</code> to disable CDATA processing. Any other value
     *      to use that value as the LinkedJSONObject key name to process as CDATA.
     */
    public XMLParserConfiguration (final String cDataTagName) {
        this(false, cDataTagName, false);
    }

    /**
     * Configure the parser to use custom settings.
     * @param keepStrings <code>true</code> to parse all values as string.
     *      <code>false</code> to try and convert XML string values into a JSON value.
     * @param cDataTagName<code>null</code> to disable CDATA processing. Any other value
     *      to use that value as the LinkedJSONObject key name to process as CDATA.
     */
    public XMLParserConfiguration (final boolean keepStrings, final String cDataTagName) {
        this.keepStrings = keepStrings;
        this.cDataTagName = cDataTagName;
        this.convertNilAttributeToNull = false;
    }

    /**
     * Configure the parser to use custom settings.
     * @param keepStrings <code>true</code> to parse all values as string.
     *      <code>false</code> to try and convert XML string values into a JSON value.
     * @param cDataTagName <code>null</code> to disable CDATA processing. Any other value
     *      to use that value as the LinkedJSONObject key name to process as CDATA.
     * @param convertNilAttributeToNull <code>true</code> to parse values with attribute xsi:nil="true" as null.
     *                                  <code>false</code> to parse values with attribute xsi:nil="true" as {"xsi:nil":true}.
     */
    public XMLParserConfiguration (final boolean keepStrings, final String cDataTagName, final boolean convertNilAttributeToNull) {
        this.keepStrings = keepStrings;
        this.cDataTagName = cDataTagName;
        this.convertNilAttributeToNull = convertNilAttributeToNull;
    }
}
