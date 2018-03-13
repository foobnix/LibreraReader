package com.foobnix.pdf.info;

import java.util.List;

import com.foobnix.pdf.info.model.OutlineLinkWrapper;

public class OutlineHelper {

    public static int getRootItemByPageNumber(List<OutlineLinkWrapper> outline, int pageNumber) {
        try {
            for (int i = 0; i < outline.size(); i++) {
                OutlineLinkWrapper item = outline.get(i);

                if (item.targetPage == pageNumber) {
                    return i;
                } else if (item.targetPage > pageNumber) {
                    return Math.max(0, i - 1);
                }

            }

            return outline.size() - 1;
        } catch (Exception e) {
            return 0;
        }
    }

    public static OutlineLinkWrapper getCurrentByPageNumber(List<OutlineLinkWrapper> outline, int pageNumber) {
        return outline.get(getRootItemByPageNumber(outline, pageNumber));
    }

}
