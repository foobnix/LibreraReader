package com.foobnix.pdf.search.activity;

import java.util.ArrayList;
import java.util.List;

import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.TextWord;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;

import android.graphics.Matrix;
import android.util.SparseArray;

public class PageImageState {
    private static final int M9 = 9;

    private final static PageImageState instance = new PageImageState();

    private SparseArray<List<TextWord>> selectedWords = new SparseArray<List<TextWord>>();
    public final SparseArray<TextWord[][]> pagesText = new SparseArray<TextWord[][]>();
    public final SparseArray<List<PageLink>> pagesLinks = new SparseArray<List<PageLink>>();
    private Matrix matrix = new Matrix();

    public static volatile int currentPage = 0;

    public boolean isAutoFit = true;
    public boolean isShowCuttingLine = false;

    public boolean needAutoFit = false;

    public static PageImageState get() {
        return instance;
    }

    public void clearResouces() {
        selectedWords.clear();
        pagesText.clear();
        pagesLinks.clear();
    }

    public List<TextWord> getSelectedWords(int page) {
        return selectedWords.get(page);
    }

    public void putWords(int page, List<TextWord> words) {
        selectedWords.put(page, words);
    }

    public void addWord(int page, TextWord word) {
        List<TextWord> list = selectedWords.get(page);
        if (list == null) {
            list = new ArrayList<TextWord>();
            selectedWords.put(page, list);
        }
        list.add(word);
    }

    public void cleanSelectedWords() {
        selectedWords.clear();
    }

    public Matrix getMatrix() {
        return matrix;
    }

    public String getMatrixAsString() {
        return fromMatrix(matrix);
    }

    public static Matrix fromString(String value) {
        Matrix matrix = new Matrix();
        if (TxtUtils.isEmpty(value)) {
            return matrix;
        }

        try {
            float[] values = new float[M9];
            String[] split = value.split(",");
            for (int i = 0; i < M9; i++) {
                values[i] = Float.valueOf(split[i]);
            }
            matrix.setValues(values);
        } catch (Exception e) {
            LOG.e(e);
        }
        return matrix;
    }

    public static String fromMatrix(Matrix matrix) {
        float[] values = new float[M9];
        matrix.getValues(values);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < M9; i++) {
            out.append(values[i]);
            if (i != M9 - 1) {
                out.append(",");
            }
        }
        return out.toString();

    }

}
