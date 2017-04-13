package com.foobnix.aidle;

interface IDataOfficeService{
		
		 Bitmap getPageBitmap(String path, int page, int width);
		 
		 int getPagesCount(String path);
		 
}