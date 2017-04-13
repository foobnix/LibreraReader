package com.foobnix.aidle;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.RemoteException;

import org.libreoffice.kit.LibreOfficeKit;

public class LibreService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LibreOfficeKit.putenv("SAL_LOG=+WARN+INFO");
        LibreOfficeKit.init(this);
    }

    private final IDataOfficeService.Stub mBinder = new IDataOfficeService.Stub() {

        @Override
        public Bitmap getPageBitmap(String path, int page, int width) throws RemoteException {
            return LibreSimpleApi.getPageBitmap(path, page, width, LibreService.this);
        }

        @Override
        public int getPagesCount(String path) throws RemoteException {
            return LibreSimpleApi.getPagesCount(path, LibreService.this);
        }


    };

}
