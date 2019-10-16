package com.foobnix.pdf.info.wrapper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.foobnix.android.utils.Keyboards;
import com.foobnix.model.AppBookmark;
import com.foobnix.pdf.info.BookmarksData;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.presentation.BookmarksAdapter;

import java.util.List;

public class ListBoxHelper {

    public static void showAddDialog(final DocumentController controller, final List<AppBookmark> objects, final BookmarksAdapter bookmarksAdapter, String text, final Runnable onRefresh) {
        final Activity a = controller.getActivity();

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);

        builder.setTitle(a.getString(R.string.bookmark_on_page_) + " " + controller.getCurentPageFirst1());

        LinearLayout layout = new LinearLayout(a);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText editText = new EditText(a);
        // editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES |
        // InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        // editText.setHorizontallyScrolling(false);
        editText.setLines(6);
        editText.setGravity(Gravity.TOP);
        editText.setText(text);
        editText.requestFocus();


        CheckBox isFloat = new CheckBox(a);
        isFloat.setText(a.getString(R.string.floating_bookmark));


        layout.addView(editText);
        layout.addView(isFloat);

        builder.setView(layout);

        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                try {
                    final String text = editText.getText().toString();
                    if (text != null && !text.trim().equals("")) {
                        final AppBookmark bookmark = new AppBookmark(controller.getCurrentBook().getPath(), text, controller.getPercentage());
                        bookmark.isF = isFloat.isChecked();
                        BookmarksData.get().add(bookmark);
                        if (objects != null) {
                            objects.add(0, bookmark);
                        }
                        if (bookmark != null) {
                            bookmarksAdapter.notifyDataSetChanged();
                        }

                    }
                } catch (final Exception e) {
                    // not important
                }
                Keyboards.close(editText);
                Keyboards.hideNavigation(controller.getActivity());

                if (onRefresh != null) {
                    onRefresh.run();
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                Keyboards.close(editText);
                Keyboards.hideNavigation(controller.getActivity());

                dialog.dismiss();
            }
        });

        builder.show();
    }

    public static void addBookmark(DocumentController controller, String text) {
        final AppBookmark bookmark = new AppBookmark(controller.getCurrentBook().getPath(), text, controller.getPercentage());
        BookmarksData.get().add(bookmark);
    }

    public static void showEditDeleteDialog(final AppBookmark bookmark, DocumentController controller, final BookmarksAdapter bookmarksAdapter, final List<AppBookmark> objects, Runnable onRefresh) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(controller.getActivity());
        builder.setTitle(controller.getActivity().getString(R.string.bookmark_on_page_) + " " + controller.getCurentPage());


        LinearLayout layout = new LinearLayout(controller.getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText editText = new EditText(controller.getActivity());
        editText.setLines(6);
        editText.setGravity(Gravity.TOP);
        editText.setText(bookmark.getText());
        editText.requestFocus();


        CheckBox isFloat = new CheckBox(controller.getActivity());
        isFloat.setText(controller.getString(R.string.floating_bookmark));
        isFloat.setChecked(bookmark.isF);

        layout.addView(editText);
        layout.addView(isFloat);

        builder.setView(layout);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                try {
                    final String text = editText.getText().toString();
                    if (text != null && !text.trim().equals("")) {
                        bookmark.text = text;
                        //BookmarksData.get().remove(bookmark);
                        bookmark.isF = isFloat.isChecked();
                        BookmarksData.get().add(bookmark);

                        bookmarksAdapter.notifyDataSetChanged();
                        Keyboards.close(editText);

                        if(onRefresh!=null){
                            onRefresh.run();
                        }

                    }
                } catch (final Exception e) {
                }

            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                Keyboards.close(editText);
                dialog.dismiss();
            }
        });
        builder.setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                Keyboards.close(editText);
                BookmarksData.get().remove(bookmark);
                objects.remove(bookmark);
                bookmarksAdapter.notifyDataSetChanged();

                if(onRefresh!=null){
                    onRefresh.run();
                }

                dialog.dismiss();
            }
        });

        builder.show();
    }

}
