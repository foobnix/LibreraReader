package com.foobnix.ui2.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.compose.ui.platform.ComposeView;
import androidx.core.util.Pair;

import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;

import mobi.librera.lib.gdrive.GoogleSignInComposeHelper;

public class GoogleDriveFragment2 extends UIFragment<FileMeta> {
    public static final Pair<Integer, Integer> PAIR = new Pair<Integer, Integer>(R.string.clouds, R.drawable.glyphicons_544_cloud);

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    View topPanel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_google_drive, container, false);

        ComposeView composeView = view.findViewById(R.id.compose_view);

        GoogleSignInComposeHelper.createSimpleGoogleSignInButton(
                composeView, getString(R.string.default_web_client_id)

        );
        topPanel = view.findViewById(R.id.topPanel);

        TintUtil.setBackgroundFillColor(topPanel, TintUtil.color);
        return view;
    }

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(topPanel, TintUtil.color);

    }

    @Override
    public void notifyFragment() {

    }

    @Override
    public void resetFragment() {

    }

}
