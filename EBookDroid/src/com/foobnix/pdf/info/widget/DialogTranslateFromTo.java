package com.foobnix.pdf.info.widget;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ebookdroid.LibreraApp;

import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Views;
import com.foobnix.pdf.info.DictsHelper;
import com.foobnix.pdf.info.DictsHelper.DictItem;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class DialogTranslateFromTo {
    static Map<String, String> langs = new LinkedHashMap<String, String>();
    static {
        langs.put("Afrikaans", "af");
        langs.put("Albanian", "sq");
        langs.put("Arabic", "ar");
        langs.put("Armenian", "hy");
        langs.put("Azerbaijani", "az");
        langs.put("Basque", "eu");
        langs.put("Belarusian", "be");
        langs.put("Bengali", "bn");
        langs.put("Bosnian", "bs");
        langs.put("Bulgarian", "bg");
        langs.put("Catalan", "ca");
        langs.put("Cebuano", "ceb");
        langs.put("Chichewa", "ny");
        langs.put("Chinese", "zh");
        langs.put("Chinese Traditional", "zh-TW");
        langs.put("Croatian", "hr");
        langs.put("Czech", "cs");
        langs.put("Danish", "da");
        langs.put("Dutch", "nl");
        langs.put("English", "en");
        langs.put("Esperanto", "eo");
        langs.put("Estonian", "et");
        langs.put("Filipino", "tl");
        langs.put("Finnish", "fi");
        langs.put("French", "fr");
        langs.put("Galician", "gl");
        langs.put("Georgian", "ka");
        langs.put("German", "de");
        langs.put("Greek", "el");
        langs.put("Gujarati", "gu");
        langs.put("Haitian Creole", "ht");
        langs.put("Hausa", "ha");
        langs.put("Hebrew", "iw");
        langs.put("Hindi", "hi");
        langs.put("Hmong", "hmn");
        langs.put("Hungarian", "hu");
        langs.put("Icelandic", "is");
        langs.put("Igbo", "ig");
        langs.put("Indonesian", "id");
        langs.put("Irish", "ga");
        langs.put("Italian", "it");
        langs.put("Japanese", "ja");
        langs.put("Javanese", "jw");
        langs.put("Kannada", "kn");
        langs.put("Kazakh", "kk");
        langs.put("Khmer", "km");
        langs.put("Korean", "ko");
        langs.put("Lao", "lo");
        langs.put("Latin", "la");
        langs.put("Latvian", "lv");
        langs.put("Lithuanian", "lt");
        langs.put("Macedonian", "mk");
        langs.put("Malagasy", "mg");
        langs.put("Malay", "ms");
        langs.put("Malayalam", "ml");
        langs.put("Maltese", "mt");
        langs.put("Maori", "mi");
        langs.put("Marathi", "mr");
        langs.put("Mongolian", "mn");
        langs.put("Myanmar (Burmese)", "my");
        langs.put("Nepali", "ne");
        langs.put("Norwegian", "no");
        langs.put("Persian", "fa");
        langs.put("Polish", "pl");
        langs.put("Portuguese", "pt");
        langs.put("Punjabi", "ma");
        langs.put("Romanian", "ro");
        langs.put("Russian", "ru");
        langs.put("Serbian", "sr");
        langs.put("Sesotho", "st");
        langs.put("Sinhala", "si");
        langs.put("Slovak", "sk");
        langs.put("Slovenian", "sl");
        langs.put("Somali", "so");
        langs.put("Spanish", "es");
        langs.put("Sudanese", "su");
        langs.put("Swahili", "sw");
        langs.put("Swedish", "sv");
        langs.put("Tajik", "tg");
        langs.put("Tamil", "ta");
        langs.put("Telugu", "te");
        langs.put("Thai", "th");
        langs.put("Turkish", "tr");
        langs.put("Ukrainian", "uk");
        langs.put("Urdu", "ur");
        langs.put("Uzbek", "uz");
        langs.put("Vietnamese", "vi");
        langs.put("Welsh", "cy");
        langs.put("Yiddish", "yi");
        langs.put("Yoruba", "yo");
        langs.put("Zulu", "zu");
    }

    public static String getLanuageByCode(String code) {
        try {
            if (LibreraApp.context == null) {
                return "";
            }
            if (AppState.MY_SYSTEM_LANG.equals(code)) {
                return LibreraApp.context.getString(R.string.system_language);
            }
            try {
                Locale l = new Locale(code);
                return TxtUtils.firstUppercase(l.getDisplayLanguage(l));
            } catch (Exception e) {
            }

            if (TxtUtils.isEmpty(code)) {
                return "";
            }
            if (code.length() > 2) {
                code = code.substring(0, 2);
            }
            for (String key : langs.keySet()) {
                String value = langs.get(key);
                if (code.equals(value)) {
                    return key;
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return code;

    }

    public static Spanned getSelectedDictionaryUnderline() {
        return Html.fromHtml("<u>" + getSelectedDictionary() + "</u>");
    }

    public static String getSelectedDictionary() {
        return DictItem.fetchDictName(AppState.get().rememberDict);
    }

    public static AlertDialog show(final Activity a, final Runnable runnable) {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(a);
        alertDialog.setTitle(R.string.remember_dictionary);

        View body = LayoutInflater.from(a).inflate(R.layout.dialog_translate_from_to, null, false);

        final Spinner spinnerFrom = (Spinner) body.findViewById(R.id.spinnerFrom);
        final Spinner spinnerTo = (Spinner) body.findViewById(R.id.spinnerTo);

        ImageView imageOk = (ImageView) body.findViewById(R.id.imageOK);

        final List<String> langNames = new ArrayList<String>(langs.keySet());
        final List<String> langCodes = new ArrayList<String>(langs.values());

        spinnerFrom.setAdapter(new BaseItemLayoutAdapter<String>(a, android.R.layout.simple_spinner_dropdown_item, langNames) {

            @Override
            public void populateView(View inflate, int arg1, String value) {
                Views.text(inflate, android.R.id.text1, "" + value);
            }
        });

        spinnerFrom.setSelection(langCodes.indexOf(AppState.get().fromLang));

        spinnerTo.setAdapter(new BaseItemLayoutAdapter<String>(a, android.R.layout.simple_spinner_dropdown_item, langNames) {

            @Override
            public void populateView(View inflate, int arg1, String value) {
                Views.text(inflate, android.R.id.text1, "" + value);
            }
        });

        spinnerTo.setSelection(langCodes.indexOf(AppState.get().toLang));

        final ListView dictSpinner = (ListView) body.findViewById(R.id.dictionaries);

        final List<DictItem> list = DictsHelper.getAllResolveInfoAsDictItem1(a, "");
        list.addAll(DictsHelper.getOnlineDicts(a, ""));

        dictSpinner.setAdapter(new BaseItemLayoutAdapter<DictItem>(a, R.layout.item_dict_line, list) {
            @Override
            public void populateView(View layout, int position, DictItem item) {
                ((TextView) layout.findViewById(R.id.text1)).setText(item.name);
                ((TextView) layout.findViewById(R.id.type1)).setText(item.type);
                if (item.image == null) {
                    ((ImageView) layout.findViewById(R.id.image1)).setImageResource(R.drawable.web);
                } else {
                    ((ImageView) layout.findViewById(R.id.image1)).setImageDrawable(item.image);
                }
            }
        });

        body.findViewById(R.id.onReverse).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                int p1 = spinnerFrom.getSelectedItemPosition();
                int p2 = spinnerTo.getSelectedItemPosition();
                spinnerTo.setSelection(p1);
                spinnerFrom.setSelection(p2);

            }
        });

        alertDialog.setView(body);

        alertDialog.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        final AlertDialog show = alertDialog.show();
        dictSpinner.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppState.get().rememberDict = list.get(position).toString();

                AppState.get().fromLang = langCodes.get(spinnerFrom.getSelectedItemPosition());
                AppState.get().toLang = langCodes.get(spinnerTo.getSelectedItemPosition());
                try {
                    show.dismiss();
                } catch (Exception e) {
                }
                runnable.run();
            }
        });

        imageOk.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().fromLang = langCodes.get(spinnerFrom.getSelectedItemPosition());
                AppState.get().toLang = langCodes.get(spinnerTo.getSelectedItemPosition());
                try {
                    show.dismiss();
                } catch (Exception e) {
                }
            }
        });

        return show;

    }

}
