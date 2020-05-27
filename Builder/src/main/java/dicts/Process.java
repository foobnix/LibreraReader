package dicts;


import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Process {


    public static void main(final String[] args) throws Exception {

        System.out.println("hello");

        final String DB = "/home/data/Downloads/replaces/db/sample.db";
        File file = new File(DB);
        file.delete();

        String url = "jdbc:sqlite:" + DB;
        Connection conn = DriverManager.getConnection(url);

        try {

            final Statement statement = conn.createStatement();
            statement.executeUpdate("drop table if exists DICT_META");
            statement.executeUpdate("create table DICT_META (key string, value string, PRIMARY KEY(`key`))");

            //String in = "/home/data/Downloads/replaces/db/in.txt";
            String in = "/home/data/Downloads/replaces/db/polnaya_akceptuirovannaya_paradigma.dict";
            String res = new String(Files.readAllBytes(Paths.get(in)), StandardCharsets.UTF_8);

            String slit[] = res.split(",");

            System.out.println("size = " + slit.length);


            conn.setAutoCommit(false);

            int i = 0;
            String prev = "";
            char my[] = {'а', 'о', 'и', 'у', 'е', 'ы', 'э', 'я', 'ю', 'ё'};
            for (String s : slit) {
                if (s.equals(prev)) {
                    continue;
                }
                prev = s;
                s = s.replace("`", "'").trim();
                if (!s.contains("'")) {
                    continue;
                }

                int count = 0;
                for (int w = 0; w < s.length(); w++) {
                    for (char c : my) {
                        if (s.charAt(w) == c) {
                            count++;
                            if (count > 1) {
                                break;
                            }
                        }
                    }
                    if (count > 1) {
                        break;
                    }
                }
                if (count <= 1) {
                    //System.out.println("skip:" + s);
                    continue;
                }


                i++;
                String value = s.replace("'", "*");
                String key = s.replace("'", "");


                try {
                    statement.executeUpdate(String.format("insert into DICT_META values('%s', '%s')", key, value));
                } catch (Exception e) {
                    //System.out.println("Dublicate:" + key + " " + value);
                    //e.printStackTrace();

                }
                if (i % 10000 == 0) {
                    System.out.println("p = " + i * 100 / slit.length + " |" + key + "=" + value + "|");
                    conn.commit();
                }

            }
            conn.commit();

            //CREATE TABLE `dict1` ( `key` TEXT, `value` TEXT, PRIMARY KEY(`key`) )
//            ResultSet rs = statement.executeQuery("select * from dict");
//            while (rs.next()) {
//                // read the result set
//                System.out.println("key = " + rs.getString("key"));
//                System.out.println("value = " + rs.getString("value"));
//            }
            System.out.println("finish");

        } finally {
            conn.close();
        }

    }
}
