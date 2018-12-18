package at.stefl.commons.util.collection;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

// TODO: improve exceptions
// TODO: implement Deque
// TODO: rename to CharArrayDeque
public class CharArrayQueue extends AbstractQueue<Character> implements
        RandomAccess, CharSequence, Serializable {
    
    private static final long serialVersionUID = -795960019807037815L;
    
    private char[] buffer;
    private int head;
    private int size;
    
    public CharArrayQueue(int capacity) {
        buffer = new char[capacity];
    }
    
    @Override
    public String toString() {
        char[] cbuf = new char[size];
        
        int tmp = Math.min(size, buffer.length - head);
        System.arraycopy(buffer, head, cbuf, 0, tmp);
        if (tmp < size) System.arraycopy(buffer, 0, cbuf, tmp, size - tmp);
        
        return new String(cbuf);
    }
    
    private int index(int i) {
        return (head + i) % buffer.length;
    }
    
    public char get(int i) {
        if (i >= size) throw new ArrayIndexOutOfBoundsException(i);
        
        return buffer[index(i)];
    }
    
    public void put(char c) {
        buffer[index(size)] = c;
        if (size < buffer.length) size++;
        else head = index(1);
    }
    
    @Override
    public boolean offer(Character e) {
        return offer(e);
    }
    
    public boolean offer(char c) {
        if (buffer.length <= size) return false;
        
        buffer[index(size)] = c;
        size++;
        
        return true;
    }
    
    @Override
    public boolean add(Character e) {
        return add(e.charValue());
    }
    
    public boolean add(char c) {
        if (!offer(c)) throw new IllegalStateException("queue is out of space");
        return true;
    }
    
    public boolean addAll(char[] cbuf) {
        return addAll(cbuf, 0, cbuf.length);
    }
    
    public boolean addAll(char[] cbuf, int off, int len) {
        if (off < 0) throw new IndexOutOfBoundsException("offset is negative");
        if (len < 0) throw new IndexOutOfBoundsException("length is negative");
        if ((off + len) > cbuf.length) throw new IndexOutOfBoundsException();
        if (len > (buffer.length - size)) throw new IndexOutOfBoundsException(
                "given buffer is to big");
        if (len == 0) return false;
        
        int tmp = Math.min(len, buffer.length - index(size));
        System.arraycopy(cbuf, off, buffer, index(size), tmp);
        if (len > (buffer.length - index(size))) System.arraycopy(cbuf, off
                + tmp, buffer, 0, len - tmp);
        size += len;
        
        return true;
    }
    
    public boolean addAll(CharSequence charSequence) {
        if (charSequence.length() > (buffer.length - size)) throw new IllegalStateException(
                "given char sequence is to big");
        if (charSequence.length() == 0) return false;
        
        for (int i = 0; i < charSequence.length(); i++) {
            add(charSequence.charAt(i));
        }
        
        return true;
    }
    
    @Override
    public Character poll() {
        Character result = peek();
        if (result == null) return null;
        
        head = index(1);
        size--;
        
        return result;
    }
    
    @Override
    public Character remove() {
        return removeChar();
    }
    
    public char removeChar() {
        if (size <= 0) throw new NoSuchElementException();
        
        char result = buffer[head];
        head = index(1);
        size--;
        
        return result;
    }
    
    @Override
    public Character peek() {
        if (size <= 0) return null;
        
        return buffer[head];
    }
    
    @Override
    public Character element() {
        return elementChar();
    }
    
    public char elementChar() {
        if (size <= 0) throw new NoSuchElementException();
        
        return buffer[head];
    }
    
    @Override
    public Iterator<Character> iterator() {
        return new Iterator<Character>() {
            
            private int i;
            
            @Override
            public boolean hasNext() {
                return i < size;
            }
            
            @Override
            public Character next() {
                return buffer[index(i++)];
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove not supported");
            }
        };
    }
    
    @Override
    public int size() {
        return size;
    }
    
    public int capacity() {
        return buffer.length;
    }
    
    @Override
    public void clear() {
        head = 0;
        size = 0;
    }
    
    @Override
    public int length() {
        return size;
    }
    
    @Override
    public char charAt(int index) {
        if (index >= size) throw new ArrayIndexOutOfBoundsException(index);
        
        return buffer[index(index)];
    }
    
    @Override
    public CharSequence subSequence(int start, int end) {
        if (start > end) throw new IndexOutOfBoundsException(
                "start is higher than end");
        if (start >= size) throw new IndexOutOfBoundsException();
        if (end > size) throw new IndexOutOfBoundsException();
        if (start == end) return new CharArrayQueue(0);
        
        int size = end - start;
        CharArrayQueue result = new CharArrayQueue(size);
        
        int tmp = Math.min(size, buffer.length - head);
        System.arraycopy(buffer, index(start), result.buffer, 0, tmp);
        if (tmp < size) System.arraycopy(buffer, 0, result.buffer, tmp, size
                - tmp);
        result.size = size;
        
        return result;
    }
    
}