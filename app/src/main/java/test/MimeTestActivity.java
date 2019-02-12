package test;

import java.io.File;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import android.widget.TextView;

public class MimeTestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView text = new TextView(this);

        Intent intent = getIntent();

        StringBuilder builder = new StringBuilder();
        builder.append("\n Scheme: " + intent.getScheme());
        builder.append("\n Type: " + intent.getType());
        try {
            builder.append("\n Intent: " + intent);
        } catch (Exception e) {
            builder.append("\n Intent:");
        }

        try {
            Bundle extras = intent.getExtras();
            Set<String> keySet = extras.keySet();
            for (String key : keySet) {
                builder.append("\n Extra: " + key + " : " + extras.get(key));
            }
        } catch (Exception e) {
            builder.append("\n Extras:");
        }
        try {
            builder.append("\n Type1: " + getContentResolver().getType(intent.getData()));
        } catch (Exception e) {
            builder.append("\n Type1:");
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

        setContentView(text);

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
