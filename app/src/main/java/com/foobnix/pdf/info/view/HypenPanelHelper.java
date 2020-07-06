package com.foobnix.pdf.info.view;

import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.hypen.HyphenPattern;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.widget.DialogTranslateFromTo;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.ui2.AppDB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HypenPanelHelper {


    public static void init(View parent, DocumentController dc) {
        View hyphenPanel = parent.findViewById(R.id.showHypenLangPanel);
        hyphenPanel.setVisibility(TxtUtils.visibleIf(dc.isTextFormat() && BookCSS.get().isAutoHypens && TxtUtils.isEmpty(AppSP.get().hypenLang)));


        final TextView hypenLang = (TextView) parent.findViewById(R.id.hypenLang);
        final TextView hypenApply = (TextView) parent.findViewById(R.id.hypenApply);

        hypenLang.setText(R.string.choose_);

        TxtUtils.underlineTextView(hypenLang);
        TxtUtils.underlineTextView(hypenApply);

        hypenLang.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

                HyphenPattern[] values = HyphenPattern.values();

                List<String> all = new ArrayList<String>();

                for (HyphenPattern p : values) {
                    String e1 = DialogTranslateFromTo.getLanuageByCode(p.lang) + ":" + p.lang;
                    all.add(e1);

                }
                Collections.sort(all);
                if(TxtUtils.isEmpty(AppSP.get().lastBookLang)){
                    AppSP.get().lastBookLang = AppState.get().appLang.equals(AppState.MY_SYSTEM_LANG) ? Urls.getLangCode() : AppState.get().appLang;
                }
                String e = DialogTranslateFromTo.getLanuageByCode(AppSP.get().lastBookLang) + ":" + AppSP.get().lastBookLang;
                all.add(0, e);

                for (final String langFull : all) {
                    String[] split = langFull.split(":");
                    final String titleLang = split[0];
                    final String code = split[1];
                    popupMenu.getMenu().add(titleLang).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {


                            AppSP.get().hypenLang = code;
                            AppSP.get().lastBookLang = code;
                            hypenLang.setText(titleLang);
                            TxtUtils.underlineTextView(hypenLang);
                            FileMeta load = AppDB.get().load(dc.getCurrentBook().getPath());
                            if (load != null) {
                                load.setLang(code);
                                AppDB.get().update(load);
                            }
                            dc.restartActivity();
                            return false;
                        }
                    });
                }
                popupMenu.show();

            }
        });
        hypenApply.setVisibility(View.GONE);
        hypenApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TxtUtils.isNotEmpty(AppSP.get().hypenLang)) {
                    dc.restartActivity();
                }
            }
        });

    }
}
