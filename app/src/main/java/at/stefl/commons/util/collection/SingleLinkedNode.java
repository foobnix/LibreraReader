package at.stefl.commons.util.collection;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SingleLinkedNode<E> implements Iterable<E> {
    
    public static <E> boolean hasCycle(SingleLinkedNode<E> start) {
        Set<SingleLinkedNode<E>> collected = new HashSet<SingleLinkedNode<E>>();
        SingleLinkedNode<E> currentNode = start;
        
        while (currentNode.hasNext()) {
            currentNode = currentNode.next;
            if (collected.contains(currentNode)) return true;
            collected.add(currentNode);
        }
        
        return false;
    }
    
    private class EntryIterator implements Iterator<E> {
        
        private SingleLinkedNode<E> currentNode = SingleLinkedNode.this;
        
        @Override
        public boolean hasNext() {
            return currentNode != null;
        }
        
        @Override
        public E next() {
            if (currentNode == null) return null;
            
            E entry = currentNode.entry;
            currentNode = currentNode.next;
            
            return entry;
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    private E entry;
    private SingleLinkedNode<E> next;
    
    public SingleLinkedNode() {}
    
    public SingleLinkedNode(E entry) {
        this.entry = entry;
    }
    
    public SingleLinkedNode(E entry, SingleLinkedNode<E> next) {
        this.entry = entry;
        this.next = next;
    }
    
    public boolean hasNext() {
        return next != null;
    }
    
    public E getEntry() {
        return entry;
    }
    
    public SingleLinkedNode<E> getNext() {
        return next;
    }
    
    public void setEntry(E entry) {
        this.entry = entry;
    }
    
    public void setNext(SingleLinkedNode<E> next) {
        this.next = next;
    }
    
    public SingleLinkedNode<E> append(SingleLinkedNode<E> next) {
        setNext(next);
        return next;
    }
    
    @Override
    public Iterator<E> iterator() {
        return new EntryIterator();
    }
    
}