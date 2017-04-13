package com.example.zcontentintent;

import java.io.File;
import java.io.FileWriter;

import com.foobnix.aidle.IDataOfficeService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView) findViewById(R.id.text);
        image = (ImageView) findViewById(R.id.image);

        Intent intent = getIntent();

        StringBuilder builder = new StringBuilder();
        builder.append("\n Scheme: " + intent.getScheme());
        builder.append("\n Type: " + intent.getType());
        try {
            builder.append("\n Type1: " + getContentResolver().getType(intent.getData()));
        } catch (Exception e) {
            builder.append("\n Type1::");
        }
        try {
            builder.append("\n Type 2 : " + getMimeType(intent.getData().getPath()));

            builder.append("\n Action: " + intent.getAction());
            builder.append("\n Data: " + intent.getData());

            builder.append("\n Data Path " + intent.getData().getPath());
            builder.append("\n Data Path isFile " + new File(intent.getData().getPath()).exists());

            builder.append("\n--------\n");
            builder.append(intent);
            builder.append("\n--------\n");

            builder.append("\n DISPLAY_NAME: " + getFileName(intent.getData()));

            builder.append("\n--------\n");
        } catch (Exception e) {
            builder.append("\n Data 1 Path::");
        }

        text.setText(builder.toString());

        // connectService();

        // text.postDelayed(new Runnable() {
        //
        // @Override
        // public void run() {
        // try {
        // if (service != null) {
        // String path = "/storage/emulated/0/example.odt";
        // Bitmap page = service.getPageBitmap(path, 1, 1000);
        // text.setText("isFile: " + new File(path).isFile() + " page: " +
        // page);
        // image.setImageBitmap(page);
        // } else {
        // text.setText("service is null");
        // }
        // } catch (RemoteException e) {
        // e.printStackTrace();
        // }
        // }
        // }, 2000);

        findViewById(R.id.button1).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                make1000Files(100000);
            }
        });

    }

    Handler halder = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            text.setText("" + msg.what);
        }

    };

    public void make1000Files(final int number) {
        final File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Test");
        root.mkdirs();

        new AsyncTask() {

            @Override
            protected Object doInBackground(Object... params) {
                for (int i = 1; i <= number; i++) {
                    String string = String.format("Author %s - Title %s.txt", i, i);
                    File file = new File(root, string);
                    try {
                        FileWriter fileWriter = new FileWriter(file);
                        fileWriter.write(string);
                        fileWriter.close();
                    } catch (Exception e) {
                        throw new IllegalArgumentException(e.getMessage());
                    }
                    Message.obtain(halder, i).sendToTarget();

                    Log.d("TEST", "File " + i);
                }
                return null;
            }
        }.execute();

    }

    private IDataOfficeService service;
    private RemoteServiceConnection serviceConnection;
    private TextView text;
    private ImageView image;

    private void connectService() {
        serviceConnection = new RemoteServiceConnection();
        Intent i = new Intent("myLirbeService");
        i.setPackage("org.libreoffice");
        boolean bindService = bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);
        Toast.makeText(MainActivity.this, "bindService " + bindService, Toast.LENGTH_LONG).show();

    }

    class RemoteServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder boundService) {
            service = IDataOfficeService.Stub.asInterface(boundService);
            Toast.makeText(MainActivity.this, "Service connected", Toast.LENGTH_LONG).show();
            text.setText("connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
            Toast.makeText(MainActivity.this, "Service disconnected", Toast.LENGTH_LONG).show();
            text.setText("disconnected");
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, new String[] { OpenableColumns.DISPLAY_NAME }, null, null, null);
                if (cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {

            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

}
