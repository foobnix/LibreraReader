package at.stefl.commons.util.string;

public class CharSequenceArrayWrapper extends AbstractCharSequence {
    
    private final char[] array;
    private final int off;
    private final int len;
    private final int end;
    
    public CharSequenceArrayWrapper(char[] array) {
        this(array, 0, array.length);
    }
    
    public CharSequenceArrayWrapper(char[] array, int off, int len) {
        if (off < 0) throw new IndexOutOfBoundsException("off < 0");
        if (len < 0) throw new IndexOutOfBoundsException("len < 0");
        this.end = off + len;
        if (end > array.length) throw new IndexOutOfBoundsException("end > len");
        
        this.array = array;
        this.off = off;
        this.len = len;
    }
    
    @Override
    public String toString() {
        return new String(array, off, len);
    }
    
    @Override
    public int length() {
        return len;
    }
    
    @Override
    public char charAt(int index) {
        if (index < 0) throw new IndexOutOfBoundsException("index < 0");
        index += off;
        if (index > end) throw new IndexOutOfBoundsException("index > end");
        return array[index];
    }
    
    @Override
    public CharSequence subSequence(int start, int end) {
        return new CharSequenceArrayWrapper(array, off + start, end - start);
    }
    
}