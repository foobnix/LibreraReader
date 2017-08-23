package org.ebookdroid.common.cache;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.ebookdroid.core.codec.CodecPageInfo;

public class PageCacheFile extends File {

    private static final long serialVersionUID = 6836895806027391288L;

    PageCacheFile(final File dir, final String name) {
        super(dir, name);
    }

    public CodecPageInfo[] load() {
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
                }
            }
        } catch (final FileNotFoundException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void save(final CodecPageInfo[] infos) {
        try {
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
                ex.printStackTrace();
            } finally {
                try {
                    out.close();
                } catch (final IOException ex) {
                }
            }
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }
}
