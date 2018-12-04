package at.stefl.commons.util.string;

public class CharSequenceWraper extends AbstractCharSequence {
    
    private CharSequence charSequence;
    
    public CharSequenceWraper(CharSequence charSequence) {
        this.charSequence = charSequence;
    }
    
    @Override
    public int hashCode() {
        return CharSequenceUtil.hashCode(charSequence);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        
        if (obj instanceof CharSequenceWraper) {
            CharSequenceWraper other = (CharSequenceWraper) obj;
            return (charSequence == other.charSequence)
                    || CharSequenceUtil
                            .equals(charSequence, other.charSequence);
        } else if (obj instanceof CharSequence) {
            CharSequence other = (CharSequence) obj;
            return CharSequenceUtil.equals(this, other);
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return charSequence.toString();
    }
    
    public CharSequence getCharSequence() {
        return charSequence;
    }
    
    @Override
    public int length() {
        return charSequence.length();
    }
    
    @Override
    public char charAt(int index) {
        return charSequence.charAt(index);
    }
    
    @Override
    public CharSequence subSequence(int start, int end) {
        return new CharSequenceWraper(charSequence.subSequence(start, end));
    }
    
}