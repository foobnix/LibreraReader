package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import at.stefl.commons.util.array.ArrayUtil;
import at.stefl.commons.util.collection.SingleLinkedNode;

// TODO: implement better solution (-> growable array)
public class DividedByteArrayOutputStream extends OutputStream {
    
    private static final int DEFAULT_INITIAL_SIZE = 16;
    
    private class ConcatInputStream extends InputStream {
        private SingleLinkedNode<byte[]> currentNode;
        private byte[] currentBuffer;
        private int currentIndex;
        
        private int position;
        
        private final int revision;
        
        private ConcatInputStream() {
            this.currentNode = DividedByteArrayOutputStream.this.headNode;
            this.currentBuffer = currentNode.getEntry();
            this.revision = DividedByteArrayOutputStream.this.revision;
        }
        
        private void checkRevision() {
            if (revision != DividedByteArrayOutputStream.this.revision) throw new IllegalStateException(
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
        public int available() throws IOException {
            checkRevision();
            return DividedByteArrayOutputStream.this.size - position;
        }
        
        @Override
        public int read() throws IOException {
            checkRevision();
            if (!ensureBuffer()) return -1;
            position++;
            return currentBuffer[currentIndex++];
        }
        
        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            checkRevision();
            int read = 0;
            
            while (len > 0) {
                if (!ensureBuffer()) break;
                int part = Math.min(currentBuffer.length - currentIndex, len);
                System.arraycopy(currentBuffer, currentIndex, b, off, part);
                
                off += part;
                len -= part;
                currentIndex += part;
                read += part;
                position += part;
            }
            
            return (read == 0) ? -1 : read;
        }
    }
    
    private SingleLinkedNode<byte[]> headNode;
    private SingleLinkedNode<byte[]> currentNode;
    private byte[] currentBuffer;
    private int currentIndex;
    
    private int size;
    
    private int revision;
    
    public DividedByteArrayOutputStream() {
        this(DEFAULT_INITIAL_SIZE);
    }
    
    public DividedByteArrayOutputStream(int initialSize) {
        currentNode = headNode = new SingleLinkedNode<byte[]>();
        currentNode.setEntry(currentBuffer = new byte[initialSize]);
    }
    
    @Override
    public String toString() {
        return new String(toByteArray());
    }
    
    public String toString(Charset charset) {
        return new String(toByteArray(), charset);
    }
    
    public String toString(String charset) throws UnsupportedEncodingException {
        return new String(toByteArray(), charset);
    }
    
    public int size() {
        return size;
    }
    
    public byte[] toByteArray() {
        if (size == 0) return ArrayUtil.EMPTY_BYTE_ARRAY;
        
        byte[] result = new byte[size];
        int index = 0;
        
        for (byte[] buffer : headNode) {
            int len = Math.min(buffer.length, size - index);
            if (len <= 0) break;
            System.arraycopy(buffer, 0, result, index, len);
            index += buffer.length;
        }
        
        return result;
    }
    
    public InputStream getInputStream() {
        return new ConcatInputStream();
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
            currentNode = currentNode.append(new SingleLinkedNode<byte[]>());
            currentNode.setEntry(currentBuffer = new byte[newSize]);
        }
        
        currentIndex = 0;
    }
    
    @Override
    public void write(int b) {
        ensureSpace(1);
        currentBuffer[currentIndex] = (byte) b;
        currentIndex++;
        size++;
    }
    
    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }
    
    @Override
    public void write(byte[] b, int off, int len) {
        if (b == null) throw new NullPointerException();
        if ((off < 0) || (len < 0) || (len > (b.length - off))) throw new IndexOutOfBoundsException();
        
        while (len > 0) {
            ensureSpace(len);
            int part = Math.min(currentBuffer.length - currentIndex, len);
            System.arraycopy(b, off, currentBuffer, currentIndex, part);
            
            off += part;
            len -= part;
            currentIndex += part;
            size += part;
        }
    }
    
    // TODO: improve buffer scaling
    public int write(InputStream in) throws IOException {
        int lastCount = size;
        
        while (true) {
            int read = in.read(currentBuffer, currentIndex,
                    currentBuffer.length - currentIndex);
            if (read == -1) break;
            
            currentIndex += read;
            size += read;
            
            ensureSpace(currentBuffer.length);
        }
        
        return size - lastCount;
    }
    
    // TODO: improve buffer scaling
    public int write(InputStream in, int len) throws IOException {
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
    
    public void writeTo(OutputStream out) throws IOException {
        for (byte[] buffer : headNode) {
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