package com.foobnix.pdf.info;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.wrapper.AppState;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class AndroidWhatsNew {

    public static void show(final Context c) {
        String versionName = Apps.getVersionName(c);
        View inflate = LayoutInflater.from(c).inflate(R.layout.whatsnew, null, false);

        TextView notes = (TextView) inflate.findViewById(R.id.textNotes);
        TextView textRateIt = (TextView) inflate.findViewById(R.id.textRateIt);
        TxtUtils.underlineTextView(textRateIt);
        textRateIt.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                rateIT(c);
            }
        });
        String textNotes = "loading...";
        try {
            String langCode = Urls.getLangCode();
            textNotes = getWhatsNew(c, langCode);
        } catch (Exception e) {
            try {
                textNotes = getWhatsNew(c, "en");
            } catch (Exception e1) {
                textNotes = "erorr...";
            }
            LOG.d(e);
        }

        notes.setText(textNotes);

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(c.getString(R.string.what_is_new) + " " + AppsConfig.APP_NAME + " " + versionName);
        builder.setView(inflate);
        builder.setNegativeButton(R.string.close, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.setPositiveButton(R.string.write_feedback, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                rateIT(c);
            }
        });

        builder.show();
    }

    public static String getWhatsNew(Context c, String langCode) throws IOException {
        InputStream input = c.getResources().getAssets().open("whatsnew/" + langCode + ".txt");
        return steamToString(input);

    }

    public static void checkWhatsNew(final Context c) {
        if (!AppState.get().isShowWhatIsNewDialog) {
            return;
        }
        String currentVersion = Apps.getVersionName(c);
        String oldVersion = AppState.get().versionNew;

        if (TxtUtils.isEmpty(oldVersion) || !isEqualsFirstSecondDigit(currentVersion, oldVersion)) {
            show(c);
            AppState.get().versionNew = currentVersion;
            AppState.get().save(c);
        }
    }

    public static void rateIT(Context c) {
        try {
            Urls.open(c, "market://details?id=" + c.getPackageName());
        } catch (Exception e) {
            Urls.open(c, "https://play.google.com/store/apps/details?id=" + c.getPackageName());
            LOG.e(e);
        }
    }

    public static String steamToString(InputStream input) {
        StringBuilder content = new StringBuilder();
        try {

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                content.append(ExtUtils.upperCaseFirst(line) + "\n");
            }
            bufferedReader.close();
        } catch (Exception e) {
            LOG.e(e);
        }
        return content.toString();
    }

    public static boolean isEqualsFirstSecondDigit(String first, String second) {
        try {
            LOG.d("Compare first second", first, second);
            first = first.replace("beta:", "");
            first = second.replace("beta:", "");
            String[] splitFist = first.split(Pattern.quote("."));
            String[] splitSecond = second.split(Pattern.quote("."));

            if (splitFist[0].equals(splitSecond[0]) && splitFist[1].equals(splitSecond[1])) {
                LOG.d("Compare first second", true);
                return true;
            }

            LOG.d("Compare first second", false);

            return false;
        } catch (Exception e) {
            LOG.e(e);
            return true;
        }
    }

}
