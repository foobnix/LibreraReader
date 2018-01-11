package com.artifex.mupdf.fitz;

public interface PathWalker
{
	public void moveTo(float x, float y);
	public void lineTo(float x, float y);
	public void curveTo(float cx1, float cy1, float cx2, float cy2, float ex, float ey);
	public void closePath();
}
