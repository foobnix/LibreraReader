package com.foobnix.pdf.info.view;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.greenrobot.eventbus.EventBus;

import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.Playlists;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.search.activity.msg.UpdateAllFragments;
import com.foobnix.sys.TempHolder;
import com.jmedeisis.draglinearlayout.DragLinearLayout;
import com.jmedeisis.draglinearlayout.DragLinearLayout.OnViewSwapListener;

import android.annotation.TargetApi;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DialogsPlaylist {
    private static final int SIZE = Dips.isLargeOrXLargeScreen() ? Dips.DP_80 : Dips.DP_60;
    static AlertDialog create;

    public static void showPlaylistsDialog(final Context a, final Runnable refresh, final File file) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);

        if (file != null) {
            builder.setTitle(R.string.add_to_playlist);
        }

        View inflate = LayoutInflater.from(a).inflate(R.layout.dialog_tags, null, false);

        final ListView list = (ListView) inflate.findViewById(R.id.listView1);
        final TextView add = (TextView) inflate.findViewById(R.id.addTag);
        TxtUtils.underline(add, a.getString(R.string.create_playlist));

        final List<String> items = Playlists.getAllPlaylists();

        final BaseItemLayoutAdapter<String> adapter = new BaseItemLayoutAdapter<String>(a, R.layout.tag_item_text, items) {
            @Override
            public void populateView(View layout, final int position, final String tagName) {
                TextView text = (TextView) layout.findViewById(R.id.text1);
                text.setText(Playlists.formatPlaylistName(tagName));
                if (file == null) {
                    TxtUtils.underlineTextView(text);
                }

                layout.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (file != null) {
                            Playlists.addMetaToPlaylist(tagName, file);
                            create.dismiss();
                        } else {
                            showPlayList(a, tagName);
                        }
                    }
                });

                ImageView img = layout.findViewById(R.id.delete1);
                if (file != null) {
                    img.setVisibility(View.GONE);
                }
                TintUtil.setTintImageWithAlpha(img);
                img.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AlertDialogs.showOkDialog((Activity) a, a.getString(R.string.are_you_sure_to_delete_playlist_), new Runnable() {

                            @Override
                            public void run() {
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

            @Override
            public void onClick(View v) {
                addPlaylistDialog(a, new Runnable() {

                    @Override
                    public void run() {
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

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        create = builder.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
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

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Keyboards.close(edit);
            }
        });

        builder.setPositiveButton(R.string.add, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Keyboards.close(edit);
            }
        });

        final AlertDialog create = builder.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
            }
        });
        create.show();

        create.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String text = edit.getText().toString().trim();

                if (TxtUtils.isEmpty(text)) {
                    Toast.makeText(a, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
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


    public static void showPlayList(final Context a, final String file) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(Playlists.formatPlaylistName(file));

        final DragLinearLayout layout = new DragLinearLayout(a);
        layout.setOrientation(LinearLayout.VERTICAL);

        List<String> res = Playlists.getPlaylistItems(file);

        final Runnable update = new Runnable() {

            @Override
            public void run() {
                List<String> list = new ArrayList<String>();
                for (int i = 0; i < layout.getChildCount(); i++) {
                    View child = layout.getChildAt(i);
                    list.add((String) child.getTag());
                }
                Playlists.updatePlaylist(file, list);

                EventBus.getDefault().post(new UpdateAllFragments());
            }
        };

        for (final String s : res) {

            final View library = LayoutInflater.from(a).inflate(R.layout.item_tab_line, null, false);
            library.setTag(s);

            ((TextView) library.findViewById(R.id.text1)).setText(" " + new File(s).getName());
            ((CheckBox) library.findViewById(R.id.isVisible)).setVisibility(View.GONE);
            ImageView img = ((ImageView) library.findViewById(R.id.image1));

            ImageView imgDrag = ((ImageView) library.findViewById(R.id.imageDrag));
            imgDrag.setImageResource(R.drawable.glyphicons_208_remove_2);
            TintUtil.setTintImageWithAlpha(imgDrag);

            imgDrag.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    layout.removeView(library);
                }
            });

            int size = SIZE;
            img.getLayoutParams().width = size;
            img.getLayoutParams().height = (int) (size * IMG.WIDTH_DK);
            IMG.getCoverPageWithEffect(img, s, size, null);
            layout.addView(library);

            library.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    update.run();
                    ExtUtils.showDocument(a, Uri.fromFile(new File(s)), -1, file);
                }
            });

        }

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            layout.setViewDraggable(child, child);
        }



        builder.setView(layout);

        builder.setNegativeButton(R.string.close, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                update.run();
            }
        });
        if (res.size() > 0) {
            builder.setPositiveButton(R.string.play, new AlertDialog.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    update.run();

                    FileMeta f = new FileMeta(Playlists.getFile(file).getPath());
                    ExtUtils.openFile((Activity) a, f);

                }
            });
        }
        builder.show();
    }



    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void dispalyPlaylist(final Activity a, final DocumentController dc) {
        final DragLinearLayout playlist = (DragLinearLayout) a.findViewById(R.id.playlist);
        final TextView playListName = (TextView) a.findViewById(R.id.playListName);
        playListName.setVisibility(View.GONE);

        final String palylistPath = a.getIntent().getStringExtra(DocumentController.EXTRA_PLAYLIST);
        if (TxtUtils.isEmpty(palylistPath) && dc != null && !dc.isMusicianMode()) {
            playlist.setVisibility(View.GONE);
            return;
        }
        playListName.setVisibility(View.VISIBLE);
        playlist.removeAllViews();

        List<String> playlistItems = Playlists.getPlaylistItems(palylistPath);
        if (TxtUtils.isNotEmpty(palylistPath)) {
            playListName.setText(Playlists.formatPlaylistName(palylistPath));
        }

        playListName.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final List<String> items = Playlists.getAllPlaylists();

                MyPopupMenu menu = new MyPopupMenu(v);
                for (final String item : items) {
                    menu.getMenu().add(Playlists.formatPlaylistName(item)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem m) {
                            a.getIntent().putExtra(DocumentController.EXTRA_PLAYLIST, item);
                            dispalyPlaylist(a, dc);
                            return false;
                        }
                    });
                }

                menu.getMenu().add(R.string.playlists).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        showPlaylistsDialog(a, new Runnable() {

                            @Override
                            public void run() {
                                dispalyPlaylist(a, dc);
                            }
                        }, null);
                        return false;
                    }
                });

                menu.show();

            }
        });

        for (final String s : playlistItems) {
            UnderlineImageView img = new UnderlineImageView(a, null);
            img.setTag(s);
            img.setUnderlineValue(Dips.DP_3);
            img.setLeftPadding(false);
            img.setBackgroundResource(R.drawable.bg_clickable);



            int size = SIZE;
            img.setLayoutParams(new LayoutParams(size, (int) (size * IMG.WIDTH_DK)));
            img.setPadding(Dips.DP_3, 0, Dips.DP_5, Dips.DP_10);
            if (dc != null && dc.getCurrentBook() != null && s.equals(dc.getCurrentBook().getPath())) {
                // img.setBackgroundColor(Color.YELLOW);
                img.underline(true);
            }

            IMG.getCoverPageWithEffect(img, s, size, null);
            if (Build.VERSION.SDK_INT >= 16) {
                img.setCropToPadding(true);
            }
            img.setAdjustViewBounds(true);
            img.setScaleType(ScaleType.CENTER_CROP);
            playlist.addView(img);

            img.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (dc != null && dc.getCurrentBook() != null && !dc.getCurrentBook().getPath().equals(s)) {

                        dc.onCloseActivityFinal(new Runnable() {

                            @Override
                            public void run() {
                                ExtUtils.showDocumentWithoutDialog(a, new File(s), -1, palylistPath);
                            }
                        });
                    }
                }
            });

            playlist.setViewDraggable(img, img);
            playlist.setOnViewSwapListener(new OnViewSwapListener() {

                @Override
                public void onSwapFinish() {
                    List<String> list = new ArrayList<String>();
                    for (int i = 0; i < playlist.getChildCount(); i++) {
                        View child = playlist.getChildAt(i);
                        list.add((String) child.getTag());
                    }
                    Playlists.updatePlaylist(palylistPath, list);
                }

                @Override
                public void onSwap(View draggedView, int initPosition, View swappedView, int swappedPosition) {
                }
            });

        }

    }

}
