package org.emdev.utils.collections;
import android.util.SparseArray;

public class SymbolTree<E> {

    private final Node<E> root = new Node<E>();

    public void add(final E value, final String s) {
        add(value, s.toCharArray(), 0, s.length());
    }

    public void add(final E value, final char[] ch, final int start, final int length) {
        Node<E> node = root;
        for (int i = 0; i < length;) {
            final char c = ch[start + i];
            if (node.children == null) {
                node.children = new SparseArray<SymbolTree.Node<E>>(8);
            }

            final Node<E> child = node.children.get(c);
            if (child == null) {
                final Node<E> term = new Node<E>();
                term.ch = ch;
                term.start = start + i + 1;
                term.length = length - i - 1;
                term.value = value;
                node.children.append(c, term);
                return;
            }

            if (child.length == 0) {
                node = child;
                i++;
                continue;
            }

            final int tail = length - i - 1;
            if (tail <= 0) {
                final Node<E> inter = new Node<E>();
                node.children.append(c, inter);

                inter.children = new SparseArray<SymbolTree.Node<E>>(8);
                inter.children.append(child.ch[child.start], child);

                child.start += 1;
                child.length -= 1;

                inter.value = value;
                return;
            }

            int pref = 0;

            for (final int n = Math.min(child.length, tail); pref < n; pref++) {
                if (child.ch[child.start + pref] != ch[start + i + 1 + pref]) {
                    break;
                }
            }

            if (pref == child.length) {
                node = child;
                i += pref + 1;
                continue;
            } else if (pref > 0) {
                final Node<E> inter = new Node<E>();
                node.children.append(c, inter);

                inter.ch = child.ch;
                inter.start = child.start;
                inter.length = pref;
                inter.value = null;
                inter.children = new SparseArray<SymbolTree.Node<E>>(8);
                inter.children.append(child.ch[child.start + pref], child);

                child.start += pref + 1;
                child.length -= pref + 1;

                if (tail == pref) {
                    inter.value = value;
                    return;
                }

                node = inter;
                i += pref + 1;
                continue;

            } else {
                final Node<E> inter = new Node<E>();
                node.children.append(c, inter);

                inter.children = new SparseArray<SymbolTree.Node<E>>(8);
                inter.children.append(child.ch[child.start], child);

                child.start += 1;
                child.length -= 1;

                node = inter;
                i++;
                continue;
            }
        }
    }

    public E get(final String s) {
        return get(s.toCharArray(), 0, s.length());
    }

    public E get(final char[] ch, final int start, final int length) {
        Node<E> node = root;
        for (int i = 0; i < length;) {
            if (node.children == null) {
                return null;
            }
            final char c = ch[start + i];
            final Node<E> child = node.children.get(c);
            if (child == null) {
                return null;
            }
            final int tail = length - i - 1;
            if (child.length == 0) {
                if (tail == 0) {
                    return child.value;
                }
                node = child;
                i++;
                continue;
            }

            if (child.length > tail) {
                return null;
            }
            if (child.length <= tail) {
                for (int ii = 0; ii < child.length; ii++) {
                    if (child.ch[ii + child.start] != ch[start + i + 1 + ii]) {
                        return null;
                    }
                }
                if (child.length == tail) {
                    return child.value;
                }
                i += 1 + child.length;
            }
            node = child;
        }

        return null;
    }

    private static class Node<E> {

        char[] ch;
        int start;
        int length;
        E value;
        SparseArray<SymbolTree.Node<E>> children;

        Node() {
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder("<");

            if (length > 0) {
                for (int i = 0; i < length; i++) {
                    buf.append(ch[start + i]);
                }
            }

            buf.append(":");
            buf.append(value);
            buf.append(":");
            buf.append(children);
            return buf.append(">").toString();
        }
    }

    public void clear() {
        // TODO Auto-generated method stub

    }
}
