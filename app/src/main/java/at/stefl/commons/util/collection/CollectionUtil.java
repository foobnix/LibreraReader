package at.stefl.commons.util.collection;

import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

import at.stefl.commons.util.iterator.IterableIterator;
import at.stefl.commons.util.iterator.IteratorEnumeration;
import at.stefl.commons.util.iterator.IteratorUtil;
import at.stefl.commons.util.object.ObjectTransformer;

// TODO: improve argument names
// TODO: call method by method, avoid redundant code?
public class CollectionUtil {
    
    public static <E> Iterable<E> getIterable(Iterator<E> iterator) {
        return new IterableIterator<E>(iterator);
    }
    
    public static boolean containsAll(Collection<?> c, Object... array) {
        for (int i = 0; i < array.length; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Boolean> c,
            boolean... array) {
        for (int i = 0; i < array.length; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Byte> c, byte... array) {
        for (int i = 0; i < array.length; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Character> c,
            char... array) {
        for (int i = 0; i < array.length; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Short> c,
            short... array) {
        for (int i = 0; i < array.length; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Integer> c,
            int... array) {
        for (int i = 0; i < array.length; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Long> c, long... array) {
        for (int i = 0; i < array.length; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Float> c,
            float... array) {
        for (int i = 0; i < array.length; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Double> c,
            double... array) {
        for (int i = 0; i < array.length; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<?> c, Object[] array, int off,
            int len) {
        int end = off + len;
        
        for (int i = off; i < end; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Boolean> c,
            boolean[] array, int off, int len) {
        int end = off + len;
        
        for (int i = off; i < end; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Byte> c, byte[] array,
            int off, int len) {
        int end = off + len;
        
        for (int i = off; i < end; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Character> c,
            char[] array, int off, int len) {
        int end = off + len;
        
        for (int i = off; i < end; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Short> c,
            short[] array, int off, int len) {
        int end = off + len;
        
        for (int i = off; i < end; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Integer> c,
            int[] array, int off, int len) {
        int end = off + len;
        
        for (int i = off; i < end; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Long> c, long[] array,
            int off, int len) {
        int end = off + len;
        
        for (int i = off; i < end; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Float> c,
            float[] array, int off, int len) {
        int end = off + len;
        
        for (int i = off; i < end; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static boolean containsAll(Collection<? super Double> c,
            double[] array, int off, int len) {
        int end = off + len;
        
        for (int i = off; i < end; i++) {
            if (!c.contains(array[i])) return false;
        }
        
        return true;
    }
    
    public static <E> boolean addAll(Collection<? super E> c, E... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Boolean> c,
            boolean... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Byte> c, byte... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Character> c, char... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Short> c, short... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Integer> c, int... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Long> c, long... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Float> c, float... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Double> c, double... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static <E> boolean addAll(Collection<? super E> c, E[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Boolean> c,
            boolean[] array, int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Byte> c, byte[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Character> c, char[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Short> c, short[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Integer> c, int[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Long> c, long[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Float> c, float[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Double> c, double[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<?> c, Object... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Byte> c,
            boolean... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Byte> c, byte... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Character> c,
            char... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Short> c, short... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Integer> c, int... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Long> c, long... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Float> c, float... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Double> c,
            double... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<?> c, Object[] array, int off,
            int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Byte> c,
            boolean[] array, int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Byte> c, byte[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Character> c,
            char[] array, int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Short> c, short[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Integer> c, int[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Long> c, long[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Float> c, float[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static boolean removeAll(Collection<? super Double> c,
            double[] array, int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.remove(array[i]);
        }
        
        return result;
    }
    
    public static <E extends Comparable<E>> E getGreatest(
            Collection<? extends E> c) {
        if (c.isEmpty()) throw new NoSuchElementException();
        
        if ((c instanceof List) && (c instanceof RandomAccess)) return getGreatest((List<? extends E>) c);
        
        Iterator<? extends E> iterator = c.iterator();
        E result = iterator.next();
        E element;
        
        while (iterator.hasNext()) {
            element = iterator.next();
            if (element.compareTo(result) > 0) result = element;
        }
        
        return result;
    }
    
    private static <E extends Comparable<E>> E getGreatest(
            List<? extends E> randomAccessList) {
        E result = randomAccessList.get(0);
        
        for (int i = 1; i < randomAccessList.size(); i++) {
            E element = randomAccessList.get(i);
            if (element.compareTo(result) > 0) result = element;
        }
        
        return result;
    }
    
    public static <E> E getGreatest(Comparator<? super E> comparator,
            Collection<? extends E> c) {
        if (c.isEmpty()) throw new NoSuchElementException();
        
        if ((c instanceof List) && (c instanceof RandomAccess)) return getGreatest(
                comparator, (List<? extends E>) c);
        
        Iterator<? extends E> iterator = c.iterator();
        E result = iterator.next();
        E element;
        
        while (iterator.hasNext()) {
            element = iterator.next();
            if (comparator.compare(element, result) > 0) result = element;
        }
        
        return result;
    }
    
    private static <E> E getGreatest(Comparator<? super E> comparator,
            List<? extends E> randomAccessList) {
        E result = randomAccessList.get(0);
        
        for (int i = 1; i < randomAccessList.size(); i++) {
            E element = randomAccessList.get(i);
            if (comparator.compare(element, result) > 0) result = element;
        }
        
        return result;
    }
    
    public static <E extends Comparable<E>> E getGreatestNotNull(
            Collection<? extends E> c) {
        if ((c instanceof List) && (c instanceof RandomAccess)) return getGreatestNotNull((List<? extends E>) c);
        
        Iterator<? extends E> iterator = c.iterator();
        E result = iterator.next();
        E element;
        
        while (iterator.hasNext()) {
            element = iterator.next();
            if (element == null) continue;
            if ((result == null) || (element.compareTo(result) > 0)) result = element;
        }
        
        if (result == null) throw new NoSuchElementException();
        
        return result;
    }
    
    private static <E extends Comparable<E>> E getGreatestNotNull(
            List<? extends E> randomAccessList) {
        E result = null;
        E element = randomAccessList.get(0);
        
        for (int i = 1; i < randomAccessList.size(); i++) {
            element = randomAccessList.get(i);
            if (element == null) continue;
            if ((result == null) || (element.compareTo(result) > 0)) result = element;
        }
        
        if (result == null) throw new NoSuchElementException();
        
        return result;
    }
    
    public static <E> E getGreatestNotNull(Comparator<? super E> comparator,
            Collection<? extends E> c) {
        if ((c instanceof List) && (c instanceof RandomAccess)) return getGreatestNotNull(
                comparator, (List<? extends E>) c);
        
        Iterator<? extends E> iterator = c.iterator();
        E result = iterator.next();
        E element;
        
        while (iterator.hasNext()) {
            element = iterator.next();
            if (element == null) continue;
            if ((result == null) || (comparator.compare(element, result) > 0)) result = element;
        }
        
        if (result == null) throw new NoSuchElementException();
        
        return result;
    }
    
    private static <E> E getGreatestNotNull(Comparator<? super E> comparator,
            List<? extends E> randomAccessList) {
        E result = randomAccessList.get(0);
        E element;
        
        for (int i = 1; i < randomAccessList.size(); i++) {
            element = randomAccessList.get(i);
            if (element == null) continue;
            if ((result == null) || (comparator.compare(element, result) > 0)) result = element;
        }
        
        if (result == null) throw new NoSuchElementException();
        
        return result;
    }
    
    public static <E extends Comparable<E>> E getSmallest(
            Collection<? extends E> c) {
        if (c.isEmpty()) throw new NoSuchElementException();
        
        if ((c instanceof List) && (c instanceof RandomAccess)) return getSmallest((List<? extends E>) c);
        
        Iterator<? extends E> iterator = c.iterator();
        E result = iterator.next();
        E element;
        
        while (iterator.hasNext()) {
            element = iterator.next();
            if (element.compareTo(result) < 0) result = element;
        }
        
        return result;
    }
    
    private static <E extends Comparable<E>> E getSmallest(
            List<? extends E> randomAccessList) {
        E result = randomAccessList.get(0);
        
        for (int i = 1; i < randomAccessList.size(); i++) {
            E element = randomAccessList.get(i);
            if (element.compareTo(result) < 0) result = element;
        }
        
        return result;
    }
    
    public static <E> E getSmallest(Comparator<? super E> comparator,
            Collection<? extends E> c) {
        if (c.isEmpty()) throw new NoSuchElementException();
        
        if ((c instanceof List) && (c instanceof RandomAccess)) return getSmallest(
                comparator, (List<? extends E>) c);
        
        Iterator<? extends E> iterator = c.iterator();
        E result = iterator.next();
        E element;
        
        while (iterator.hasNext()) {
            element = iterator.next();
            if (comparator.compare(element, result) < 0) result = element;
        }
        
        return result;
    }
    
    private static <E> E getSmallest(Comparator<? super E> comparator,
            List<? extends E> randomAccessList) {
        E result = randomAccessList.get(0);
        
        for (int i = 1; i < randomAccessList.size(); i++) {
            E element = randomAccessList.get(i);
            if (comparator.compare(element, result) < 0) result = element;
        }
        
        return result;
    }
    
    public static <E extends Comparable<E>> E getSmallestNotNull(
            Collection<? extends E> c) {
        if ((c instanceof List) && (c instanceof RandomAccess)) return getSmallestNotNull((List<? extends E>) c);
        
        Iterator<? extends E> iterator = c.iterator();
        E result = iterator.next();
        E element;
        
        while (iterator.hasNext()) {
            element = iterator.next();
            if (element == null) continue;
            if ((result == null) || (element.compareTo(result) < 0)) result = element;
        }
        
        if (result == null) throw new NoSuchElementException();
        
        return result;
    }
    
    private static <E extends Comparable<E>> E getSmallestNotNull(
            List<? extends E> randomAccessList) {
        E result = null;
        E element = randomAccessList.get(0);
        
        for (int i = 1; i < randomAccessList.size(); i++) {
            element = randomAccessList.get(i);
            if (element == null) continue;
            if ((result == null) || (element.compareTo(result) < 0)) result = element;
        }
        
        if (result == null) throw new NoSuchElementException();
        
        return result;
    }
    
    public static <E> E getSmallestNotNull(Comparator<? super E> comparator,
            Collection<? extends E> c) {
        if ((c instanceof List) && (c instanceof RandomAccess)) return getSmallestNotNull(
                comparator, (List<? extends E>) c);
        
        Iterator<? extends E> iterator = c.iterator();
        E result = iterator.next();
        E element;
        
        while (iterator.hasNext()) {
            element = iterator.next();
            if (element == null) continue;
            if ((result == null) || (comparator.compare(element, result) < 0)) result = element;
        }
        
        if (result == null) throw new NoSuchElementException();
        
        return result;
    }
    
    private static <E> E getSmallestNotNull(Comparator<? super E> comparator,
            List<? extends E> randomAccessList) {
        E result = randomAccessList.get(0);
        E element;
        
        for (int i = 1; i < randomAccessList.size(); i++) {
            element = randomAccessList.get(i);
            if (element == null) continue;
            if ((result == null) || (comparator.compare(element, result) < 0)) result = element;
        }
        
        if (result == null) throw new NoSuchElementException();
        
        return result;
    }
    
    public static <K, V> void putAll(Map<? super K, ? super V> map,
            ObjectTransformer<? super V, ? extends K> keyGenerator,
            Collection<? extends V> values) {
        if ((values instanceof List) && (values instanceof RandomAccess)) {
            putAll(map, keyGenerator, (List<? extends V>) values);
            return;
        }
        
        for (V value : values) {
            K key = keyGenerator.transform(value);
            map.put(key, value);
        }
    }
    
    private static <K, V> void putAll(Map<? super K, ? super V> map,
            ObjectTransformer<? super V, ? extends K> keyGenerator,
            List<? extends V> randomAccessList) {
        for (int i = 0; i < randomAccessList.size(); i++) {
            V value = randomAccessList.get(i);
            K key = keyGenerator.transform(value);
            map.put(key, value);
        }
    }
    
    public static <K, V> void putAllNotNull(Map<? super K, ? super V> map,
            ObjectTransformer<? super V, ? extends K> keyGenerator,
            Collection<? extends V> values) {
        if ((values instanceof List) && (values instanceof RandomAccess)) {
            putAllNotNull(map, keyGenerator, (List<? extends V>) values);
            return;
        }
        
        for (V value : values) {
            K key = keyGenerator.transform(value);
            if (key == null) continue;
            map.put(key, value);
        }
    }
    
    private static <K, V> void putAllNotNull(Map<? super K, ? super V> map,
            ObjectTransformer<? super V, ? extends K> keyGenerator,
            List<? extends V> randomAccessList) {
        for (int i = 0; i < randomAccessList.size(); i++) {
            V value = randomAccessList.get(i);
            K key = keyGenerator.transform(value);
            if (key == null) continue;
            map.put(key, value);
        }
    }
    
    // TODO: implement key/value generator
    // TODO: implement multiple put (put to multiple maps)
    public static <K, V> void putAll(Map<? super K, ? super V> map,
            ObjectTransformer<? super V, ? extends K> keyGenerator, V... values) {
        V value;
        K key;
        
        for (int i = 0; i < values.length; i++) {
            value = values[i];
            key = keyGenerator.transform(value);
            map.put(key, value);
        }
    }
    
    public static <K, V> void putAllNotNull(Map<? super K, ? super V> map,
            ObjectTransformer<? super V, ? extends K> keyGenerator, V... values) {
        V value;
        K key;
        
        for (int i = 0; i < values.length; i++) {
            value = values[i];
            key = keyGenerator.transform(value);
            if (key == null) continue;
            map.put(key, value);
        }
    }
    
    public static <K, V> HashMap<K, V> toHashMap(
            ObjectTransformer<? super V, ? extends K> keyGenerator, V... values) {
        HashMap<K, V> result = new HashMap<K, V>();
        putAll(result, keyGenerator, values);
        return result;
    }
    
    public static <K, V> HashMap<K, V> toHashMapNotNull(
            ObjectTransformer<? super V, ? extends K> keyGenerator, V... values) {
        HashMap<K, V> result = new HashMap<K, V>();
        putAllNotNull(result, keyGenerator, values);
        return result;
    }
    
    public static <K, V> HashMap<K, V> toHashMap(
            ObjectTransformer<? super V, ? extends K> keyGenerator,
            Collection<? extends V> values) {
        HashMap<K, V> result = new HashMap<K, V>();
        putAll(result, keyGenerator, values);
        return result;
    }
    
    public static <K, V> HashMap<K, V> toHashMapNotNull(
            ObjectTransformer<? super V, ? extends K> keyGenerator,
            Collection<? extends V> values) {
        HashMap<K, V> result = new HashMap<K, V>();
        putAllNotNull(result, keyGenerator, values);
        return result;
    }
    
    public static <E> Enumeration<E> enumeration(Collection<? extends E> c) {
        return new IteratorEnumeration<E>(c.iterator());
    }
    
    public static <E> void swap(List<E> list, int i, int j) {
        if (list instanceof RandomAccess) {
            list.set(i, list.set(j, list.get(i)));
        } else {
            ListIterator<E> iterator = list.listIterator(i);
            iterator.set(list.set(j, iterator.next()));
        }
    }
    
    public static <E> void swapAll(List<E> list) {
        if (list instanceof RandomAccess) {
            for (int i = 0, j = list.size() - 1; i < j; i++, j--) {
                list.set(i, list.set(j, list.get(i)));
            }
        } else {
            ListIterator<E> iteratorI = list.listIterator();
            ListIterator<E> iteratorJ = list.listIterator(list.size());
            E tmp;
            
            while (iteratorI.nextIndex() < iteratorJ.nextIndex()) {
                tmp = iteratorI.next();
                iteratorI.set(iteratorJ.previous());
                iteratorJ.set(tmp);
            }
        }
    }
    
    public static <E> void swapAll(List<E> list, int off, int len) {
        if (list instanceof RandomAccess) {
            int last = off + len - 1;
            for (int i = off, j = last; i < j; i++, j--) {
                list.set(i, list.set(j, list.get(i)));
            }
        } else {
            int end = off + len;
            ListIterator<E> iteratorI = list.listIterator(off);
            ListIterator<E> iteratorJ = list.listIterator(end);
            E tmp;
            
            while (iteratorI.nextIndex() < iteratorJ.nextIndex()) {
                tmp = iteratorI.next();
                iteratorI.set(iteratorJ.previous());
                iteratorJ.set(tmp);
            }
        }
    }
    
    public static <V> void get(Map<?, ? extends V> map,
            Collection<? extends Object> keys, Collection<? super V> values) {
        for (Object key : keys) {
            if (!map.containsKey(key)) continue;
            
            V value = map.get(key);
            values.add(value);
        }
    }
    
    public static <V> void get(Map<?, ? extends V> map,
            Collection<? super V> values, Object... keys) {
        for (int i = 0; i < keys.length; i++) {
            Object key = keys[i];
            if (!map.containsKey(key)) continue;
            
            V value = map.get(key);
            values.add(value);
        }
    }
    
    public static <V> void getNotNull(Map<?, ? extends V> map,
            Collection<? extends Object> keys, Collection<? super V> values) {
        for (Object key : keys) {
            V value = map.get(key);
            if (value != null) values.add(value);
        }
    }
    
    public static <V> void getNotNull(Map<?, ? extends V> map,
            Collection<? super V> values, Object... keys) {
        for (int i = 0; i < keys.length; i++) {
            Object key = keys[i];
            V value = map.get(key);
            if (value != null) values.add(value);
        }
    }
    
    public static <V> HashSet<V> getHashSet(Map<?, ? extends V> map,
            Collection<? extends Object> keys) {
        HashSet<V> result = new HashSet<V>();
        get(map, keys, result);
        return result;
    }
    
    public static <V> HashSet<V> getHashSet(Map<?, ? extends V> map,
            Object... keys) {
        HashSet<V> result = new HashSet<V>();
        get(map, result, keys);
        return result;
    }
    
    public static <V> HashSet<V> getHashSetNotNull(Map<?, ? extends V> map,
            Collection<? extends Object> keys) {
        HashSet<V> result = new HashSet<V>();
        getNotNull(map, keys, result);
        return result;
    }
    
    public static <V> HashSet<V> getHashSetNotNull(Map<?, ? extends V> map,
            Object... keys) {
        HashSet<V> result = new HashSet<V>();
        getNotNull(map, result, keys);
        return result;
    }
    
    public static <E> E[] toArray(Collection<? extends E> from, E[] to) {
        return toArray(from, to, 0);
    }
    
    public static <E> E[] toArray(Collection<? extends E> from, E[] to, int off) {
        return toArray(from, to, off, from.size());
    }
    
    public static <E> E[] toArray(Collection<? extends E> from, E[] to,
            int off, int len) {
        return IteratorUtil.toArray(from.iterator(), to, off, len);
    }
    
    private CollectionUtil() {}
    
}