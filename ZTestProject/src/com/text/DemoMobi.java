package com.text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.mobi.MobiFile;
import com.mobi.MobiMeta;

public class DemoMobi {

    public static void main(String[] args) throws IOException {
        String pathname = "/home/ivan-dev/Documents/rtf2/Aleksandrova_Domovyonok-Kuzka.297647.fb2.mobi";
        MobiMeta meta = MobiFile.parseMobiMeta(new File(pathname));

        if (meta.getCover() != null) {
            FileOutputStream out = new FileOutputStream(new File("coverMOBI.jpg"));
            out.write(meta.getCover());
            out.flush();
            out.close();
        }

        System.out.println("Title: " + meta.getTitle());
        System.out.println("Author: " + meta.getAuthor());
    }

}
