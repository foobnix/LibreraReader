package org.ebookdroid.core.events;

public interface ZoomListener {

    void zoomChanged(float oldZoom, float newZoom, boolean committed);
}
