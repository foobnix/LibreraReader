package com.foobnix.pdf.info.demo;

import java.util.List;

import org.ebookdroid.core.codec.PageLink;

import com.foobnix.android.utils.ResultResponse;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.DocumentWrapperUI;

import android.app.Activity;
import android.graphics.PointF;
import android.view.WindowManager;

public class DemoController extends DocumentController {

    public DemoController(final Activity a, final DocumentWrapperUI ui) {
        super(a);
    }

    @Override
    public void underlineText(int color, float width, AnnotationType type) {
    }

    @Override
    public void deleteAnnotation(long pageHandler, int page, int index) {
    }


    @Override
    public int getCurentPageFirst1() {
        return 0;
    }

    @Override
    public void onCrop() {
    }

    @Override
    public void onScrollY(final int value) {
    }

    @Override
    public List<PageLink> getLinksForPage(int page) {
        return null;
    }

    @Override
    public void onGoToPage(final int progress) {
        toast("seek" + progress);
    }

    @Override
    public void cleanImageMatrix() {
    }

    @Override
    public void saveAnnotationsToFile() {
    }

    @Override
    public void onZoomInOut(int x, int y) {

    }

    @Override
    public void recenterDocument() {
    }

    @Override
    public String getFootNote(String text) {
        return "";
    }

    @Override
    public String getPagePath(int page) {
        return null;
    }

    @Override
    public void clearSelectedText() {

    }

    @Override
    public void onSrollLeft() {
        toast("onSrollLeft");
    }

    @Override
    public void onSrollRight() {
        toast("onSrollRight");
    }

    @Override
    public void onNextPage(final boolean animate) {
        toast("onNextPage");
    }

    @Override
    public void onPrevPage(final boolean animate) {
        toast("onPrevPage");
    }

    @Override
    public void onNextScreen(final boolean animate) {
        toast("onNextScreen");
    }

    @Override
    public String getTextForPage(int page) {
        return "not text";
    }

    @Override
    public void onPrevScreen(final boolean animate) {
        toast("onNextScreen");
    }

    @Override
    public void onZoomInc() {
        toast("onZoomInc");
    }

    @Override
    public void saveChanges(List<PointF> points, int color) {
    }

    @Override
    public void onZoomDec() {
        toast("onZoomDec");
    }

    @Override
    public boolean isCropCurrentBook() {
        return false;
    }

    @Override
    public void getOutline(ResultResponse<List<OutlineLinkWrapper>> outline) {
    }

    @Override
    public void doSearch(String text, ResultResponse<Integer> result) {
    }

    @Override
    public void onFullScreen() {
        AppState.getInstance().setFullScrean(!AppState.getInstance().isFullScrean());
        if (AppState.getInstance().isFullScrean()) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        } else {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public List<String> getMediaAttachments() {
        return null;
    }

    @Override
    public void onAutoScroll() {
    }

    @Override
    public void onCloseActivity() {
        saveSettings();
        getActivity().finish();
    }

    @Override
    public void onNightMode() {
        toast("onNightMode");
    }

    @Override
    public int getCurentPage() {
        return 1;
    }

    @Override
    public int getPageCount() {
        return 100;
    }

    @Override
    public void onScrollUp() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onScrollDown() {
        // TODO Auto-generated method stub

    }

}
