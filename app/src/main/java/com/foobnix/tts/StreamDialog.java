package com.foobnix.tts;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.foobnix.android.utils.Dips;
import com.foobnix.model.AppProfile;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.BookCSS;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The internet radio to choose from: every source, and under a playlist all the stations it holds.
 * A station is played with a tap; a source is added by its address and taken away with its cross.
 */
public final class StreamDialog {

    public interface OnPick {
        void onPick(StreamPlaylist.Entry entry);
    }

    private static final Handler handler = new Handler(Looper.getMainLooper());

    private final Context context;
    private final OnPick onPick;
    private final List<Object> rows = new ArrayList<Object>();
    private final Adapter adapter = new Adapter();
    private String filter = "";
    private AlertDialog dialog;

    private StreamDialog(Context context, OnPick onPick) {
        this.context = context;
        this.onPick = onPick;
    }

    public static void show(Context context, OnPick onPick) {
        new StreamDialog(context, onPick).show();
    }

    private void show() {
        final LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(Dips.DP_10, Dips.DP_4, Dips.DP_10, 0);

        final EditText search = new EditText(context);
        search.setHint(R.string.filter);
        search.setSingleLine();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override public void afterTextChanged(Editable s) {
                filter = s.toString().trim().toLowerCase(Locale.getDefault());
                refresh();
            }
        });
        layout.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                             ViewGroup.LayoutParams.WRAP_CONTENT));

        final ListView list = new ListView(context);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            final Object row = rows.get(position);
            if (row instanceof StreamPlaylist.Entry) {
                onPick.onPick((StreamPlaylist.Entry) row);
                adapter.notifyDataSetChanged();
            }
        });
        layout.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Dips.dpToPx(360)));

        dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.play_stream)
                .setView(layout)
                .setPositiveButton(R.string.add, null)
                .setNegativeButton(R.string.close, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> askAddress()));
        dialog.show();

        refresh();
        load();
    }

    /** Reads the sources not read yet, one after another, the list growing as each comes in. */
    private void load() {
        final List<String> sources = StreamPlaylist.sources();
        new Thread(() -> {
            for (String source : sources) {
                if (!StreamPlaylist.isLoaded(source)) {
                    StreamPlaylist.entries(source);
                    handler.post(this::refresh);
                }
            }
        }, "@T streams").start();
    }

    private void refresh() {
        rows.clear();
        for (String source : StreamPlaylist.sources()) {
            rows.add(source);
            if (!StreamPlaylist.isLoaded(source)) {
                continue;
            }
            for (StreamPlaylist.Entry entry : StreamPlaylist.entries(source)) {
                if (filter.length() == 0 || entry.title.toLowerCase(Locale.getDefault()).contains(filter)) {
                    rows.add(entry);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void askAddress() {
        final EditText input = new EditText(context);
        input.setHint("https://");
        input.setSingleLine();
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        final LinearLayout box = new LinearLayout(context);
        box.setPadding(Dips.DP_20, Dips.DP_10, Dips.DP_20, 0);
        box.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                         ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(context)
                .setTitle(R.string.stream_address)
                .setView(box)
                .setPositiveButton(R.string.add, (d, which) -> {
                    if (StreamPlaylist.add(input.getText().toString())) {
                        AppProfile.save(context);
                        refresh();
                        load();
                    } else {
                        Toast.makeText(context, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Takes a source away, once asked: one tap on its cross must not lose a list of stations. */
    private void removeSource(final String source) {
        new AlertDialog.Builder(context)
                .setMessage(context.getString(R.string.remove) + ": " + StreamPlaylist.name(source) + "?")
                .setPositiveButton(R.string.remove, (d, which) -> {
                    StreamPlaylist.remove(source);
                    AppProfile.save(context);
                    refresh();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private final class Adapter extends BaseAdapter {

        @Override public int getCount() {
            return rows.size();
        }

        @Override public Object getItem(int position) {
            return rows.get(position);
        }

        @Override public long getItemId(int position) {
            return position;
        }

        @Override public int getViewTypeCount() {
            return 2;
        }

        @Override public int getItemViewType(int position) {
            return rows.get(position) instanceof String ? 0 : 1;
        }

        @Override public boolean isEnabled(int position) {
            return rows.get(position) instanceof StreamPlaylist.Entry;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            final Object row = rows.get(position);
            if (row instanceof String) {
                return header((String) row, convertView);
            }
            return entry((StreamPlaylist.Entry) row, convertView);
        }

        private View header(final String source, View convertView) {
            LinearLayout line = (LinearLayout) convertView;
            if (line == null) {
                line = new LinearLayout(context);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setGravity(Gravity.CENTER_VERTICAL);
                line.setPadding(0, Dips.DP_10, 0, Dips.DP_4);
                final TextView name = new TextView(context);
                name.setTypeface(Typeface.DEFAULT_BOLD);
                name.setSingleLine();
                name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                line.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                final ImageView delete = new ImageView(context);
                delete.setImageResource(R.drawable.glyphicons_599_menu_close);
                delete.setPadding(Dips.DP_8, Dips.DP_8, Dips.DP_8, Dips.DP_8);
                delete.setContentDescription(context.getString(R.string.remove));
                TintUtil.setTintImageWithAlpha(delete, TintUtil.color);
                line.addView(delete, new LinearLayout.LayoutParams(Dips.dpToPx(36), Dips.dpToPx(36)));
            }
            final TextView name = (TextView) line.getChildAt(0);
            final int count = StreamPlaylist.isLoaded(source) ? StreamPlaylist.entries(source).size() : -1;
            name.setText(StreamPlaylist.name(source) + (count < 0 ? "  …" : "  (" + count + ")"));
            line.getChildAt(1).setOnClickListener(v -> removeSource(source));
            return line;
        }

        private View entry(StreamPlaylist.Entry entry, View convertView) {
            TextView text = (TextView) convertView;
            if (text == null) {
                text = new TextView(context);
                text.setSingleLine();
                text.setEllipsize(TextUtils.TruncateAt.END);
                text.setPadding(Dips.DP_10, Dips.DP_8, 0, Dips.DP_8);
                text.setTextSize(16);
            }
            final boolean playing = entry.url.equals(BookCSS.get().mp3BookPathGet());
            text.setText((playing ? "▶ " : "") + entry.title);
            text.setTypeface(playing ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            return text;
        }
    }
}
