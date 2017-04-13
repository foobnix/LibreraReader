package com.foobnix.pdf.info.wrapper;

import android.util.TypedValue;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;


public class DocumentGuestureDetector extends SimpleOnGestureListener {

	private final View c;
	private final DocumentGestureListener listener;
	private float border;

	public DocumentGuestureDetector(View c, DocumentGestureListener listener) {
		this.c = c;
		this.listener = listener;
		border = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 10, c.getResources().getDisplayMetrics());
	}



	@Override
	public boolean onDoubleTap(final MotionEvent e) {
		int x = (int) e.getX();
		int maxX = c.getWidth();

		if (x > (maxX - border)) {
			// listener.onNextPage();
		} else if (x < border) {
			// listener.onPrevPage();
		} else {
			listener.onDoubleTap();
			return true;
		}
		return false;
	}

	@Override
	public boolean onDown(final MotionEvent ev) {
		int x = (int) ev.getX();
		int maxX = c.getWidth();

		if (x > (maxX - border)) {
			listener.onNextPage();
		} else if (x < border) {
			listener.onPrevPage();
		} else {
			return false;
		}

		return true;
	}


	/**
	 * {@inheritDoc}
	 * 
	 * @see android.view.GestureDetector.SimpleOnGestureListener#onSingleTapConfirmed(android.view.MotionEvent)
	 */
	@Override
	public boolean onSingleTapConfirmed(final MotionEvent e) {

		int x = (int) e.getX();
		int maxX = c.getWidth();
		if (x > (maxX - border)) {
			// listener.onNextPage();
		} else if (x < border) {
			// listener.onPrevPage();
		} else {
			listener.onSingleTap();
			return true;
		}
		return false;
	}

}
