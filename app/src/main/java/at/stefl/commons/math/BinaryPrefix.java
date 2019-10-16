package at.stefl.commons.math;

import java.math.BigDecimal;

public enum BinaryPrefix implements UnitPrefix {
    
    KIBI(10, 1, "Ki"), MEBI(20, 2, "Me"), GIBI(30, 3, "Gi"), TEBI(40, 4, "Ti"),
    PEBI(50, 5, "Pi"), EXBI(60, 6, "Ei"), ZEBI(70, 7, "Zi"), YOBI(80, 8, "Yi");
    
    public static final int BASE_2 = 2;
    public static final int BASE_1024 = 1024;
    public static final BigDecimal BIG_BASE_2 = BigDecimal.valueOf(2);
    public static final BigDecimal BIG_BASE_1024 = BigDecimal.valueOf(1024);
    
    private final String name;
    private final int exponent2;
    private final int exponent1024;
    private final double value;
    private final BigDecimal bigValue;
    private final String symbol;
    
    private BinaryPrefix(int exponent2, int exponent1024, String symbol) {
        this.name = super.toString().toLowerCase();
        this.exponent2 = exponent2;
        this.exponent1024 = exponent1024;
        this.value = Math.pow(1024, exponent1024);
        this.bigValue = BigDecimal.valueOf(1024).pow(exponent1024);
        this.symbol = symbol;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    public int exponent2() {
        return exponent2;
    }
    
    public int exponent1024() {
        return exponent1024;
    }
    
    @Override
    public double value() {
        return value;
    }
    
    @Override
    public BigDecimal bigValue() {
        return bigValue;
    }
    
    @Override
    public String symbol() {
        return symbol;
    }
    
}