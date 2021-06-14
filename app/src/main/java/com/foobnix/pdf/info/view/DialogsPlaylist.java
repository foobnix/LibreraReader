package com.foobnix.pdf.info.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
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

import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.Playlists;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.view.drag.OnStartDragListener;
import com.foobnix.pdf.info.view.drag.PlaylistAdapter;
import com.foobnix.pdf.info.view.drag.SimpleItemTouchHelperCallback;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.search.activity.msg.UpdateAllFragments;
import com.foobnix.sys.TempHolder;
import com.jmedeisis.draglinearlayout.DragLinearLayout;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.List;

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
                            showPlayList(a, tagName, null);
                        }
                    }
                });

                ImageView img = layout.findViewById(R.id.delete1);
                if (file != null) {
                    img.setVisibility(View.GONE);
                }
                TintUtil.setTintImageWithAlpha(img, Color.GRAY);
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

    static ItemTouchHelper mItemTouchHelper;

    public static void showPlayList(final Context a, final String file, final Runnable refresh) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(Playlists.formatPlaylistName(file));

        RecyclerView recyclerView = new RecyclerView(a);
        //recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(a);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        final List<String> res = Playlists.getPlaylistItems(file);

        final PlaylistAdapter adapter = new PlaylistAdapter(a, res, new OnStartDragListener() {

            @Override
            public void onStartDrag(ViewHolder viewHolder) {
                mItemTouchHelper.startDrag(viewHolder);
            }

            @Override
            public void onRevemove() {
                Playlists.updatePlaylist(file, res);
            }

            @Override
            public void onItemClick(String result) {
                Playlists.updatePlaylist(file, res);
                EventBus.getDefault().post(new UpdateAllFragments());

                ExtUtils.showDocumentWithoutDialog2(a, Uri.fromFile(new File(result)), -1, file);
            }

        }, false);
        recyclerView.setAdapter(adapter);

        final Runnable update = new Runnable() {

            @Override
            public void run() {
                List<String> list = adapter.getItems();
                Playlists.updatePlaylist(file, list);
                EventBus.getDefault().post(new UpdateAllFragments());
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

            @Override
            public void onClick(DialogInterface dialog, int which) {
                update.run();
                if (refresh != null) {
                    refresh.run();
                }
            }
        });
        if (res.size() > 0 && refresh == null) {
            builder.setPositiveButton(R.string.play, new AlertDialog.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    LOG.d("play-click", file);
                    update.run();

                    FileMeta f = new FileMeta(Playlists.getFile(file).getPath());
                    LOG.d("play-click meta", f.getPath());

                    ExtUtils.openFile((Activity) a, f);

                }
            });
        }
        builder.show();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void dispalyPlaylist(final Activity a, final DocumentController dc) {
        final RecyclerView playlistRecycleView = (RecyclerView) a.findViewById(R.id.playlistRecycleView);

        //playlistRecycleView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(a);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        playlistRecycleView.setLayoutManager(linearLayoutManager);

        final TextView playListName = (TextView) a.findViewById(R.id.playListName);
        final TextView playListNameEdit = (TextView) a.findViewById(R.id.playListNameEdit);
        final View playListParent = a.findViewById(R.id.playListParent);


        // TxtUtils.updateAllLinks((ViewGroup) playListNameEdit.getParent());


        playListName.setVisibility(View.GONE);
        playListNameEdit.setVisibility(View.GONE);
        playlistRecycleView.setVisibility(View.GONE);
        playListParent.setVisibility(View.GONE);

        final String palylistPath = a.getIntent().getStringExtra(DocumentController.EXTRA_PLAYLIST);

        if (TxtUtils.isNotEmpty(palylistPath)) {
            playListName.setText(Playlists.formatPlaylistName(palylistPath));
            playListName.setVisibility(View.VISIBLE);
            playListParent.setVisibility(View.VISIBLE);
            playListNameEdit.setVisibility(View.VISIBLE);
            playlistRecycleView.setVisibility(View.VISIBLE);
        }

        final List<String> res = Playlists.getPlaylistItems(palylistPath);

        final PlaylistAdapter adapter = new PlaylistAdapter(a, res, new OnStartDragListener() {

            @Override
            public void onStartDrag(ViewHolder viewHolder) {
            }

            @Override
            public void onRevemove() {
            }

            @Override
            public void onItemClick(final String s) {
                LOG.d("onItemClick", s);
                if (dc != null && dc.getCurrentBook() != null && !dc.getCurrentBook().getPath().equals(s)) {

                    dc.onCloseActivityFinal(new Runnable() {

                        @Override
                        public void run() {
                            ExtUtils.showDocumentWithoutDialog(a, new File(s), palylistPath);
                        }
                    });
                }

            }

        }, true);
        if (dc != null && dc.getCurrentBook() != null) {
            String path = dc.getCurrentBook().getPath();
            adapter.setCurrentPath(path);
            int indexOf = res.indexOf(path);
            if (indexOf > 3) {
                playlistRecycleView.scrollToPosition(indexOf - 3);
            }

        }
        playlistRecycleView.setAdapter(adapter);

        playListNameEdit.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showPlayList(a, palylistPath, new Runnable() {
                    @Override
                    public void run() {
                        adapter.getItems().clear();
                        adapter.getItems().addAll(Playlists.getPlaylistItems(palylistPath));
                        adapter.notifyDataSetChanged();

                    }
                });
            }
        });

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

                if (false) {
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
                }

                menu.show();

            }
        });

    }

}
