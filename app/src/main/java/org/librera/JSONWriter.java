package org.librera;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/*
Copyright (c) 2006 JSON.org

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
 * JSONWriter provides a quick and convenient way of producing JSON text.
 * The texts produced strictly conform to JSON syntax rules. No whitespace is
 * added, so the results are ready for transmission or storage. Each instance of
 * JSONWriter can produce one JSON text.
 * <p>
 * A JSONWriter instance provides a <code>value</code> method for appending
 * values to the
 * text, and a <code>key</code>
 * method for adding keys before values in objects. There are <code>array</code>
 * and <code>endArray</code> methods that make and bound array values, and
 * <code>object</code> and <code>endObject</code> methods which make and bound
 * object values. All of these methods return the JSONWriter instance,
 * permitting a cascade style. For example, <pre>
 * new JSONWriter(myWriter)
 *     .object()
 *         .key("JSON")
 *         .value("Hello, World!")
 *     .endObject();</pre> which writes <pre>
 * {"JSON":"Hello, World!"}</pre>
 * <p>
 * The first method called must be <code>array</code> or <code>object</code>.
 * There are no methods for adding commas or colons. JSONWriter adds them for
 * you. Objects and arrays can be nested up to 200 levels deep.
 * <p>
 * This can sometimes be easier than using a LinkedJSONObject to build a string.
 * @author JSON.org
 * @version 2016-08-08
 */
public class JSONWriter {
    private static final int maxdepth = 200;

    /**
     * The comma flag determines if a comma should be output before the next
     * value.
     */
    private boolean comma;

    /**
     * The current mode. Values:
     * 'a' (array),
     * 'd' (done),
     * 'i' (initial),
     * 'k' (key),
     * 'o' (object).
     */
    protected char mode;

    /**
     * The object/array stack.
     */
    private final LinkedJSONObject stack[];

    /**
     * The stack top index. A value of 0 indicates that the stack is empty.
     */
    private int top;

    /**
     * The writer that will receive the output.
     */
    protected Appendable writer;

    /**
     * Make a fresh JSONWriter. It can be used to build one JSON text.
     */
    public JSONWriter(Appendable w) {
        this.comma = false;
        this.mode = 'i';
        this.stack = new LinkedJSONObject[maxdepth];
        this.top = 0;
        this.writer = w;
    }

    /**
     * Append a value.
     * @param string A string value.
     * @return this
     * @throws org.librera.JSONException If the value is out of sequence.
     */
    private JSONWriter append(String string) throws org.librera.JSONException {
        if (string == null) {
            throw new org.librera.JSONException("Null pointer");
        }
        if (this.mode == 'o' || this.mode == 'a') {
            try {
                if (this.comma && this.mode == 'a') {
                    this.writer.append(',');
                }
                this.writer.append(string);
            } catch (IOException e) {
            	// Android as of API 25 does not support this exception constructor
            	// however we won't worry about it. If an exception is happening here
            	// it will just throw a "Method not found" exception instead.
                throw new org.librera.JSONException(e);
            }
            if (this.mode == 'o') {
                this.mode = 'k';
            }
            this.comma = true;
            return this;
        }
        throw new org.librera.JSONException("Value out of sequence.");
    }

    /**
     * Begin appending a new array. All values until the balancing
     * <code>endArray</code> will be appended to this array. The
     * <code>endArray</code> method must be called to mark the array's end.
     * @return this
     * @throws org.librera.JSONException If the nesting is too deep, or if the object is
     * started in the wrong place (for example as a key or after the end of the
     * outermost array or object).
     */
    public JSONWriter array() throws org.librera.JSONException {
        if (this.mode == 'i' || this.mode == 'o' || this.mode == 'a') {
            this.push(null);
            this.append("[");
            this.comma = false;
            return this;
        }
        throw new org.librera.JSONException("Misplaced array.");
    }

    /**
     * End something.
     * @param m Mode
     * @param c Closing character
     * @return this
     * @throws org.librera.JSONException If unbalanced.
     */
    private JSONWriter end(char m, char c) throws org.librera.JSONException {
        if (this.mode != m) {
            throw new org.librera.JSONException(m == 'a'
                ? "Misplaced endArray."
                : "Misplaced endObject.");
        }
        this.pop(m);
        try {
            this.writer.append(c);
        } catch (IOException e) {
        	// Android as of API 25 does not support this exception constructor
        	// however we won't worry about it. If an exception is happening here
        	// it will just throw a "Method not found" exception instead.
            throw new org.librera.JSONException(e);
        }
        this.comma = true;
        return this;
    }

    /**
     * End an array. This method most be called to balance calls to
     * <code>array</code>.
     * @return this
     * @throws org.librera.JSONException If incorrectly nested.
     */
    public JSONWriter endArray() throws org.librera.JSONException {
        return this.end('a', ']');
    }

    /**
     * End an object. This method most be called to balance calls to
     * <code>object</code>.
     * @return this
     * @throws org.librera.JSONException If incorrectly nested.
     */
    public JSONWriter endObject() throws org.librera.JSONException {
        return this.end('k', '}');
    }

    /**
     * Append a key. The key will be associated with the next value. In an
     * object, every value must be preceded by a key.
     * @param string A key string.
     * @return this
     * @throws org.librera.JSONException If the key is out of place. For example, keys
     *  do not belong in arrays or if the key is null.
     */
    public JSONWriter key(String string) throws org.librera.JSONException {
        if (string == null) {
            throw new org.librera.JSONException("Null key.");
        }
        if (this.mode == 'k') {
            try {
                LinkedJSONObject topObject = this.stack[this.top - 1];
                // don't use the built in putOnce method to maintain Android support
				if(topObject.has(string)) {
					throw new org.librera.JSONException("Duplicate key \"" + string + "\"");
				}
                topObject.put(string, true);
                if (this.comma) {
                    this.writer.append(',');
                }
                this.writer.append(LinkedJSONObject.quote(string));
                this.writer.append(':');
                this.comma = false;
                this.mode = 'o';
                return this;
            } catch (IOException e) {
            	// Android as of API 25 does not support this exception constructor
            	// however we won't worry about it. If an exception is happening here
            	// it will just throw a "Method not found" exception instead.
                throw new org.librera.JSONException(e);
            }
        }
        throw new org.librera.JSONException("Misplaced key.");
    }


    /**
     * Begin appending a new object. All keys and values until the balancing
     * <code>endObject</code> will be appended to this object. The
     * <code>endObject</code> method must be called to mark the object's end.
     * @return this
     * @throws org.librera.JSONException If the nesting is too deep, or if the object is
     * started in the wrong place (for example as a key or after the end of the
     * outermost array or object).
     */
    public JSONWriter object() throws org.librera.JSONException {
        if (this.mode == 'i') {
            this.mode = 'o';
        }
        if (this.mode == 'o' || this.mode == 'a') {
            this.append("{");
            this.push(new LinkedJSONObject());
            this.comma = false;
            return this;
        }
        throw new org.librera.JSONException("Misplaced object.");

    }


    /**
     * Pop an array or object scope.
     * @param c The scope to close.
     * @throws org.librera.JSONException If nesting is wrong.
     */
    private void pop(char c) throws org.librera.JSONException {
        if (this.top <= 0) {
            throw new org.librera.JSONException("Nesting error.");
        }
        char m = this.stack[this.top - 1] == null ? 'a' : 'k';
        if (m != c) {
            throw new org.librera.JSONException("Nesting error.");
        }
        this.top -= 1;
        this.mode = this.top == 0
            ? 'd'
            : this.stack[this.top - 1] == null
            ? 'a'
            : 'k';
    }

    /**
     * Push an array or object scope.
     * @param jo The scope to open.
     * @throws org.librera.JSONException If nesting is too deep.
     */
    private void push(LinkedJSONObject jo) throws org.librera.JSONException {
        if (this.top >= maxdepth) {
            throw new org.librera.JSONException("Nesting too deep.");
        }
        this.stack[this.top] = jo;
        this.mode = jo == null ? 'a' : 'k';
        this.top += 1;
    }

    /**
     * Make a JSON text of an Object value. If the object has an
     * value.toJSONString() method, then that method will be used to produce the
     * JSON text. The method is required to produce a strictly conforming text.
     * If the object does not contain a toJSONString method (which is the most
     * common case), then a text will be produced by other means. If the value
     * is an array or Collection, then a JSONArray will be made from it and its
     * toJSONString method will be called. If the value is a MAP, then a
     * LinkedJSONObject will be made from it and its toJSONString method will be
     * called. Otherwise, the value's toString method will be called, and the
     * result will be quoted.
     *
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @param value
     *            The value to be serialized.
     * @return a printable, displayable, transmittable representation of the
     *         object, beginning with <code>{</code>&nbsp;<small>(left
     *         brace)</small> and ending with <code>}</code>&nbsp;<small>(right
     *         brace)</small>.
     * @throws org.librera.JSONException
     *             If the value is or contains an invalid number.
     */
    public static String valueToString(Object value) throws org.librera.JSONException {
        if (value == null || value.equals(null)) {
            return "null";
        }
        if (value instanceof JSONString) {
            String object;
            try {
                object = ((JSONString) value).toJSONString();
            } catch (Exception e) {
                throw new org.librera.JSONException(e);
            }
            if (object != null) {
                return object;
            }
            throw new org.librera.JSONException("Bad value from toJSONString: " + object);
        }
        if (value instanceof Number) {
            // not all Numbers may match actual JSON Numbers. i.e. Fractions or Complex
            final String numberAsString = LinkedJSONObject.numberToString((Number) value);
            if(LinkedJSONObject.NUMBER_PATTERN.matcher(numberAsString).matches()) {
                // Close enough to a JSON number that we will return it unquoted
                return numberAsString;
            }
            // The Number value is not a valid JSON number.
            // Instead we will quote it as a string
            return LinkedJSONObject.quote(numberAsString);
        }
        if (value instanceof Boolean || value instanceof LinkedJSONObject
                || value instanceof org.librera.JSONArray) {
            return value.toString();
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            return new LinkedJSONObject(map).toString();
        }
        if (value instanceof Collection) {
            Collection<?> coll = (Collection<?>) value;
            return new org.librera.JSONArray(coll).toString();
        }
        if (value.getClass().isArray()) {
            return new JSONArray(value).toString();
        }
        if(value instanceof Enum<?>){
            return LinkedJSONObject.quote(((Enum<?>)value).name());
        }
        return LinkedJSONObject.quote(value.toString());
    }

    /**
     * Append either the value <code>true</code> or the value
     * <code>false</code>.
     * @param b A boolean.
     * @return this
     * @throws org.librera.JSONException
     */
    public JSONWriter value(boolean b) throws org.librera.JSONException {
        return this.append(b ? "true" : "false");
    }

    /**
     * Append a double value.
     * @param d A double.
     * @return this
     * @throws org.librera.JSONException If the number is not finite.
     */
    public JSONWriter value(double d) throws org.librera.JSONException {
        return this.value(Double.valueOf(d));
    }

    /**
     * Append a long value.
     * @param l A long.
     * @return this
     * @throws org.librera.JSONException
     */
    public JSONWriter value(long l) throws org.librera.JSONException {
        return this.append(Long.toString(l));
    }


    /**
     * Append an object value.
     * @param object The object to append. It can be null, or a Boolean, Number,
     *   String, LinkedJSONObject, or JSONArray, or an object that implements JSONString.
     * @return this
     * @throws org.librera.JSONException If the value is out of sequence.
     */
    public JSONWriter value(Object object) throws JSONException {
        return this.append(valueToString(object));
    }
}
