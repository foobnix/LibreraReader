package com.text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Runner {

    public static void main(String[] args) throws IOException {
        File file = new File("/home/ivan-dev/dev/workspace/pdf4/ZTestProject/input/fb2zip");
        File[] listFiles = file.listFiles();

        for (File in : listFiles) {
            byte[] imageCover = null;

            if (in.getPath().endsWith(".epub")) {
                imageCover = EpubCoverExtractor.getImageCover(in.getPath());
            } else if (in.getPath().endsWith(".fb2")) {
                System.out.println(in.getPath());
                imageCover = Fb2Extractor.getImageCover(in.getPath());
            } else if (in.getPath().endsWith(".fb2.zip")) {
                System.out.println(in.getPath());
                imageCover = Fb2Extractor.getImageCoverZip(in.getPath());
            }

            if (imageCover == null) {
                throw new RuntimeException("fuck " + in.getPath());
            }
            File fileOutput = new File("/home/ivan-dev/dev/workspace/pdf4/ZTestProject/output/" + in.getName() + ".jpg");
            if (fileOutput.exists()) {
                fileOutput.delete();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(fileOutput);
            fileOutputStream.write(imageCover);
            fileOutputStream.close();
        }

    }

}
