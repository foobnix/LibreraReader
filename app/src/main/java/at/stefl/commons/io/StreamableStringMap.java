package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import at.stefl.commons.util.collection.AbstractEntryWrapper;
import at.stefl.commons.util.collection.OrderedPair;
import at.stefl.commons.util.iterator.DelegationIterator;
import at.stefl.commons.util.string.AbstractCharSequence;
import at.stefl.commons.util.string.CharSequenceArrayWrapper;
import at.stefl.commons.util.string.CharSequenceWraper;

// TODO: improve: make use of object tools
// TODO: improve: re-implement for mapt, set, ... matching ability
// TODO: override HashMap?
public class StreamableStringMap<V> extends AbstractMap<String, V> {
    
    private static class EntryWrapper<V>
            extends
            AbstractEntryWrapper<AbstractCharSequence, OrderedPair<String, V>, String, V> {
        
        public EntryWrapper(
                Entry<AbstractCharSequence, OrderedPair<String, V>> entry) {
            super(entry);
        }
        
        @Override
        public String getKey() {
            return entry.getKey().toString();
        }
        
        @Override
        public V getValue() {
            return entry.getValue().getElement2();
        }
        
        @Override
        public V setValue(V value) {
            V result = entry.getValue().getElement2();
            entry.setValue(entry.getValue().setElement2(value));
            return result;
        }
    }
    
    private class EntrySetIterator
            extends
            DelegationIterator<Entry<AbstractCharSequence, OrderedPair<String, V>>, Entry<String, V>> {
        
        public EntrySetIterator() {
            super(map.entrySet().iterator());
        }
        
        @Override
        public Entry<String, V> next() {
            return new EntryWrapper<V>(iterator.next());
        }
    }
    
    private class EntrySet extends AbstractSet<Entry<String, V>> {
        
        @Override
        public Iterator<Entry<String, V>> iterator() {
            return new EntrySetIterator();
        }
        
        @Override
        public boolean add(Entry<String, V> e) {
            StreamableStringMap.this.put(e.getKey(), e.getValue());
            return true;
        }
        
        @Override
        public boolean remove(Object o) {
            boolean result = StreamableStringMap.this.containsKey(o);
            if (result) StreamableStringMap.this.remove(o);
            return result;
        }
        
        @Override
        public boolean contains(Object o) {
            return StreamableStringMap.this.containsKey(o);
        }
        
        @Override
        public int size() {
            return map.size();
        }
    }
    
    private final HashMap<AbstractCharSequence, OrderedPair<String, V>> map;
    
    private EntrySet entrySet;
    
    private int bufferSize;
    private char[] buffer;
    
    public StreamableStringMap() {
        map = new HashMap<AbstractCharSequence, OrderedPair<String, V>>();
    }
    
    public StreamableStringMap(int capacity) {
        map = new HashMap<AbstractCharSequence, OrderedPair<String, V>>(
                capacity);
    }
    
    public StreamableStringMap(Map<? extends String, ? extends V> map) {
        this(map.size());
        
        putAll(map);
    }
    
    @Override
    public V put(String key, V value) {
        if (key == null) throw new NullPointerException();
        
        int len = key.length();
        if (len > bufferSize) {
            bufferSize = len;
            if (buffer != null) buffer = new char[bufferSize];
        }
        
        OrderedPair<String, V> result = map.put(new CharSequenceWraper(key),
                new OrderedPair<String, V>(key, value));
        return (result == null) ? null : result.getElement2();
    }
    
    @Override
    public V remove(Object key) {
        if (!(key instanceof String)) throw new ClassCastException();
        return remove((String) key);
    }
    
    public V remove(String key) {
        OrderedPair<String, V> result = map.remove(new CharSequenceWraper(key));
        return (result == null) ? null : result.getElement2();
    }
    
    @Override
    public boolean containsKey(Object key) {
        if (!(key instanceof String)) throw new ClassCastException();
        return containsKey((String) key);
    }
    
    public boolean containsKey(String key) {
        return map.containsKey(new CharSequenceWraper(key));
    }
    
    @Override
    public V get(Object key) {
        if (!(key instanceof String)) throw new ClassCastException();
        return get((String) key);
    }
    
    public V get(String key) {
        OrderedPair<String, V> result = get((CharSequence) key);
        return (result == null) ? null : result.getElement2();
    }
    
    private OrderedPair<String, V> get(CharSequence charSequence) {
        return map.get(new CharSequenceWraper(charSequence));
    }
    
    public OrderedPair<String, V> match(Reader in) throws IOException {
        if (buffer == null) buffer = new char[bufferSize];
        
        int read = CharStreamUtil.readTireless(in, buffer);
        
        Object matcher = new CharSequenceArrayWrapper(buffer, 0, read);
        return map.get(matcher);
    }
    
    @Override
    public Set<Entry<String, V>> entrySet() {
        if (entrySet == null) entrySet = new EntrySet();
        return entrySet;
    }
    
    public void clearBuffer() {
        bufferSize = 0;
        buffer = null;
    }
    
    @Override
    public void clear() {
        map.clear();
        entrySet = null;
        
        clearBuffer();
    }
    
}