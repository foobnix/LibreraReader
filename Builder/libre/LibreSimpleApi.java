package com.foobnix.aidle;

import java.nio.ByteBuffer;

import org.libreoffice.kit.Document;
import org.libreoffice.kit.LibreOfficeKit;
import org.libreoffice.kit.Office;

import android.content.Context;
import android.graphics.Bitmap;

public class LibreSimpleApi {


    static Document documentCache;
    static String pathCache;

    public static int getPagesCount(String path, Context c) {
        Document document = getDocument(path, c);
        if (document != null) {
            int parts = document.getParts();
            //document.destroy();
            return parts;
        }
        return 0;
    }

    public static Bitmap getPageBitmap(String path, int page, int width, Context c) {
        Document mDocument = getDocument(path, c);
        Bitmap thumbnail = thumbnail(mDocument, width, page);
        //mDocument.destroy();
        return thumbnail;
    }

    private static Document getDocument(String path, Context c) {
        if (path.equals(pathCache) && documentCache != null) {
            return documentCache;
        }
        if (documentCache != null) {
            documentCache.destroy();
        }



        Office mOffice = new Office(LibreOfficeKit.getLibreOfficeKitHandle());
        Document mDocument = mOffice.documentLoad(path);

        if (mDocument == null) {
            mOffice.destroy();
            ByteBuffer handle = LibreOfficeKit.getLibreOfficeKitHandle();
            mOffice = new Office(handle);
            mDocument = mOffice.documentLoad(path);
        }

        if (mDocument != null) {
            mDocument.initializeForRendering();
        }

//        if (checkDocument(mOffice, mDocument)) {
//            mDocument.setPart(0);
//            // setupDocumentFonts(mDocument);
//        }
        pathCache = path;
        documentCache = mDocument;


        return mDocument;
    }

    public static Bitmap thumbnail(Document mDocument, int size, int page) {

        int mWidthTwip = (int) mDocument.getDocumentWidth();
        int mHeightTwip = (int) (mDocument.getDocumentHeight() / mDocument.getParts());

        if (mDocument.getDocumentType() == Document.DOCTYPE_PRESENTATION) {
            mDocument.setPart(page - 1);
            mHeightTwip = (int) (mDocument.getDocumentHeight());
            page = 1;
        }

        int widthPixel = mWidthTwip;
        int heightPixel = mHeightTwip;

        if (widthPixel > heightPixel) {
            double ratio = heightPixel / (double) widthPixel;
            widthPixel = size;
            heightPixel = (int) (widthPixel * ratio);
        } else {
            double ratio = widthPixel / (double) heightPixel;
            heightPixel = size;
            widthPixel = (int) (heightPixel * ratio);
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(widthPixel * heightPixel * 4);
        if (mDocument != null)
            mDocument.paintTile(buffer, widthPixel, heightPixel, 0, (page - 1) * mHeightTwip, mWidthTwip, mHeightTwip);

        Bitmap bitmap = Bitmap.createBitmap(widthPixel, heightPixel, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        return bitmap;
    }






}
