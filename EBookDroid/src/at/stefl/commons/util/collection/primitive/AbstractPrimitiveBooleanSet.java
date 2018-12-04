package at.stefl.commons.util.collection.primitive;

import java.util.Collection;

public abstract class AbstractPrimitiveBooleanSet extends AbstractPrimitiveBooleanCollection implements PrimitiveBooleanSet {

    public abstract boolean isFull();

    @Override
    public boolean addAll(Collection<? extends Boolean> c) {
        if (isFull())
            return false;

        if (c instanceof PrimitiveBooleanCollection) {
            PrimitiveBooleanCollection pc = (PrimitiveBooleanCollection) c;
            return ((pc.contains(false)) ? add(false) : false) | ((pc.contains(true)) ? add(true) : false);
        } else {
            return ((c.contains(false)) ? add(false) : false) | ((c.contains(true)) ? add(true) : false);
        }
    }

    @Override
    public abstract PrimitiveBooleanIterator iterator();

}