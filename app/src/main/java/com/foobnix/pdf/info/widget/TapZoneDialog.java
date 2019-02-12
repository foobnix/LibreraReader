package com.foobnix.pdf.info.widget;

import java.util.Arrays;
import java.util.List;

import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.Views;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;

public class TapZoneDialog {

	public static void show(final Context c) {
		final View inflate = LayoutInflater.from(c).inflate(R.layout.dialog_tap_zone, null, false);

        final List<String> items = Arrays.asList(c.getString(R.string.next_page), c.getString(R.string.previous_page), c.getString(R.string.db_do_nothing));

		final BaseItemLayoutAdapter<String> adapter = new BaseItemLayoutAdapter<String>(c,
				android.R.layout.simple_spinner_dropdown_item, items) {

			@Override
			public void populateView(View inflate, int arg1, String value) {
				Views.text(inflate, android.R.id.text1, "" + value);
			}

			@Override
			public String getItem(int position) {
				return items.get(position);
			}
		};

		final Spinner leftSide = (Spinner) inflate.findViewById(R.id.leftSide);
		final Spinner rightSide = (Spinner) inflate.findViewById(R.id.rightSide);
		final Spinner topSide = (Spinner) inflate.findViewById(R.id.topSide);
		final Spinner bottomSide = (Spinner) inflate.findViewById(R.id.bottomSide);

		leftSide.setAdapter(adapter);
		rightSide.setAdapter(adapter);
		topSide.setAdapter(adapter);
		bottomSide.setAdapter(adapter);

		leftSide.setSelection(AppState.get().tapZoneLeft, false);
		rightSide.setSelection(AppState.get().tapZoneRight, false);
		topSide.setSelection(AppState.get().tapZoneTop, false);
		bottomSide.setSelection(AppState.get().tapZoneBottom, false);

		final AlertDialog.Builder builder = new AlertDialog.Builder(c);
		builder.setTitle(R.string.customize_tap_zones);
		builder.setView(inflate);

		builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				AppState.get().tapZoneLeft = leftSide.getSelectedItemPosition();
				AppState.get().tapZoneRight = rightSide.getSelectedItemPosition();
				AppState.get().tapZoneTop = topSide.getSelectedItemPosition();
				AppState.get().tapZoneBottom = bottomSide.getSelectedItemPosition();
			}
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.show();

	}

}
