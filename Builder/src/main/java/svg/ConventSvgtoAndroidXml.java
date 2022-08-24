package svg;

import javax.swing.plaf.IconUIResource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class ConventSvgtoAndroidXml {
    public static void main(String[] args) throws IOException {
        String in = "/home/dev/Dropbox/Projects/glyphicons_pro/glyphicons-basic-2-4/";
        String out = "/home/dev/git/LibreraReader/app/src/main/res/drawable/";

        List<Integer> ids = Arrays.asList(
                4,5,17,21,28,37,38,49,50,55,57,66,67,73,77,82,86,87,92,94,
                101,104,105,106,108,112, 115,137,140,144,145,146,
                150,151,157,158,159,160,166,172,173,174,175,176,177,178,185,186,193,
                211,212,215,216,217, 218, 221,222,223,224,231,232,292,298,299,
                305,309,310,336,371,372,399,
                415,417,422,439,451,476, 477, 487,498,
                522,544,545, 578,580,588, 589,599,
                600,614,631,632,636,647,648,649,650,695,
                739,761,
                809,822,885,
                1020);

        File[] files = new File(in).listFiles();
        for (File file : files) {

            String content  = Files.readString(Path.of(file.getPath()));

            String name = file.getName();
            File outFile = new File(out,name);
            boolean has = false;
            for(int id:ids){
                if(name.contains("-"+id+"-")){
                    has = true;
                    break;
                }
            }
            if(!has){
                continue;
            }

            System.out.println(file);
            System.out.println(content);
            String path = content.replace("<svg id=\"glyphicons-basic\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 32 32\">","");
            path = path.replace("</svg>","").trim();
            System.out.println("==");
            System.out.println(path);
            System.out.println("==");



            String androidXml = "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                    "    android:viewportHeight=\"32\"\n" +
                    "    android:viewportWidth=\"32\"\n" +
                    "    android:width=\"32dp\"\n" +
                    "    android:height=\"32dp\"\n" +
                    "    >\n";

            path = path.replaceAll("id=\"(\\w+-*\\w*-*\\w*)\"","");
            path = path.replace(" d=","android:fillColor=\"@color/black\"\n android:pathData=");

            androidXml += path+"\n";
            androidXml += "</vector>";

            System.out.println(outFile);
            System.out.println(androidXml);
            System.out.println("==");

            String outName = outFile.getPath();
            outName = outName.replace(".svg",".xml");
            outName = outName.replace("-basic","");
            outName = outName.replace("-","_");
            System.out.println(":"+outName+":");


            BufferedWriter writer = new BufferedWriter(new FileWriter(outName));
            writer.write(androidXml);
            writer.close();






        }


    }
}
