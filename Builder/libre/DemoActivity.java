package com.foobnix.aidle;

import org.libreoffice.kit.LibreOfficeKit;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.Toast;

public class DemoActivity extends Activity {
    RemoteServiceConnection serviceConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //LibreOfficeKit.putenv("SAL_LOG=+WARN+INFO");
      //  LibreOfficeKit.init(this);

        ImageView image = new ImageView(this);

        // openUrl("/storage/emulated/0/example.odt", 1);
        // image = openUrl("/storage/emulated/0/example.odt", 2);

        // openUrl("/storage/emulated/0/example1.odt", 1);
        // image = openUrl("/storage/emulated/0/example1.odt", 2);

        openUrl("/storage/emulated/0/example.pptx", 1);
        image = openUrl("/storage/emulated/0/example.pptx", 3);

        setContentView(image);



        serviceConnection = new RemoteServiceConnection();

        Intent intent = new Intent("myLirbeService");
        intent.setPackage("org.libreoffice");
        boolean result = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        Toast.makeText(this, "Result " + result, Toast.LENGTH_LONG).show();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    class RemoteServiceConnection implements ServiceConnection {
        IDataOfficeService service;
        @Override
        public void onServiceConnected(ComponentName name, IBinder boundService) {
            service = IDataOfficeService.Stub.asInterface(boundService);
            Toast.makeText(DemoActivity.this, "onService Connected ", Toast.LENGTH_LONG).show();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(DemoActivity.this, "onService Disconnected ", Toast.LENGTH_LONG).show();
            service = null;
        }
    }

    private ImageView openUrl(String path, int page) {
        ImageView image = new ImageView(this);

        LibreSimpleApi.getPagesCount(path, this);

        Bitmap pageBitmap = LibreSimpleApi.getPageBitmap(path, page, 1000, this);
        image.setImageBitmap(pageBitmap);
        image.setAdjustViewBounds(true);
        image.setScaleType(ScaleType.CENTER_CROP);
        return image;
    }

}
