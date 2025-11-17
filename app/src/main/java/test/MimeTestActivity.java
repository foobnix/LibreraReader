package test;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import android.widget.TextView;

import java.io.File;
import java.util.Objects;
import java.util.Set;

public class MimeTestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView text = new TextView(this);

        Intent intent = getIntent();

        StringBuilder builder = new StringBuilder();
        builder.append("\n Scheme: ").append(intent.getScheme());
        builder.append("\n Type: ").append(intent.getType());
        try {
            builder.append("\n Intent: ").append(intent);
        } catch (Exception e) {
            builder.append("\n Intent:");
        }

        try {
            Bundle extras = intent.getExtras();
            assert extras != null;
            Set<String> keySet = extras.keySet();
            for (String key : keySet) {
                builder.append("\n Extra: ").append(key).append(" : ").append(extras.get(key));
            }
        } catch (Exception e) {
            builder.append("\n Extras:");
        }
        try {
            builder.append("\n Type1: ").append(getContentResolver().getType(intent.getData()));
        } catch (Exception e) {
            builder.append("\n Type1:");
        }
        try {
            builder.append("\n Type 2 : ").append(getMimeType(intent.getData().getPath()));

            builder.append("\n Action: ").append(intent.getAction());
            builder.append("\n Data: ").append(intent.getData());

            builder.append("\n Data Path ").append(intent.getData().getPath());
            builder.append("\n Data Path isFile ").append(new File(intent.getData().getPath()).exists());

            builder.append("\n--------\n");
            builder.append(intent);
            builder.append("\n--------\n");

            builder.append("\n DISPLAY_NAME: ").append(getFileName(intent.getData()));

            builder.append("\n--------\n");
        } catch (Exception e) {
            builder.append("\n Data 1 Path::");
        }

        text.setText(builder.toString());

        setContentView(text);

    }

    public String getFileName(Uri uri) {
        String result = null;
        if (Objects.equals(uri.getScheme(), "content")) {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
                assert cursor != null;
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
            assert result != null;
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
