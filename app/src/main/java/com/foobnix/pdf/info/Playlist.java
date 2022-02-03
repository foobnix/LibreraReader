package com.foobnix.pdf.info;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.MyPath;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private final static String CURRENT_FILE_PREFIX = ">";
    private final String name;
    private List<String> paths;
    private int currentPathIndex;

    public Playlist(String name) {
        this.name = name;
        this.paths = new ArrayList<>();
        this.currentPathIndex = 0;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPathIndex = this.paths.indexOf(currentPath);
    }

    public boolean hasCurrentItem() {
        return this.currentPathIndex != 0;
    }

    public List<String> getItems() {
        if (this.paths.size() > 0) {
            return this.paths;
        }
        List<String> res = new ArrayList<>();
        try {
            if (TxtUtils.isEmpty(this.name)) {
                return res;
            }

            File child = Playlists.getFile(this.name);

            BufferedReader reader = new BufferedReader(new FileReader(child));

            String line;
            int currentPathIndex = 0;

            while ((line = reader.readLine()) != null) {
                if (TxtUtils.isNotEmpty(line)) {
                    if (line.startsWith(CURRENT_FILE_PREFIX)) {
                        line = line.replace(CURRENT_FILE_PREFIX, "");
                        this.currentPathIndex = currentPathIndex;
                    }
                    line = MyPath.toAbsolute(line);
                    res.add(line.replace(Playlists.L_PLAYLIST, ""));
                    currentPathIndex++;
                }
            }
            reader.close();
        } catch (Exception e) {
            LOG.e(e);
        }
        this.setPaths(res);
        return res;
    }

    public void updateCurrentFile(String currentFile) {
        this.setCurrentPath(currentFile);
        this.update(this.paths);
    }

    public void update(List<String> items) {
        File child = Playlists.getFile(this.name);
        LOG.d("Playlists", "updatePlaylist", child);

        String currentItem = items.get(this.currentPathIndex);
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(child)));
            for (String path : items) {
                out.println((path.equals(currentItem) ? ">" : "") + MyPath.toRelative(path));
            }
            out.close();
            this.setPaths(items);
        } catch (IOException e) {
            LOG.e(e);
        }

    }

    public String getFirstItem() {
        try {
            return getItems().get(this.currentPathIndex); // invoked when you clicked play
        } catch (Exception e) {
            LOG.e(e);
            return this.name;
        }
    }

}
