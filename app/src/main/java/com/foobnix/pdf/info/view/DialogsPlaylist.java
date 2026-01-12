package com.foobnix.pdf.info.view;

import static com.foobnix.pdf.info.Playlists.L_PLAYLIST_FAVORITES;
import static com.foobnix.pdf.info.Playlists.L_PLAYLIST_FOLDER;
import static com.foobnix.pdf.info.Playlists.L_PLAYLIST_RECENT;

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
import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppData;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.Playlists;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.io.SearchCore;
import com.foobnix.pdf.info.view.drag.OnStartDragListener;
import com.foobnix.pdf.info.view.drag.PlaylistAdapter;
import com.foobnix.pdf.info.view.drag.SimpleItemTouchHelperCallback;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.search.activity.msg.UpdateAllFragments;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import com.foobnix.ui2.fragment.BrowseFragment2;
import com.jmedeisis.draglinearlayout.DragLinearLayout;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
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

        final BaseItemLayoutAdapter<String>
                adapter =
                new BaseItemLayoutAdapter<String>(a, R.layout.tag_item_text, items) {
                    @Override
                    public void populateView(View layout, final int position, final String tagName) {
                        TextView text = (TextView) layout.findViewById(R.id.text1);
                        text.setText(Playlists.formatPlaylistName(a, tagName));
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
        builder.setTitle(Playlists.formatPlaylistName(a, file));

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

    public static List<String> convert(List<FileMeta> list, int limit) {
        List<String> res = new ArrayList<>();
        int count = 0;
        for (FileMeta meta : list) {
            if(meta.getCusType()!=null && meta.getCusType().equals(FileMetaAdapter.DISPLAY_TYPE_DIRECTORY)){
                continue;
            }
            res.add(meta.getPath());
            if (count > limit) {
                break;
            }

        }
        return res;
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
        final ImageView closePlaylist = a.findViewById(R.id.closePlaylist);

        playListName.setVisibility(View.VISIBLE);

        playListNameEdit.setVisibility(View.GONE);
        playlistRecycleView.setVisibility(View.VISIBLE);
        playListParent.setVisibility(View.VISIBLE);

        Runnable updateVisible = new Runnable() {
            @Override
            public void run() {
                if (AppState.get().isPlayListVisible) {
                    playlistRecycleView.setVisibility(View.VISIBLE);
                    closePlaylist.setVisibility(View.VISIBLE);
                } else {
                    playlistRecycleView.setVisibility(View.GONE);
                    closePlaylist.setVisibility(View.GONE);
                }
            }
        };
        closePlaylist.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AppState.get().isPlayListVisible = false;
                updateVisible.run();
            }
        });
        updateVisible.run();
        playListParent.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AppState.get().isPlayListVisible = !AppState.get().isPlayListVisible;
                updateVisible.run();
            }
        });

        //a.getIntent().putExtra(DocumentController.EXTRA_PLAYLIST,dc.getString(R.string.recent));

        String palylistPathCheck = a.getIntent().getStringExtra(DocumentController.EXTRA_PLAYLIST);
        if (TxtUtils.isEmpty(palylistPathCheck)) {
            palylistPathCheck = AppState.get().playlistDefault;
        }
        final String playlistPath = palylistPathCheck;

        List<String> res = new ArrayList<String>();

        playListNameEdit.setVisibility(View.GONE);

        LOG.d("getFilesAndDirs","init",playlistPath);
        if (playlistPath.equals(L_PLAYLIST_RECENT)) {
            res = convert(AppData.get().getAllRecent(false), 50);
        } else if (playlistPath.equals(L_PLAYLIST_FAVORITES)) {
            res = convert(AppData.get().getAllFavoriteFiles(false), 50);
        } else if (playlistPath.startsWith(L_PLAYLIST_FOLDER)) {
            List<FileMeta> filesAndDirs = SearchCore.getFilesAndDirs(playlistPath.replace(L_PLAYLIST_FOLDER,""),false,
                    AppState.get().isDisplayAllFilesInFolder);
            BrowseFragment2.sortItems(filesAndDirs);
            res = convert(filesAndDirs,100);

        } else {
            playListNameEdit.setVisibility(View.VISIBLE);
            res = Playlists.getPlaylistItems(playlistPath);
        }

        playListName.setText("☰ " + Playlists.formatPlaylistName(a, playlistPath));
        TxtUtils.updateAllLinks((ViewGroup) playListNameEdit.getParent());

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
                            ExtUtils.showDocumentWithoutDialog(a, new File(s), playlistPath);
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
                showPlayList(a, playlistPath, new Runnable() {
                    @Override
                    public void run() {
                        adapter.getItems().clear();
                        adapter.getItems().addAll(Playlists.getPlaylistItems(playlistPath));
                        adapter.notifyDataSetChanged();

                    }
                });
            }
        });

        playListName.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!AppState.get().isPlayListVisible) {
                    AppState.get().isPlayListVisible = true;
                    updateVisible.run();
                    return;
                }

                final List<String> items = Playlists.getAllPlaylists();
                items.add(L_PLAYLIST_RECENT);
                items.add(L_PLAYLIST_FAVORITES);
                final List<FileMeta> folders = AppData.get().getAllFavoriteFolders();
                if (!folders.isEmpty()) {
                    for (FileMeta folder : folders) {
                        items.add(L_PLAYLIST_FOLDER + folder.getPath());
                    }
                }

                MyPopupMenu menu = new MyPopupMenu(v);
                for (final String item : items) {
                    menu.getMenu()
                        .add(Playlists.formatPlaylistName(a, item))
                        .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem m) {
                                AppState.get().playlistDefault = item;
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
