package com.foobnix.pdf.info.view;

import static com.foobnix.pdf.info.Playlists.L_PLAYLIST_CURRENT_FOLDER;
import static com.foobnix.pdf.info.Playlists.L_PLAYLIST_FAVORITES;
import static com.foobnix.pdf.info.Playlists.L_PLAYLIST_FOLDER;
import static com.foobnix.pdf.info.Playlists.L_PLAYLIST_RECENT;
import static com.foobnix.pdf.info.Playlists.L_PLAYLIST_TAGS;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.text.TextUtils;
import androidx.core.graphics.ColorUtils;
import android.util.TypedValue;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.HorizontalScrollView;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.foobnix.LibreraApp;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppData;
import com.foobnix.model.AppState;
import com.foobnix.model.TagData;
import com.foobnix.model.Tags2;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.Playlists;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.DocumentWrapperUI;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.pdf.info.io.SearchCore;
import com.foobnix.pdf.info.view.drag.OnStartDragListener;
import com.foobnix.pdf.info.view.drag.PlaylistAdapter;
import com.foobnix.pdf.info.view.drag.SimpleItemTouchHelperCallback;
import com.foobnix.pdf.info.widget.ShareDialog;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.search.activity.msg.UpdateAllFragments;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AppDB;
import com.foobnix.tts.TTSControlsView;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import com.foobnix.ui2.fragment.BrowseFragment2;
import com.foobnix.ui2.fragment.FavoritesFragment2;
import com.jmedeisis.draglinearlayout.DragLinearLayout;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.PorterDuffColorFilter;

public class DialogsPlaylist {
    public static final int LIMIT_MAX_BOOKS = 50;
    static AlertDialog create;

    public static void showPlaylistsDialog(final Context a, final Runnable refresh, final File file) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);

        if (file != null) {
            builder.setTitle(R.string.add_to_playlist);
        }

        View inflate = LayoutInflater.from(a)
                                     .inflate(R.layout.dialog_tags, null, false);

        final ListView list = (ListView) inflate.findViewById(R.id.listView1);
        final TextView add = (TextView) inflate.findViewById(R.id.addTag);
        TxtUtils.underline(add, a.getString(R.string.create_playlist));
        // Drawn as the settings panel draws its buttons: a ring in the colour of the word.
        TintUtil.asLinkButton(add);

        final List<String> items = Playlists.getAllPlaylists();

        final BaseItemLayoutAdapter<String> adapter =
                new BaseItemLayoutAdapter<String>(a, R.layout.tag_item_text, items) {
                    @Override public void populateView(View layout, final int position, final String tagName) {
                        TextView text = (TextView) layout.findViewById(R.id.text1);
                        text.setText(Playlists.formatPlaylistName(a, tagName));
                        if (file == null) {
                            TxtUtils.underlineTextView(text);
                        }

                        layout.setOnClickListener(new OnClickListener() {

                            @Override public void onClick(View v) {
                                if (file != null) {
                                    Playlists.addMetaToPlaylist(tagName, file);
                                    create.dismiss();
                                } else {
                                    showPlayList(a, tagName, null);
                                }
                            }
                        });

                        ImageView img = layout.findViewById(R.id.delete1);
                        if (file != null) {
                            img.setVisibility(View.GONE);
                        }
                        // The cross in the colour links are drawn in.
                        final int deleteColor = TxtUtils.getLinkTextColor(a);
                        TintUtil.setTintImageNoAlpha(img, deleteColor);
                        img.setOnClickListener(new OnClickListener() {

                            @Override public void onClick(View v) {
                                AlertDialogs.showOkDialog((Activity) a,
                                        a.getString(R.string.are_you_sure_to_delete_playlist_), new Runnable() {

                                            @Override public void run() {
                                                Playlists.deletePlaylist(tagName);
                                                items.clear();
                                                items.addAll(Playlists.getAllPlaylists());
                                                notifyDataSetChanged();

                                                if (refresh != null) {
                                                    refresh.run();
                                                }

                                            }
                                        });

                            }
                        });

                    }
                };

        add.setOnClickListener(new OnClickListener() {

            @Override public void onClick(View v) {
                addPlaylistDialog(a, new Runnable() {

                    @Override public void run() {
                        items.clear();
                        items.addAll(Playlists.getAllPlaylists());
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });

        list.setAdapter(adapter);

        builder.setView(inflate);

        builder.setNegativeButton(R.string.close, new AlertDialog.OnClickListener() {

            @Override public void onClick(DialogInterface dialog, int which) {

            }
        });

        create = builder.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override public void onDismiss(DialogInterface dialog) {
                create = null;
                if (refresh != null) {
                    refresh.run();
                }
                TempHolder.listHash++;
                Keyboards.close((Activity) a);
                Keyboards.hideNavigation((Activity) a);

            }
        });
        create.show();

    }

    public static void addPlaylistDialog(final Context a, final Runnable onRefresh) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.create_playlist);

        final EditText edit = new EditText(a);

        builder.setView(edit);

        builder.setNegativeButton(R.string.cancel, new AlertDialog.OnClickListener() {

            @Override public void onClick(DialogInterface dialog, int which) {
                Keyboards.close(edit);
            }
        });

        builder.setPositiveButton(R.string.add, new AlertDialog.OnClickListener() {

            @Override public void onClick(DialogInterface dialog, int which) {
                Keyboards.close(edit);
            }
        });

        final AlertDialog create = builder.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override public void onDismiss(DialogInterface dialog) {
            }
        });
        create.show();

        create.getButton(AlertDialog.BUTTON_POSITIVE)
              .setOnClickListener(new OnClickListener() {

                  @Override public void onClick(View v) {
                      String text = edit.getText()
                                        .toString()
                                        .trim();

                      if (TxtUtils.isEmpty(text)) {
                          Toast.makeText(a, R.string.incorrect_value, Toast.LENGTH_SHORT)
                               .show();
                          return;
                      }

                      Playlists.createPlayList(text);

                      Keyboards.close(edit);
                      Keyboards.hideNavigation((Activity) a);

                      if (onRefresh != null) {
                          onRefresh.run();
                      }
                      create.dismiss();
                  }
              });
    }

    static ItemTouchHelper mItemTouchHelper;

    public static void showPlayList(final Context a, final String file, final Runnable refresh) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(Playlists.formatPlaylistName(a, file));

        RecyclerView recyclerView = new RecyclerView(a);
        //recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(a);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        final List<String> res = Playlists.getPlaylistItems(file);

        final PlaylistAdapter adapter = new PlaylistAdapter(a, res, new OnStartDragListener() {

            @Override public void onStartDrag(ViewHolder viewHolder) {
                mItemTouchHelper.startDrag(viewHolder);
            }

            @Override public void onRevemove() {
                Playlists.updatePlaylist(file, res);
            }

            @Override public void onItemLongClick(String result) {
                onItemClick(result);
            }

            @Override public void onItemClick(String result) {
                Playlists.updatePlaylist(file, res);
                EventBus.getDefault()
                        .post(new UpdateAllFragments());

                ExtUtils.showDocumentWithoutDialog2(a, Uri.fromFile(new File(result)), -1, file);
            }

        }, false);
        recyclerView.setAdapter(adapter);

        final Runnable update = new Runnable() {

            @Override public void run() {
                List<String> list = adapter.getItems();
                Playlists.updatePlaylist(file, list);
                EventBus.getDefault()
                        .post(new UpdateAllFragments());
                if (refresh != null) {
                    refresh.run();
                }
            }
        };

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);

        mItemTouchHelper.attachToRecyclerView(recyclerView);

        final DragLinearLayout layout = new DragLinearLayout(a);
        layout.setOrientation(LinearLayout.VERTICAL);

        builder.setView(recyclerView);

        builder.setNegativeButton(R.string.close, new AlertDialog.OnClickListener() {

            @Override public void onClick(DialogInterface dialog, int which) {
                update.run();
                if (refresh != null) {
                    refresh.run();
                }
            }
        });
        if (res.size() > 0 && refresh == null) {
            builder.setPositiveButton(R.string.play, new AlertDialog.OnClickListener() {

                @Override public void onClick(DialogInterface dialog, int which) {
                    LOG.d("play-click", file);
                    update.run();

                    FileMeta f = new FileMeta(Playlists.getFile(file)
                                                       .getPath());
                    LOG.d("play-click meta", f.getPath());

                    ExtUtils.openFile((Activity) a, f);

                }
            });
        }
        builder.show();
    }

    public static List<String> convert(List<FileMeta> list, int limit) {

        List<String> res = new ArrayList<>();
        int count = 0;
        for (FileMeta meta : list) {
            if (meta.getCusType() != null && meta.getCusType()
                                                 .equals(FileMetaAdapter.DISPLAY_TYPE_DIRECTORY)) {
                continue;
            }
            res.add(meta.getPath());
            if (count > limit) {
                break;
            }

        }
        return res;
    }

    /** The lists the strip of covers can show: recent, favourites, folders, playlists and tags. */
    private static List<String> playlistChoices() {
        final List<String> items = new ArrayList<>();
        items.add(L_PLAYLIST_RECENT);
        items.add(L_PLAYLIST_FAVORITES);
        items.add(L_PLAYLIST_CURRENT_FOLDER);
        for (FileMeta folder : AppData.get().getAllFavoriteFolders()) {
            items.add(L_PLAYLIST_FOLDER + folder.getPath());
        }
        items.addAll(Playlists.getAllPlaylists());
        for (String tag : Tags2.getAllTags()) {
            if (TxtUtils.isNotEmpty(tag)) {
                items.add(L_PLAYLIST_TAGS + tag);
            }
        }
        return items;
    }

    /** A list's name on its tab, short: the folder's own name, the playlist's without its count. */
    private static String tabName(Activity a, String item) {
        if (item.startsWith(L_PLAYLIST_RECENT) || item.startsWith(L_PLAYLIST_FAVORITES)) {
            return Playlists.formatPlaylistName(a, item);
        }
        if (item.startsWith(L_PLAYLIST_CURRENT_FOLDER)) {
            return ExtUtils.getFileName(AppState.get().displayPath);
        }
        if (item.startsWith(L_PLAYLIST_FOLDER)) {
            return ExtUtils.getFileName(item);
        }
        if (item.startsWith(L_PLAYLIST_TAGS)) {
            return item.replace(L_PLAYLIST_TAGS, "");
        }
        return TxtUtils.firstUppercase(ExtUtils.getFileName(item).replace(Playlists.L_PLAYLIST, ""));
    }

    private static int tabIcon(String item) {
        if (item == null || item.startsWith(L_PLAYLIST_RECENT)) {
            return R.drawable.glyphicons_422_book_library;
        }
        if (item.startsWith(L_PLAYLIST_FAVORITES)) {
            return R.drawable.glyphicons_13_heart;
        }
        if (item.startsWith(L_PLAYLIST_CURRENT_FOLDER)) {
            return R.drawable.glyphicons_145_folder_open;
        }
        if (item.startsWith(L_PLAYLIST_FOLDER)) {
            return R.drawable.glyphicons_336_folder;
        }
        if (item.startsWith(L_PLAYLIST_TAGS)) {
            return R.drawable.glyphicons_66_tag;
        }
        return R.drawable.glyphicons_160_playlist;
    }

    /** How many books a list holds, as the strip would show them, before it is cut to its length. Off the main thread. */
    private static int countOf(String item) {
        if (item.equals(L_PLAYLIST_RECENT)) {
            return AppData.get().getAllRecent(false).size();
        }
        if (item.equals(L_PLAYLIST_FAVORITES)) {
            return FavoritesFragment2.getFavoritesSorted(false).size();
        }
        if (item.startsWith(L_PLAYLIST_FOLDER) || item.startsWith(L_PLAYLIST_CURRENT_FOLDER)) {
            final String path = item.startsWith(L_PLAYLIST_CURRENT_FOLDER)
                    ? AppState.get().displayPath : item.replace(L_PLAYLIST_FOLDER, "");
            final List<FileMeta> files = SearchCore.getFilesAndDirs(path, true, AppState.get().isDisplayAllFilesInFolder);
            ExtUtils.removeReadBooks(files);
            int count = 0;
            for (FileMeta meta : files) {
                if (meta.getCusType() == null || meta.getCusType() != FileMetaAdapter.DISPLAY_TYPE_DIRECTORY) {
                    count++;
                }
            }
            return count;
        }
        if (item.startsWith(L_PLAYLIST_TAGS)) {
            return Tags2.getAllFilesByTag(item.replace(L_PLAYLIST_TAGS, "")).size();
        }
        return Playlists.getPlaylistItems(item).size();
    }

    /**
     * The lists the strip can show, as tabs over it: each its mark, its name and how many books
     * it holds, the one shown filled in. A tap on one shows it.
     */
    private static void showPlaylistTabs(final Activity a, final DocumentController dc, final String current) {
        final LinearLayout tabs = a.findViewById(R.id.playListTabs);
        final HorizontalScrollView scroll = a.findViewById(R.id.playListTabsScroll);
        if (tabs == null || scroll == null) {
            return;
        }
        tabs.removeAllViews();
        final int accent = MagicHelper.getTextOrIconColor();
        final List<String> items = playlistChoices();
        final List<TextView> badges = new ArrayList<>();
        View selected = null;
        for (final String item : items) {
            final boolean chosen = item.equals(current);
            final int ink = chosen ? accent : ColorUtils.setAlphaComponent(accent, 0xB3);

            final LinearLayout tab = new LinearLayout(a);
            tab.setOrientation(LinearLayout.HORIZONTAL);
            tab.setGravity(Gravity.CENTER_VERTICAL);
            tab.setPadding(Dips.dpToPx(12), Dips.DP_6, Dips.dpToPx(12), Dips.DP_6);
            final GradientDrawable pill = new GradientDrawable();
            pill.setCornerRadius(Dips.dpToPx(50));
            pill.setColor(chosen ? ColorUtils.setAlphaComponent(accent, 0x33) : Color.TRANSPARENT);
            tab.setBackground(pill);
            tab.setContentDescription(Playlists.formatPlaylistName(a, item));

            final ImageView icon = new ImageView(a);
            icon.setImageResource(tabIcon(item));
            icon.setColorFilter(ink);
            tab.addView(icon, new LinearLayout.LayoutParams(Dips.dpToPx(18), Dips.dpToPx(18)));

            final TextView label = new TextView(a);
            label.setText(tabName(a, item));
            label.setTextColor(ink);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            label.setTypeface(null, chosen ? Typeface.BOLD : Typeface.NORMAL);
            label.setSingleLine(true);
            final LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelLp.leftMargin = Dips.DP_6;
            tab.addView(label, labelLp);

            // How many books, once counted
            final TextView badge = new TextView(a);
            badge.setTextColor(ink);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            badge.setPadding(Dips.DP_6, 0, Dips.DP_6, 0);
            final GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(Dips.dpToPx(50));
            badgeBg.setColor(ColorUtils.setAlphaComponent(accent, chosen ? 0x55 : 0x26));
            badge.setBackground(badgeBg);
            // Counted before, shown at once: the tabs are made again on every tap, and a count
            // coming in later widened them all one frame after
            final Integer known = TAB_COUNTS.get(item);
            if (known != null) {
                badge.setText(String.valueOf(known));
            } else {
                badge.setVisibility(View.GONE);
            }
            final LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            badgeLp.leftMargin = Dips.DP_6;
            tab.addView(badge, badgeLp);
            badges.add(badge);

            tab.setOnClickListener(v -> {
                if (chosen) {
                    return;
                }
                AppState.get().playlistDefault = item;
                a.getIntent().putExtra(DocumentController.EXTRA_PLAYLIST, item);
                dispalyPlaylist(a, dc);
            });
            final LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tabLp.rightMargin = Dips.DP_4;
            tabs.addView(tab, tabLp);
            if (chosen) {
                selected = tab;
            }
        }
        // The list shown is brought into view, the strip left where it is when it is seen whole
        final View shown = selected;
        if (shown != null) {
            scroll.post(() -> {
                final int from = scroll.getScrollX();
                final int to = from + scroll.getWidth();
                if (shown.getLeft() < from) {
                    scroll.smoothScrollTo(Math.max(0, shown.getLeft() - Dips.dpToPx(24)), 0);
                } else if (shown.getRight() > to) {
                    scroll.smoothScrollTo(shown.getRight() + Dips.dpToPx(24) - scroll.getWidth(), 0);
                }
            });
        }

        final Handler main = new Handler(Looper.getMainLooper());
        AppsConfig.executorServiceSingle.execute(() -> {
            for (int i = 0; i < items.size(); i++) {
                if (Apps.isDestroyedActivity(a)) {
                    return;
                }
                int count;
                try {
                    count = countOf(items.get(i));
                } catch (Exception e) {
                    LOG.e(e);
                    continue;
                }
                final Integer before = TAB_COUNTS.put(items.get(i), count);
                if (before != null && before == count) {
                    continue;
                }
                final TextView badge = badges.get(i);
                final int n = count;
                main.post(() -> {
                    badge.setText(String.valueOf(n));
                    badge.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    // How many books each tab's list held when last counted
    private static final java.util.Map<String, Integer> TAB_COUNTS = new java.util.concurrent.ConcurrentHashMap<>();

    private static final int ROW_OWN = 0, ROW_ICON = 2, ROW_ICON_OPEN = 3;

    // How tall the strip of covers last stood with covers in it, to keep when it has none
    private static int stripHeight;

    // The list the strip of covers shows, whose mark the Recent button carries
    private static String shownPlaylist = L_PLAYLIST_RECENT;
    // Its name, short as its tab has it: a folder by its own name only
    private static String shownPlaylistName = "";

    /** Turned on its side: the screen's own measure, as the reading screens keep their configuration through a turn. */
    public static boolean isLandscape() {
        return Dips.screenWidth() > Dips.screenHeight();
    }

    /**
     * The Recent button first in the row of buttons, ringed as the scroll button at the row's
     * end is: named in landscape, its mark alone upright, as that button is - the mark of the
     * list it shows. It puts the strip of covers out and away again, and is the only button
     * for that: the row it had of its own keeps the strip's tabs while the strip is out and the
     * reading controls while they are up, and otherwise folds away.
     */
    private static void keepBesideButtons(final View chipRow, final View buttons, final View ttsSlot, final View strip) {
        if (chipRow == null || !(buttons instanceof ViewGroup) || !(chipRow instanceof ViewGroup)
                || !(chipRow.getParent() instanceof View) || chipRow.getTag(R.id.footerButtons) != null) {
            return;
        }
        final TextView name = chipRow.findViewById(R.id.playListName);
        if (name == null) {
            return;
        }
        chipRow.setTag(R.id.footerButtons, ROW_OWN);
        final int paddingTop = chipRow.getPaddingTop();

        final TextView pill = buttons.findViewById(R.id.nextTypeBootom);
        final TextView button = new TextView(chipRow.getContext());
        button.setContentDescription(chipRow.getContext().getString(R.string.recent));
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setVisibility(View.GONE);
        button.setOnClickListener(v -> chipRow.performClick());
        final int pillHeight = pill != null && pill.getLayoutParams().height > 0
                ? pill.getLayoutParams().height : Dips.dpToPx(32);
        final int side = pill != null ? pill.getPaddingLeft() : Dips.DP_8;
        button.setPadding(side, 0, side, 0);
        if (pill != null) {
            button.setTextSize(TypedValue.COMPLEX_UNIT_PX, pill.getTextSize());
            button.setAllCaps(true);
        }
        final LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, pillHeight);
        buttonLp.rightMargin = Dips.DP_4;
        ((ViewGroup) buttons).addView(button, 0, buttonLp);

        // Named in landscape, its mark alone upright; in the scroll button's colours and ring.
        // Only when that changes: dressed, it asks for another layout.
        final String[] dressedFor = {null};
        final Runnable dress = () -> {
            final boolean named = isLandscape();
            final String key = named + "|" + shownPlaylistName + "|" + shownPlaylist;
            if (key.equals(dressedFor[0])) {
                return;
            }
            dressedFor[0] = key;
            // The colour the bar's icons and rings take, rather than a text colour read off a
            // button that may not be coloured yet
            final int ink = MagicHelper.getTextOrIconColor();
            button.setText(named ? shownPlaylistName : "");
            button.setTextColor(ink);
            button.setTag(R.id.pillRing, null);
            // The mark of the list the strip shows, as its tab has it
            final Drawable mark = ContextCompat.getDrawable(button.getContext(), tabIcon(shownPlaylist));
            // The scroll button's ring, as it was coloured for the theme, without its mark
            final View ringed = pill != null ? pill : name;
            Object ring = ringed.getTag(R.id.pillRing);
            if (!(ring instanceof Drawable)) {
                ring = ringed.getBackground();
            }
            if (ring instanceof Drawable && ((Drawable) ring).getConstantState() != null) {
                final Drawable copy = ((Drawable) ring).getConstantState().newDrawable().mutate();
                // The ring in the mark's colour - in paged mode the scroll button is never shown,
                // nor its ring coloured - before the mark goes in: coloured with the ring, it
                // would come out dark
                button.setBackground(copy);
                TintUtil.setRingColor(button, ink);
                button.setTag(R.id.pillRing, copy);
            }
            if (mark != null) {
                DocumentWrapperUI.pillIcon(button, mark, !named);
            }
        };

        // The panel's column, not the row's own parent: turned on its side the row stands in
        // half of another row (LandscapeSplit), which goes when the screen is upright again
        final View column = chipRow.getRootView().findViewById(R.id.footerColumn);
        final View parent = column != null ? column : (View) chipRow.getParent();
        final View toggle = chipRow.findViewById(R.id.playListToggle);
        final View chip = name.getParent() instanceof View ? (View) name.getParent() : null;
        parent.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            // The slot keeps the reading controls in it, out of sight while nothing is read
            boolean ttsShown = false;
            if (ttsSlot instanceof ViewGroup && ttsSlot.getVisibility() == View.VISIBLE) {
                for (int i = 0; i < ((ViewGroup) ttsSlot).getChildCount(); i++) {
                    ttsShown |= ((ViewGroup) ttsSlot).getChildAt(i).getVisibility() == View.VISIBLE;
                }
            }
            final boolean landscape = isLandscape();
            final CharSequence wasText = button.getText();
            dress.run();
            final boolean open = strip != null && strip.getVisibility() == View.VISIBLE;
            // The row keeps the tabs while the strip is out, and the reading controls while
            // they are up; otherwise it folds away
            final int mode = open ? ROW_ICON_OPEN : ttsShown ? ROW_OWN : ROW_ICON;
            final ViewGroup.MarginLayoutParams rowLp = (ViewGroup.MarginLayoutParams) chipRow.getLayoutParams();
            final int height = mode == ROW_ICON ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
            if (Integer.valueOf(mode).equals(chipRow.getTag(R.id.footerButtons)) && rowLp.height == height
                    && button.getVisibility() == View.VISIBLE
                    && (chip == null || chip.getVisibility() == View.GONE)
                    && (toggle == null || toggle.getVisibility() == View.GONE)
                    && Boolean.valueOf(landscape).equals(button.getTag(R.id.playListToggle))
                    && TextUtils.equals(wasText, button.getText())) {
                return;
            }
            chipRow.setTag(R.id.footerButtons, mode);
            button.setTag(R.id.playListToggle, landscape);
            // not in the middle of the layout pass that reported the sizes
            chipRow.post(() -> {
                dress.run();
                // The button among the others is the only one: the row's own and the mark
                // ahead of the tabs are never shown
                button.setVisibility(View.VISIBLE);
                if (chip != null) {
                    chip.setVisibility(View.GONE);
                }
                if (toggle != null) {
                    toggle.setVisibility(View.GONE);
                }
                chipRow.setPadding(chipRow.getPaddingLeft(), paddingTop, chipRow.getPaddingRight(), chipRow.getPaddingBottom());
                // Read again: the row may have moved in or out of the landscape split since
                final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) chipRow.getLayoutParams();
                lp.height = height;
                lp.bottomMargin = 0;
                chipRow.setLayoutParams(lp);
            });
        });
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void dispalyPlaylist(final Activity a, final DocumentController dc) {
        if(Apps.isDestroyedActivity(a) || dc ==null){
            return;
        }
        final Handler mainHandler = new Handler(Looper.getMainLooper());

        final RecyclerView playlistRecycleView = (RecyclerView) a.findViewById(R.id.playlistRecycleView);

        //playlistRecycleView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(a);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        playlistRecycleView.setLayoutManager(linearLayoutManager);

        final TextView playListName = (TextView) a.findViewById(R.id.playListName);
        final TextView playListNameEdit = (TextView) a.findViewById(R.id.playListNameEdit);
        final View playListParent = a.findViewById(R.id.playListParent);
        final ImageView closePlaylist = a.findViewById(R.id.closePlaylist);
        keepBesideButtons(playListParent, a.findViewById(R.id.footerButtons),
                          a.findViewById(R.id.ttsControlsSlot), playlistRecycleView);

        playListName.setVisibility(View.VISIBLE);

        // The strip keeps the height of its covers even when the list shown has none, so the
        // panel does not fold up under the tabs: as tall as it last stood with covers in it,
        // or as one cover is made
        final int coverHeight = (int) (IMG.getImageSize() * 0.7 * IMG.WIDTH_DK) + Dips.dpToPx(2)
                + playlistRecycleView.getPaddingTop() + playlistRecycleView.getPaddingBottom();
        playlistRecycleView.setMinimumHeight(Math.max(stripHeight, coverHeight));
        if (playlistRecycleView.getTag(R.id.playlistRecycleView) == null) {
            playlistRecycleView.setTag(R.id.playlistRecycleView, Boolean.TRUE);
            playlistRecycleView.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                final RecyclerView strip = (RecyclerView) v;
                if (strip.getAdapter() != null && strip.getAdapter().getItemCount() > 0 && b - t > stripHeight) {
                    stripHeight = b - t;
                    strip.post(() -> strip.setMinimumHeight(stripHeight));
                }
            });
        }

        playListNameEdit.setVisibility(View.GONE);
        playlistRecycleView.setVisibility(View.VISIBLE);
        playListParent.setVisibility(View.VISIBLE);

        final View tabsScroll = a.findViewById(R.id.playListTabsScroll);
        final ImageView toggle = a.findViewById(R.id.playListToggle);
        final View ttsSlot = a.findViewById(R.id.ttsControlsSlot);
        Runnable updateVisible = new Runnable() {
            @Override public void run() {
                // With the strip out its lists stand as tabs in place of the button; the reading
                // controls beside them keep only the room they need
                final boolean tabs = AppState.get().isPlayListVisible && tabsScroll != null;
                if (tabsScroll != null) {
                    tabsScroll.setVisibility(tabs ? View.VISIBLE : View.GONE);
                }
                // The Recent button stands among the buttons below; the row's own is never shown
                ((View) playListName.getParent()).setVisibility(View.GONE);
                if (ttsSlot != null && ttsSlot.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                    final LinearLayout.LayoutParams slotLp = (LinearLayout.LayoutParams) ttsSlot.getLayoutParams();
                    final int slotWidth = tabs ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
                    final float slotWeight = tabs ? 0 : 1;
                    if (slotLp.width != slotWidth || slotLp.weight != slotWeight) {
                        slotLp.width = slotWidth;
                        slotLp.weight = slotWeight;
                        ttsSlot.setLayoutParams(slotLp);
                    }
                }
                // No close of its own: the button ahead of the tabs puts the strip away
                closePlaylist.setVisibility(View.GONE);
                playlistRecycleView.setVisibility(AppState.get().isPlayListVisible ? View.VISIBLE : View.GONE);
                // The reading controls go up to a line of their own over the tabs, and back
                final List<TTSControlsView> controls = new ArrayList<>();
                for (int id : new int[]{R.id.ttsControlsSlot, R.id.ttsControlsSlotTop}) {
                    final View slot = a.findViewById(id);
                    if (slot instanceof ViewGroup) {
                        for (int i = 0; i < ((ViewGroup) slot).getChildCount(); i++) {
                            if (((ViewGroup) slot).getChildAt(i) instanceof TTSControlsView) {
                                controls.add((TTSControlsView) ((ViewGroup) slot).getChildAt(i));
                            }
                        }
                    }
                }
                for (TTSControlsView control : controls) {
                    control.post(control::standInPanel);
                }
            }
        };
        closePlaylist.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                AppState.get().isPlayListVisible = false;
                updateVisible.run();
            }
        });
        updateVisible.run();
        playListParent.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                AppState.get().isPlayListVisible = !AppState.get().isPlayListVisible;
                updateVisible.run();
            }
        });

        //a.getIntent().putExtra(DocumentController.EXTRA_PLAYLIST,dc.getString(R.string.recent));

        String palylistPathCheck = a.getIntent()
                                    .getStringExtra(DocumentController.EXTRA_PLAYLIST);
        if (TxtUtils.isEmpty(palylistPathCheck)) {
            palylistPathCheck = AppState.get().playlistDefault;
        }
        final String displayPath = palylistPathCheck;
        shownPlaylist = displayPath;
        shownPlaylistName = tabName(a, displayPath);
        if (toggle != null) {
            toggle.setImageResource(tabIcon(displayPath));
        }
        showPlaylistTabs(a, dc, displayPath);

        final List<String> res = new ArrayList<String>();

        AppsConfig.executorServiceSingle.execute(() -> {

            if (Apps.isDestroyedActivity(a)) {
                return;
            }
            LOG.d("getFilesAndDirs", "init", displayPath);
            if (displayPath.equals(L_PLAYLIST_RECENT)) {
                res.addAll(convert(AppData.get()
                                          .getAllRecent(false), LIMIT_MAX_BOOKS));
            } else if (displayPath.equals(L_PLAYLIST_FAVORITES)) {
                res.addAll(convert(FavoritesFragment2.getFavoritesSorted(false), LIMIT_MAX_BOOKS));
            } else if (displayPath.startsWith(L_PLAYLIST_FOLDER) || displayPath.startsWith(
                    L_PLAYLIST_CURRENT_FOLDER)) {
                String showPath = "";
                if (displayPath.startsWith(L_PLAYLIST_CURRENT_FOLDER)) {
                    showPath = AppState.get().displayPath;
                } else {
                    showPath = displayPath.replace(L_PLAYLIST_FOLDER, "");
                }

                List<FileMeta> filesAndDirs =
                        SearchCore.getFilesAndDirs(showPath, true, AppState.get().isDisplayAllFilesInFolder);
                ExtUtils.removeReadBooks(filesAndDirs);
                BrowseFragment2.sortItems(filesAndDirs);
                res.addAll(convert(filesAndDirs, LIMIT_MAX_BOOKS));

            } else if (displayPath.startsWith(L_PLAYLIST_TAGS)) {
//                List<FileMeta> allTags = AppDB.get()
//                                              .searchBy("@tags " + displayPath.replace(L_PLAYLIST_TAGS, ""),
//                                                      AppDB.SORT_BY.FILE_NAME, false);
                //Tags2.getAllTags()
                String tag = displayPath.replace(L_PLAYLIST_TAGS, "");

                res.addAll(Tags2.getAllFilesByTag(tag));
                Collections.reverse(res);
            } else {
                mainHandler.post(() -> {
                            playListNameEdit.setVisibility(View.VISIBLE);
                        });
                res.addAll(Playlists.getPlaylistItems(displayPath));
            }

            if (Apps.isDestroyedActivity(a)) {
                return;
            }

            mainHandler.post(() -> {

                if (Apps.isDestroyedActivity(a)) {
                    return;
                }

                final PlaylistAdapter adapter = new PlaylistAdapter(a, res, new OnStartDragListener() {

                    @Override public void onStartDrag(ViewHolder viewHolder) {
                    }

                    @Override public void onRevemove() {
                    }

                    @Override public void onItemLongClick(String result) {
                        ShareDialog.show(dc.getActivity(), new File(result), new Runnable() {
                            @Override public void run() {

                            }
                        }, 0, dc, new Runnable() {
                            @Override public void run() {

                            }
                        });
                    }

                    @Override public void onItemClick(final String s) {
                        LOG.d("onItemClick", s);
                        if (dc != null && dc.getCurrentBook() != null && !dc.getCurrentBook()
                                                                            .getPath()
                                                                            .equals(s)) {

                            dc.closeWithAnnotationsSaved(new Runnable() {

                                @Override public void run() {
                                    ExtUtils.showDocumentWithoutDialog(a, new File(s), displayPath);
                                }
                            });
                        }

                    }

                }, true);
                if (dc != null && dc.getCurrentBook() != null) {
                    String path = dc.getCurrentBook()
                                    .getPath();
                    adapter.setCurrentPath(path);
                    int indexOf = res.indexOf(path);
                    if (indexOf > 3) {
                        playlistRecycleView.scrollToPosition(indexOf - 3);
                    }

                }
                playlistRecycleView.setAdapter(adapter);

                playListNameEdit.setOnClickListener(new OnClickListener() {

                    @Override public void onClick(View v) {
                        showPlayList(a, displayPath, new Runnable() {
                            @Override public void run() {
                                adapter.getItems()
                                       .clear();
                                adapter.getItems()
                                       .addAll(Playlists.getPlaylistItems(displayPath));
                                adapter.notifyDataSetChanged();

                            }
                        });
                    }
                });

            });

        });

        // The mark and the name are one button: a ring around both, with the mark drawn inside
        // it rather than set in the text as a character.
        // Short, as its tab names it: a folder by its own name only
        playListName.setText(tabName(a, displayPath));
        playListName.setContentDescription(Playlists.formatPlaylistName(a, displayPath));
        int playlistAccent = MagicHelper.getTextOrIconColor();
        playListName.setTextColor(playlistAccent);
        TintUtil.asLinkButton(playListName);
        Drawable playlistMark = ContextCompat.getDrawable(a, tabIcon(displayPath));
        if (playlistMark != null) {
            int markSize = (int) playListName.getTextSize();
            playlistMark.setBounds(0, 0, markSize, markSize);
            playlistMark.setColorFilter(new PorterDuffColorFilter(playlistAccent, PorterDuff.Mode.SRC_IN));
            playListName.setCompoundDrawables(playlistMark, null, null, null);
            playListName.setCompoundDrawablePadding(Dips.DP_6);
        }
        TxtUtils.updateAllLinks((ViewGroup) playListNameEdit.getParent());
        if (toggle != null) {
            // In the chip's ring, as it was coloured for the theme, and its colour
            toggle.setColorFilter(playlistAccent);
            if (playListName.getBackground() != null && playListName.getBackground().getConstantState() != null) {
                toggle.setBackground(playListName.getBackground().getConstantState().newDrawable().mutate());
            }
            toggle.setOnClickListener(v -> playListParent.performClick());
        }

        View.OnClickListener onChoosePlaylist = v -> {
            if (!AppState.get().isPlayListVisible) {
                AppState.get().isPlayListVisible = true;
                updateVisible.run();
                return;
            }

            final List<String> items = playlistChoices();

            MyPopupMenu menu = new MyPopupMenu(v);
            for (final String item : items) {
                menu.getMenu()
                    .add(Playlists.formatPlaylistName(a, item))
                    .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override public boolean onMenuItemClick(MenuItem m) {
                            AppState.get().playlistDefault = item;
                            a.getIntent()
                             .putExtra(DocumentController.EXTRA_PLAYLIST, item);
                            dispalyPlaylist(a, dc);
                            return false;
                        }
                    });
            }

            if (false) {
                menu.getMenu()
                    .add(R.string.playlists)
                    .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override public boolean onMenuItemClick(MenuItem item) {
                            showPlaylistsDialog(a, new Runnable() {

                                @Override public void run() {
                                    dispalyPlaylist(a, dc);
                                }
                            }, null);
                            return false;
                        }
                    });
            }

            menu.show();

        };

        playListName.setOnClickListener(onChoosePlaylist);

    }

}
