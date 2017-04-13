/**
 * Copyright (C) 2013
 * Nicholas J. Little <arealityfarbetween@googlemail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mobi;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;


public class StreamUtil {

    public static interface InputAction {
        void act(InputStream stream) throws IOException;
    }

    public static interface OutputAction {
        void act(OutputStream stream) throws IOException;
    }

    private Queue<IOException> exceptions = new LinkedList<IOException>();
    private int transactions;

    /**
     * An atomic operation on an InputStream
     *
     * @param stream
     * @param user
     * @return
     */
    public synchronized boolean read(InputStream stream,
                                     InputAction user)
    {
        start();

        try {
            user.act(stream);
        } catch (IOException ex) {
            record(ex);
        } finally {
            close(stream);
        }

        return end();
    }

    /**
     * An atomic operation on an OutputStream
     *
     * @param stream
     * @param user
     * @return
     */
    public synchronized boolean write(OutputStream stream,
                                      OutputAction user)
    {
        start();

        try {
            user.act(stream);
        } catch (IOException ex) {
            record(ex);
        } finally {
            flush(stream);
            close(stream);
        }

        return end();
    }

    /**
     * Flushes a Flushable
     *
     * @param flushable
     * @return
     */
    public synchronized boolean flush(Flushable flushable) {
        start();

        try {
            flushable.flush();
        } catch (IOException ex) {
            record(ex);
        }

        return end();
    }

    /**
     * Closes a Closeable
     *
     * @param closeable
     * @return
     */
    public synchronized boolean close(Closeable closeable)
    {
        start();

        try {
            closeable.close();
        } catch (IOException ex) {
            record(ex);
        }

        return end();
    }

    /**
     * Signals the end of an atomic operation
     *
     * @return true if there are no Exceptions recorded
     */
    protected boolean end() {
        transactions = transactions > 0 ? transactions - 1 : 0;
        return exceptions.isEmpty();
    }

    /**
     * Signals the start of an atomic operation
     */
    protected void start() {
        if (transactions < 1)
            exceptions.clear();

        ++transactions;
    }

    /**
     * Records an IOException
     *
     * @param ex
     */
    protected void record(IOException ex) {
        ex.printStackTrace();
        exceptions.add(ex);
    }

    /**
     * Retrieves the first IOException
     *
     * @return
     */
    public IOException getFirstException() { return exceptions.poll(); }
}
