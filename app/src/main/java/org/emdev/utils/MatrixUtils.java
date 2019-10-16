package org.emdev.utils;

import android.graphics.Matrix;

public class MatrixUtils {

    private static final ThreadLocal<Matrix> objects = new ThreadLocal<Matrix>() {

        @Override
        protected Matrix initialValue() {
            return new Matrix();
        }
    };

    public static Matrix get() {
        Matrix matrix = objects.get();
        matrix.reset();
        return matrix;
    }
}
