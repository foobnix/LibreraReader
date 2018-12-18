package at.stefl.svm.object;

public class Fraction {
    
    private int numeratior;
    private int denominator;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Fraction [numeratior=");
        builder.append(numeratior);
        builder.append(", denominator=");
        builder.append(denominator);
        builder.append("]");
        return builder.toString();
    }
    
    public int getNumeratior() {
        return numeratior;
    }
    
    public int getDenominator() {
        return denominator;
    }
    
    public void setNumeratior(int numeratior) {
        this.numeratior = numeratior;
    }
    
    public void setDenominator(int denominator) {
        this.denominator = denominator;
    }
    
}