package at.stefl.commons.util.string;

public abstract class AbstractCharSequence implements CharSequence {
    
    @Override
    public int hashCode() {
        return CharSequenceUtil.hashCode(this);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        
        if (!(obj instanceof CharSequence)) return false;
        CharSequence other = (CharSequence) obj;
        
        return CharSequenceUtil.equals(this, other);
    }
    
    @Override
    public String toString() {
        return CharSequenceUtil.toString(this);
    }
    
}