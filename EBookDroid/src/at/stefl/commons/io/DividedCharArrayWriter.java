package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import at.stefl.commons.util.array.ArrayUtil;
import at.stefl.commons.util.collection.SingleLinkedNode;

// TODO: implement better solution (-> growable array)
public class DividedCharArrayWriter extends Writer {
    
    private static final int DEFAULT_INITIAL_SIZE = 16;
    
    private class ConcatReader extends Reader {
        private SingleLinkedNode<char[]> currentNode;
        private char[] currentBuffer;
        private int currentIndex;
        
        private int position;
        
        private final int revision;
        
        private ConcatReader() {
            this.currentNode = DividedCharArrayWriter.this.headNode;
            this.currentBuffer = currentNode.getEntry();
            this.revision = DividedCharArrayWriter.this.revision;
        }
        
        private void checkRevision() {
            if (revision != DividedCharArrayWriter.this.revision) throw new IllegalStateException(
                    "stream was reset");
        }
        
        private boolean ensureBuffer() {
            if (currentIndex >= currentBuffer.length) {
                if (!currentNode.hasNext()) return false;
                currentNode = currentNode.getNext();
                currentBuffer = currentNode.getEntry();
                currentIndex = 0;
            }
            
            return true;
        }
        
        @Override
        public boolean ready() {
            checkRevision();
            return position < DividedCharArrayWriter.this.size;
        }
        
        @Override
        public int read() throws IOException {
            checkRevision();
            if (!ensureBuffer()) return -1;
            return currentBuffer[currentIndex++];
        }
        
        @Override
        public int read(char[] cbuf) throws IOException {
            return read(cbuf, 0, cbuf.length);
        }
        
        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            checkRevision();
            
            int read = 0;
            
            while (len > 0) {
                if (!ensureBuffer()) break;
                int part = Math.min(currentBuffer.length - currentIndex, len);
                System.arraycopy(currentBuffer, currentIndex, cbuf, off, part);
                
                off += part;
                len -= part;
                currentIndex += part;
                read += part;
            }
            
            return (read == 0) ? -1 : read;
        }
        
        @Override
        public void close() {}
    }
    
    private SingleLinkedNode<char[]> headNode;
    private SingleLinkedNode<char[]> currentNode;
    private char[] currentBuffer;
    private int currentIndex;
    
    private int size;
    
    private int revision;
    
    public DividedCharArrayWriter() {
        this(DEFAULT_INITIAL_SIZE);
    }
    
    public DividedCharArrayWriter(int initialSize) {
        currentNode = headNode = new SingleLinkedNode<char[]>();
        currentNode.setEntry(currentBuffer = new char[initialSize]);
    }
    
    @Override
    public String toString() {
        return new String(toCharArray());
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public int size() {
        return size;
    }
    
    public char[] toCharArray() {
        if (size == 0) return ArrayUtil.EMPTY_CHAR_ARRAY;
        
        char[] result = new char[size];
        int index = 0;
        
        for (char[] buffer : headNode) {
            int len = Math.min(buffer.length, size - index);
            if (len <= 0) break;
            System.arraycopy(buffer, 0, result, index, len);
            index += buffer.length;
        }
        
        return result;
    }
    
    public Reader getReader() {
        return new ConcatReader();
    }
    
    private void ensureSpace(int space) {
        if (currentIndex >= currentBuffer.length) getMoreSpace(space);
    }
    
    // TODO: improve buffer scaling
    private void getMoreSpace(int space) {
        if (currentNode.hasNext()) {
            currentNode = currentNode.getNext();
            currentBuffer = currentNode.getEntry();
        } else {
            int newSize = Math.max(currentBuffer.length << 1, space);
            currentNode = currentNode.append(new SingleLinkedNode<char[]>());
            currentNode.setEntry(currentBuffer = new char[newSize]);
        }
        
        currentIndex = 0;
    }
    
    @Override
    public void write(int c) {
        ensureSpace(1);
        currentBuffer[currentIndex] = (char) c;
        currentIndex++;
        size++;
    }
    
    @Override
    public void write(char[] cbuf) {
        write(cbuf, 0, cbuf.length);
    }
    
    @Override
    public void write(char[] cbuf, int off, int len) {
        if (cbuf == null) throw new NullPointerException();
        if ((off < 0) || (len < 0) || (len > (cbuf.length - off))) throw new IndexOutOfBoundsException();
        
        size += len;
        
        while (len > 0) {
            ensureSpace(len);
            int part = Math.min(currentBuffer.length - currentIndex, len);
            System.arraycopy(cbuf, off, currentBuffer, currentIndex, part);
            
            off += part;
            len -= part;
            currentIndex += part;
        }
    }
    
    @Override
    public void write(String str) {
        write(str, 0, str.length());
    }
    
    @Override
    public void write(String str, int off, int len) {
        if (str == null) throw new NullPointerException();
        if ((off < 0) || (len < 0) || (len > (str.length() - off))) throw new IndexOutOfBoundsException();
        
        while (len > 0) {
            ensureSpace(len);
            int part = Math.min(currentBuffer.length - currentIndex, len);
            str.getChars(off, off + part, currentBuffer, currentIndex);
            
            off += part;
            len -= part;
            currentIndex += part;
            size += part;
        }
    }
    
    public int write(Reader in) throws IOException {
        int lastCount = size;
        
        while (true) {
            int read = in.read(currentBuffer, currentIndex,
                    currentBuffer.length - currentIndex);
            if (read == -1) break;
            
            currentIndex += read;
            size += read;
            
            if (currentIndex >= currentBuffer.length) getMoreSpace(currentBuffer.length);
        }
        
        return size - lastCount;
    }
    
    public int write(Reader in, int len) throws IOException {
        int lastCount = size;
        
        while (true) {
            int part = Math.min(currentBuffer.length - currentIndex, len);
            int read = in.read(currentBuffer, currentIndex, part);
            if (read == -1) break;
            
            len -= read;
            currentIndex += read;
            size += read;
            
            if (len > 0) break;
            
            ensureSpace(currentBuffer.length);
        }
        
        return size - lastCount;
    }
    
    @Override
    public Writer append(char c) {
        write(c);
        return this;
    }
    
    @Override
    public Writer append(CharSequence csq) {
        return append(csq, 0, csq.length());
    }
    
    @Override
    public Writer append(CharSequence csq, int start, int end) {
        if ((start < 0) || (end < 0) || (start > end)) throw new IndexOutOfBoundsException();
        if (csq == null) csq = "null";
        
        for (int i = start; i <= end; i++) {
            ensureSpace(end - i + 1);
            currentBuffer[currentIndex] = csq.charAt(i);
            currentIndex++;
            size++;
        }
        
        return this;
    }
    
    public void writeTo(Writer out) throws IOException {
        for (char[] buffer : headNode) {
            if (buffer == currentBuffer) {
                out.write(currentBuffer, 0, currentIndex);
                break;
            } else {
                out.write(buffer);
            }
        }
    }
    
    public void reset() {
        currentNode = headNode;
        currentBuffer = currentNode.getEntry();
        currentIndex = 0;
        size = 0;
        
        revision++;
    }
    
    @Override
    public void flush() {}
    
    @Override
    public void close() {}
    
}