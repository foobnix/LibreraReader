package com.foobnix.pdf.info.wrapper;

import java.util.List;

import com.foobnix.android.utils.Keyboards;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.presentation.BookmarksAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;

public class ListBoxHelper {

    public static void showAddDialog(final DocumentController controller, final List<AppBookmark> objects, final BookmarksAdapter bookmarksAdapter, String text) {
        final Activity a = controller.getActivity();

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        final int curentPageFirst1 = PageUrl.fakeToReal(controller.getCurentPageFirst1());

        if (AppState.get().isCut) {
            builder.setTitle(a.getString(R.string.bookmark_on_page_) + " " + curentPageFirst1 + " (" + controller.getCurentPageFirst1() + ")");
        } else {
            builder.setTitle(a.getString(R.string.bookmark_on_page_) + " " + curentPageFirst1);
        }

        final EditText editText = new EditText(a);
        editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setHorizontallyScrolling(false);
        editText.setLines(6);
        editText.setGravity(Gravity.TOP);
        editText.setText(text);
        editText.requestFocus();

        builder.setView(editText);

        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                try {
                    final String text = editText.getText().toString();
                    if (text != null && !text.trim().equals("")) {
                        final AppBookmark bookmark = new AppBookmark(controller.getCurrentBook().getPath(), text, curentPageFirst1, controller.getTitle());
                        AppSharedPreferences.get().addBookMark(bookmark);
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
        final AppBookmark bookmark = new AppBookmark(controller.getCurrentBook().getPath(), text, controller.getCurentPage(), controller.getTitle());
        AppSharedPreferences.get().addBookMark(bookmark);
    }

    public static void showEditDeleteDialog(final AppBookmark bookmark, DocumentController controller, final BookmarksAdapter bookmarksAdapter, final List<AppBookmark> objects) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(controller.getActivity());
        builder.setTitle(controller.getActivity().getString(R.string.bookmark_on_page_) + " " + controller.getCurentPage());
        final EditText editText = new EditText(controller.getActivity());
        editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setHorizontallyScrolling(false);
        editText.setLines(6);
        editText.setGravity(Gravity.TOP);
        editText.setText(bookmark.getText());
        editText.requestFocus();

        builder.setView(editText);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                try {
                    final String text = editText.getText().toString();
                    if (text != null && !text.trim().equals("")) {
                        bookmark.setText(text);
                        AppSharedPreferences.get().removeBookmark(bookmark);
                        AppSharedPreferences.get().addBookMark(bookmark);

                        bookmarksAdapter.notifyDataSetChanged();
                        Keyboards.close(editText);
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
                AppSharedPreferences.get().removeBookmark(bookmark);
                objects.remove(bookmark);
                bookmarksAdapter.notifyDataSetChanged();

                dialog.dismiss();
            }
        });

        builder.show();
    }

}
