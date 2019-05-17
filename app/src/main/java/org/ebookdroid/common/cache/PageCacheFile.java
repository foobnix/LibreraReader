package org.ebookdroid.common.cache;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppState;

import org.ebookdroid.core.codec.CodecPageInfo;
import org.emdev.utils.StringUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PageCacheFile extends File {

    private static final long serialVersionUID = 6836895806027391288L;


    public static PageCacheFile getPageFile(final String path, int pages) {
        long lastModified = new File(path).lastModified();
        final String md5 = StringUtils.md5(path + lastModified + pages + (AppState.get().fullScreenMode == AppState.FULL_SCREEN_FULLSCREEN));
        LOG.d("getPageFile", "LAST" + md5);
        final File cacheDir = CacheZipUtils.CACHE_RECENT;
        LOG.d("PageCacheFile-getPageFile");
        return new PageCacheFile(cacheDir, md5 + ".cache");
    }

    PageCacheFile(final File dir, final String name) {
        super(dir, name);
    }

    public CodecPageInfo[] load() {
        LOG.d("PageCacheFile-load");
        try {
            final DataInputStream in = new DataInputStream(new FileInputStream(this));
            try {
                final int pages = in.readInt();
                final CodecPageInfo[] infos = new CodecPageInfo[pages];

                for (int i = 0; i < infos.length; i++) {
                    infos[i] = new CodecPageInfo(in.readInt(), in.readInt());
                    if (infos[i].width == -1 || infos[i].height == -1) {
                        return null;
                    }
                }
                return infos;
            } catch (final EOFException ex) {
                ex.printStackTrace();
            } catch (final IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (final IOException ex) {
                    LOG.e(ex);
                }
            }
        } catch (final FileNotFoundException ex) {
            LOG.e(ex);
        }
        return null;
    }

    public void save(final CodecPageInfo[] infos) {
        LOG.d("PageCacheFile-save");
        try {
            getParentFile().mkdirs();
            final DataOutputStream out = new DataOutputStream(new FileOutputStream(this));
            try {
                out.writeInt(infos.length);
                for (int i = 0; i < infos.length; i++) {
                    if (infos[i] != null) {
                        out.writeInt(infos[i].width);
                        out.writeInt(infos[i].height);
                    } else {
                        out.writeInt(-1);
                        out.writeInt(-1);
                    }
                }
            } catch (final IOException ex) {
                LOG.e(ex);
            } finally {
                try {
                    out.close();
                } catch (final IOException ex) {
                    LOG.e(ex);
                }
            }
        } catch (final IOException ex) {
            LOG.e(ex);
        }
    }
}
